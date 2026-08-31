# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Keep this file up to date.** After making an important change to the codebase (architecture,
auth/security, deployment process, new services, changed endpoints, etc.), update the relevant
section of this file in the same piece of work — don't leave it to a future session to notice the
drift.

## What this is

A notifier for new items appearing on auction marketplaces. Watched pages are polled on a
schedule; new items are pushed to Telegram bots. The system is configured at runtime over a REST
API. Supported sources: meshok.net, antiques.ay.by, etsy.com, ebay.com/ebay.de, olx.pl, plus
parsers for avito and kufar.

## Architecture

Maven multi-module project (Java 21, Spring Boot 3.1.4). Two independently deployed Spring Boot
apps that talk over HTTP:

### `page-parser` — stateless scraping service
- Single endpoint: `GET /parse?url={url}` returns a JSON array of `ItemDescription`. Also `GET /health`.
- `PageParserFactory` (`parser.parsers`) routes a URL to a parser by matching each host segment
  against every parser's `getDomainName()` (e.g. host `antiques.ay.by` → parser returning `"ay"`).
  Unsupported hosts throw `OperationNotSupportedException`.
- Each marketplace parser lives in `parser.parsers.page` and extends one of two bases:
  - `AbstractPageParser` — fetches the page directly with **Jsoup** (`getDocument`). Used by
    Ay, Etsy, Kufar.
  - `SeleniumAbstractPageParser` — renders the page through a headless Chrome `WebDriverPool`,
    then hands the HTML to Jsoup. Used by Meshok, Ebay, Olx, Avito. These also declare an
    `expectedCondition()` (a Selenium `ExpectedCondition` the pool waits on) and may set
    `scroll = true` to trigger lazy loading, or `warmup = true` to have the pool visit the site's
    homepage first to pick up cookies before navigating to the real URL. Falls back to plain Jsoup
    if no WebDriver is available. **Ebay sets `warmup = true`**: confirmed 2026-07-23 that eBay
    serves a bot-block "Error Page" (HTTP-level 403 on subresources, page title "Error Page | eBay")
    to a fresh session that navigates straight to a `/sch/` or `/b/` URL with no prior visit, but
    allows it once the session has loaded `https://www.ebay.com` (or `.de`) first — reproduced
    reliably outside the app via raw Selenium Grid sessions. Without the warmup visit this silently
    returns zero items (not an exception), which looks exactly like "no new listings" rather than a
    failure — worth checking first if a Selenium-backed source (especially eBay) mysteriously stops
    producing notifications. **The warmup navigation must not parse `url` with `java.net.URI`** —
    fixed 2026-07-23 after the first `warmup` implementation used `URI.create(url)` to derive the
    homepage origin and broke ~80% of configured eBay searches within hours of deploy: many stored
    eBay search URLs contain unencoded spaces in the query string (e.g. `_nkw=glass garland`), which
    Selenium's `driver.get(url)` tolerates but `URI.create()` rejects with
    `IllegalArgumentException`. The homepage origin is now derived with a plain
    `url.replaceFirst("^(https?://[^/]+).*$", "$1")` instead.
- A parser implements three methods: `getElementCardsList(doc)` (select item cards),
  `getItemFromCard(card)` (extract one `ItemDescription`), `getDomainName()`. Per-item parse
  errors are caught and skipped — a broken card never fails the whole page.
- `WebDriverPool` bounds Selenium concurrency with a fixed thread pool sized by
  `selenium.sessions.max`. `WebDriverConfig` provides the driver via two mutually exclusive Spring
  profiles: **`local`** = a local `ChromeDriver`; **everything else (`!local`)** = a
  `RemoteWebDriver` pointing at a Selenium Grid container (`docker.chromedriver.url`). The remote
  default is what runs in production.
- **Site layouts and bot-detection change often** — most recent commits are parser/selector/stealth
  fixes. Selectors frequently use prefix matches like `div[class^=itemCard]` to survive hashed
  class names. When a parser breaks, the fix is usually in its CSS selectors and/or the
  `expectedCondition`.

### `watchers-manager` — stateful orchestrator
- REST CRUD under `ConfigurationController`: `/bots` (managers), `/bots/{id}`,
  `/bots/{id}/pages`, `DELETE /bots/{id}/pages/{pageId}`, `/health`.
- API docs via `springdoc-openapi-starter-webmvc-ui`: Swagger UI at `/swagger-ui/index.html`, raw
  spec at `/v3/api-docs`. No extra config beyond the dependency — it auto-discovers
  `ConfigurationController`. Both are gated by the same Basic auth as the rest of the API (see
  Security below); there's no separate permitAll rule for them.
- Domain model (JPA entities in `watcherbot.description`): a **`ManagerDescription`** = one
  Telegram bot (embedded `TelegramBotCredentials` token + chatId, plus a set of pages). A
  **`PageDescription`** = one watched URL with a polling `period` (minutes).
- `PageWatcherService` is the core. On startup (`@PostConstruct`) it loads all managers from the
  DB and creates a prototype-scoped `PageWatchersManager` per manager. `PageWatchersManager`
  schedules each page on a shared `ScheduledExecutorService` (`scheduleAtFixedRate`, period in
  minutes). Each run: call `page-parser` over HTTP (`ParserService`) → filter to unique items
  (`ItemsService`) → enqueue for sending (`SenderQueue` → `TelegramBotSender`).
- **Deduplication** is in `ItemsService` using raw SQL via `NamedParameterJdbcTemplate` against an
  `items` table (`item_id, image_hash, manager_id, url`). `insertIfUnique` returns true only when a
  row was actually inserted, so "new item" == "successful insert". When
  `check-for-image-duplicates=true`, an item is also considered a duplicate if another item for the
  same manager has the same image hash. Image hash = MD5 of the downloaded photo, computed lazily
  in `ItemDescription.getPhotoHash()`.
- Telegram delivery: `TelegramBotSender` calls the Telegram HTTP API directly; the primary path
  (`sendItemDescription`) uploads the photo bytes as multipart with an HTML caption linking to the
  item. `SenderQueue` serializes all sends through a single-thread executor.
- **Security**: `SecurityConfig` (`watcherbot.config`) requires HTTP Basic auth on every endpoint
  except `/health` and `/actuator/**` (left open for uptime checks and Prometheus scraping).
  Credentials come from `spring.security.user.name`/`password`, bound to the `API_USER`/
  `API_PASSWORD` env vars in prod (see Deployment) and hardcoded to `dev`/`dev` in the `dev`
  profile. Path matching must use `AntPathRequestMatcher` explicitly, not bare string patterns —
  because `h2database` is a runtime (not test-scoped) dependency, prod has both the H2 console
  servlet and the main `DispatcherServlet` mapped, and Spring Security can't auto-resolve MVC-style
  matchers when more than one servlet is present (it throws at startup). The `@WebMvcTest` in
  `ConfigurationControllerTest` bypasses the filter chain entirely via
  `@AutoConfigureMockMvc(addFilters = false)`, so it won't catch security-config regressions —
  verify auth changes by booting the app for real (e.g. `-Dspring.profiles.active=dev`) before
  redeploying.

### Cross-module note
`ItemDescription` is **duplicated** in both modules (`parser.data` and `watcherbot.description`)
by design — the parser produces it as JSON, the manager deserializes it. Keep the JSON-relevant
fields (`id`, `itemUrl`, `photoUrl`, `caption`) in sync across both copies when changing them.

### Persistence
- Production: PostgreSQL. `spring.jpa.hibernate.ddl-auto=none` everywhere — **the DB schema is
  managed externally, not by Hibernate**. The `managers`/`pages` tables back JPA entities; the
  `items` table is accessed only via raw SQL. Adding/altering tables means changing the DB by hand,
  by running `watchers-manager/src/main/resources/schema.sql` against the target DB — that file is
  only auto-applied by Spring for the `dev` profile (`spring.sql.init.mode=always` gated to
  `spring.sql.init.platform=dev` in `application-dev.properties`); prod does not run it
  automatically, so a fresh prod DB starts out with no tables until you run it by hand.
- Dev/test: in-memory H2 in PostgreSQL-compatibility mode (`application-dev.properties` for the
  `dev` profile; `src/test/resources/application.properties` for tests).
- **Where prod Postgres actually runs is per-environment**, set via `db_host`/`db_port` in each
  environment's `*-params.env`: `oracle-prod`/`oracle-test`/`oracle-dev` each run their own local
  `db` service (`db_host=localhost`) — see the `db` service in
  `watchers-manager/template-manager-compose.yml`, alongside `manager` in the same compose file,
  data on the external Docker volume `${db_volume}`. **`nas-prod` originally pointed `db_host` at
  an external Oracle Cloud Always-Free instance's public IP** instead of running its own `db`
  service (that block was commented out in the compose file) — as of 2026-08-26 that Oracle
  instance was permanently reclaimed by Oracle (Always Free instances get force-stopped when a
  trial account's resource plan lapses) and its boot volume proved unrecoverable without upgrading
  off the free tier, which was declined. **nas-prod now runs its own local `db` service** like the
  oracle-* environments — the commented-out block in `template-manager-compose.yml` was uncommented
  and is live. All dedup/manager/page history from the old Oracle-hosted DB was lost; nas-prod
  started from an empty schema (applied by hand per the paragraph above) on that date.
- **Pin the `db` service's image to a specific major version (currently `postgres:16`) — never
  `postgres:latest`.** Confirmed 2026-08-26: `postgres:latest` currently resolves to Postgres 18,
  whose image changed its expected on-disk layout for 18+ (`pg_ctlcluster`-style, versioned
  subdirectories under a single `/var/lib/postgresql` mount) and refuses to start at all against
  this compose file's plain `/var/lib/postgresql/data` bind mount — it crash-loops printing a
  layout-mismatch error instead of initializing. `postgres:16` starts cleanly against the existing
  mount as-is.

## Build, test, run

All commands from the repo root unless noted.

```bash
# Build both modules (also copies runtime deps to target/dependency, needed by the Dockerfiles)
mvn clean package

# Build skipping tests (what the deploy scripts do)
mvn clean package -DskipTests

# Run all tests
mvn test

# Test a single module
mvn -pl page-parser test
mvn -pl watchers-manager test

# Run a single test class / method
mvn -pl page-parser -Dtest=MeshokPageTest test
mvn -pl watchers-manager -Dtest=ItemsServiceTest#someMethod test
```

### Running locally
Both apps read config from env-var placeholders in `application.properties`, so set them (or use a
profile) before running:
- **page-parser**: use the `local` Spring profile to drive a local Chrome instead of the remote
  grid: `-Dspring.profiles.active=local`. Otherwise set `CHROME_HOST`/`CHROME_PORT`/
  `SE_NODE_MAX_SESSIONS` to reach a Selenium container.
- **watchers-manager**: use the `dev` profile for in-memory H2 (no Postgres needed):
  `-Dspring.profiles.active=dev`. It still needs `PARSER_HOST`/`PARSER_PORT` to reach a running
  page-parser.

## Deployment

Docker-based, driven by per-module scripts (`page-parser/page-parser-up.sh`,
`watchers-manager/watchers-manager-up.sh`, and one `-up.sh` each for the monitoring stack below).
The two app scripts additionally: `mvn clean package -DskipTests` → `docker build` (Dockerfiles
copy `target/classes` + `target/dependency` onto the classpath) → `docker-compose ... up`.

- **Every `-up.sh`/`-down.sh` script takes two optional positional args:** `$1` = `DOCKER_HOST`
  (default `ssh://nas`), `$2` = the env file passed to `docker-compose --env-file` (default varies
  per script, e.g. `../nas-prod-params.env`). Run with no args to deploy to the usual target;
  pass args to point at a different host/env, e.g. `./page-parser-up.sh ssh://other-host
  ../oracle-prod-params.env`. `grafana-up.sh` additionally takes a third, `$3` = the alerting env
  file (default `../grafana-alerting-params.env`), used to render the Telegram contact point (see
  Monitoring stack below).
- **The two apps and the Chrome container are deployed by three separate `docker-compose` runs:**
  - `watchers-manager-up.sh` deploys the manager via `template-manager-compose.yml`.
  - `page-parser-up.sh` deploys **only the parser**, via `template-parser-compose-without-chrome.yml`
    (in that file the `chrome` service is commented out). So a parser redeploy never touches Chrome.
  - The `selenium/standalone-chrome` container is defined in `template-parser-compose-with-chrome.yml`
    (the file's `parser` service is unused by the scripts) and is brought up / recreated **on its
    own**: `DOCKER_HOST=ssh://nas docker-compose -f template-parser-compose-with-chrome.yml
    --env-file ../nas-prod-params.env up -d chrome`. A separate debug Chrome
    (`chrome-test-compose.yml`, ports 4445/7901) mirrors the prod chrome service's resource limits
    (`mem_limit: 3g`, `/tmp`+`/dev/shm` tmpfs at `2g` each, `SE_NODE_MAX_SESSIONS` from
    `chrome_max_sessions`) and can run alongside it without conflict. It also needs
    `--env-file ../nas-prod-params.env` to resolve `chrome_max_sessions`.
- Runtime values come from `*-params.env` files at the repo root (e.g. `nas-prod-params.env`,
  `prometheus-params.env`, `cadvisor-params.env`, `grafana-params.env`, `grafana-alerting-params.env`):
  container names, ports, DB credentials, Selenium settings, and the `watchers-manager` API's
  `api_user`/`api_password` (wired to `API_USER`/`API_PASSWORD` in `template-manager-compose.yml`,
  see Security above). **These env files contain real secrets (DB password, Telegram bot token,
  API credentials) — do not echo, log, or commit changes that expose them.** They're `.gitignore`d
  (`*.env`), so this is safe by default even for new ones. Each has a committed `.example`
  template with placeholder values, so a new deployment target can be bootstrapped by copying one
  and filling in real values — keep these in sync when adding or renaming a key in the real env
  files. `nas-prod-params.env`, `oracle-prod-params.env`, `oracle-test-params.env`, and
  `oracle-dev-params.env` are all different hosts/environments filling the *same*
  manager+parser+chrome+db schema (they're interchangeable as the `$2` arg to the app up/down
  scripts), so they share one template, `app-deployment-params.env.example`, rather than one each.
  `prometheus-params.env`, `cadvisor-params.env`, `grafana-params.env`, and
  `grafana-alerting-params.env` have their own schemas and each keep their own `.example` file.
- The parser reaches Chrome at `chrome_host:chrome_port`; `chrome_max_sessions` must match
  `selenium.sessions.max` (the parser's `selenium.sessions.max` is bound to the container's
  `SE_NODE_MAX_SESSIONS`).
- **Chrome container failure mode (recurring):** `/tmp` and `/dev/shm` are RAM-backed tmpfs. If
  leaked Chrome profiles fill `/tmp`, new sessions fail with `session not created: DevToolsActivePort
  file doesn't exist` (HTTP 500) — the container stays "up"/healthy but every Selenium site (meshok,
  ebay, olx, avito) returns zero items and the app silently stops delivering. Mitigations already in
  place: Chrome runs `--headless=new` and uses `/dev/shm` (no `--disable-dev-shm-usage`), plus
  `SE_NODE_SESSION_TIMEOUT`, `init: true` to reap stuck sessions/zombies, and
  `SE_ENABLE_BROWSER_LEFTOVERS_CLEANUP=true`. First diagnostic: `docker exec auctions-chrome df -h
  /tmp`.
  - **`SE_ENABLE_BROWSER_LEFTOVERS_CLEANUP` alone is not sufficient** — confirmed 2026-07-12. The
    image's built-in daemon (`/opt/bin/chrome-cleanup.sh`, runs hourly by default) only deletes
    leftover dirs via `find /tmp -name "*com.google.Chrome.*" -type d -mtime
    +${SE_BROWSER_LEFTOVERS_TEMPFILES_DAYS}`. Neither compose file (nor any `*-params.env`) sets
    `SE_BROWSER_LEFTOVERS_TEMPFILES_DAYS`, so it runs at the image's built-in default of `1` (a full
    day) — confirmed via `docker exec auctions-chrome env`. **This threshold can't usefully be tuned
    lower**: GNU `find`'s `-mtime` silently truncates fractional values (e.g. `+0.1` behaves exactly
    like `+0`), and `-mtime +0` still requires ~24h of age due to whole-day bucketing — there is no
    env-var way to get sub-day cleanup out of this script (tested directly on the container; don't
    add `SE_BROWSER_LEFTOVERS_TEMPFILES_DAYS` to the compose files expecting it to help). So if
    leftover-dir creation is fast enough to fill the 2GB tmpfs in under 24h (observed: ~330 dirs/hour,
    saturated in ~8h), the built-in cleanup structurally never gets a chance to run before disk is
    full, regardless of the flag.
  - The dirs in question (`com.google.Chrome.chrome_chrome_url_fetcher_.*`) are created by Chrome's
    background network fetches (component updater, safe-browsing, domain reliability, sync,
    client-side phishing detection) — one or more per session launch. Fix applied in
    `WebDriverConfig.java`: both `ChromeOptions` blocks now pass `--disable-background-networking`,
    `--disable-component-update`, `--disable-domain-reliability`,
    `--disable-client-side-phishing-detection`, `--disable-sync`, `--disable-default-apps` to stop
    these fetches (and their temp dirs) from being created in the first place, rather than relying on
    cleanup after the fact.
  - Manual remediation when `/tmp` is already full: `docker exec auctions-chrome sh -c 'find /tmp
    -maxdepth 1 -name "*com.google.Chrome.*" -exec rm -rf {} +'` (note `-rf`, not `-f` — these are
    directories, not files).
  - The compose healthcheck is a liveness probe (does the grid HTTP respond) — it does **not**
    trigger a restart on its own.

### Monitoring stack

Three more standalone `docker-compose` deployments, each with its own `-up.sh` and `*-params.env`:

- **Prometheus** (`prometheus/prometheus-up.sh`) — config (`prometheus.yml`) is baked into a custom
  image at build time (`COPY prometheus.yml /etc/prometheus/prometheus.yml`), so changing scrape
  targets means editing that file and rerunning the up-script, not a live-reloadable mount. Scrapes
  `watchers-manager` and `page-parser` at `/actuator/prometheus` (both expose Micrometer metrics via
  `spring-boot-starter-actuator` + `micrometer-registry-prometheus`), plus `cadvisor` at `/metrics`.
  UI on port 9090.
- **cAdvisor** (`cadvisor/cadvisor-up.sh`) — reports per-container memory/CPU stats (e.g.
  `container_memory_usage_bytes`, `container_spec_memory_limit_bytes`,
  `container_cpu_usage_seconds_total`, all labeled `name="<container_name>"`) for **every**
  container on the host, not just one. Runs unprivileged with read-only mounts of `/rootfs`,
  `/var/run`, `/sys`, `/var/lib/docker` — sufficient for memory/CPU on this host's cgroup v2 +
  overlay2 setup, though some peripheral stats (per-disk I/O, some hardware metrics) may be
  incomplete. Port 8081.
- **Grafana** (`grafana/grafana-up.sh`) — auto-provisions a Prometheus datasource
  (`grafana/provisioning/datasources/datasource.yml`, baked into the image like Prometheus's
  config) pointed at `http://localhost:9090`; this only works because Grafana runs with
  `network_mode: host`, same as the two apps and Prometheus. The dashboard itself
  (`grafana/provisioning/dashboards/auctions-watcher.json`) is file-provisioned the same way —
  edit it and redeploy to change panels/queries. It's gitignored (blanket `*.json` rule) but lives
  on disk in this repo like any other file. The named Docker volume (`/var/lib/grafana`) only
  persists Grafana's own runtime DB (users, session state, UI-made tweaks since
  `allowUiUpdates: true`) — not the dashboard definition, which is re-synced from the JSON file on
  each start (`updateIntervalSeconds: 30`). Port 3000. Also provisions alerting
  (`grafana/provisioning/alerting/{policies,rules}.yaml`)
  with rules for scrape-target-down, container-stopped, and container-restarted, notifying via a
  Telegram contact point. The contact point (`contact-points.yaml`) is generated at deploy time from
  `contact-points.yaml.template` by `grafana-up.sh`, substituting the bot token/chat id from
  `grafana-alerting-params.env` — not committed, since Grafana's own `$__env{}` provisioning macro
  coerces a numeric-looking chat id into a JSON number and fails schema validation. The Telegram
  message body is overridden (`settings.message` in the template, `parse_mode: HTML`) to render via
  a custom notification template, `telegram.message`, provisioned from
  `provisioning/alerting/templates.yaml` — without it Grafana's default template dumps every label,
  including the noisy `container_label_com_docker_compose_*`/`org_opencontainers_image_*` labels
  cAdvisor/Docker Compose attach, which is unreadable in a Telegram message. The custom template
  renders one line per alert (firing/resolved + alert name) plus the rule's `summary` annotation, so
  every rule in `rules.yaml` must keep a human-readable `summary` annotation for the notification to
  make sense.

## Adding a new marketplace parser

1. Create a class in `parser.parsers.page` extending `AbstractPageParser` (Jsoup-only) or
   `SeleniumAbstractPageParser` (needs JS rendering). Annotate `@Component` — the factory
   auto-discovers all `AbstractPageParser` beans.
2. Implement `getDomainName()` to return a host segment that appears in target URLs, plus
   `getElementCardsList` and `getItemFromCard`. For Selenium parsers also implement
   `expectedCondition()`.
3. Add a corresponding test in `page-parser/src/test/.../page/` following the existing
   `*PageTest` pattern.