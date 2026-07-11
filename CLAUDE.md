# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
    `scroll = true` to trigger lazy loading. Falls back to plain Jsoup if no WebDriver is available.
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

### Cross-module note
`ItemDescription` is **duplicated** in both modules (`parser.data` and `watcherbot.description`)
by design — the parser produces it as JSON, the manager deserializes it. Keep the JSON-relevant
fields (`id`, `itemUrl`, `photoUrl`, `caption`) in sync across both copies when changing them.

### Persistence
- Production: PostgreSQL. `spring.jpa.hibernate.ddl-auto=none` everywhere — **the DB schema is
  managed externally, not by Hibernate**. The `managers`/`pages` tables back JPA entities; the
  `items` table is accessed only via raw SQL. Adding/altering tables means changing the DB by hand.
- Dev/test: in-memory H2 in PostgreSQL-compatibility mode (`application-dev.properties` for the
  `dev` profile; `src/test/resources/application.properties` for tests).

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
  container names, ports, DB credentials, and Selenium settings. **These env files contain real
  secrets (DB password, Telegram bot token) — do not echo, log, or commit changes that expose
  them.** They're `.gitignore`d (`*.env`), so this is safe by default even for new ones.
- The parser reaches Chrome at `chrome_host:chrome_port`; `chrome_max_sessions` must match
  `selenium.sessions.max` (the parser's `selenium.sessions.max` is bound to the container's
  `SE_NODE_MAX_SESSIONS`).
- **Chrome container failure mode (recurring):** `/tmp` and `/dev/shm` are RAM-backed tmpfs. If
  leaked Chrome profiles fill `/tmp`, new sessions fail with `session not created: DevToolsActivePort
  file doesn't exist` (HTTP 500) — the container stays "up"/healthy but every Selenium site (meshok,
  ebay, olx, avito) returns zero items and the app silently stops delivering. Mitigations already in
  place: Chrome runs `--headless=new` and uses `/dev/shm` (no `--disable-dev-shm-usage`), plus
  `SE_NODE_SESSION_TIMEOUT`, `init: true` to reap stuck sessions/zombies, and
  `SE_ENABLE_BROWSER_LEFTOVERS_CLEANUP=true` to clean up orphaned Chrome profiles. First diagnostic:
  `docker exec auctions-chrome df -h /tmp`. The compose healthcheck is a liveness probe (does the
  grid HTTP respond) — it does **not** trigger a restart on its own.

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
  `network_mode: host`, same as the two apps and Prometheus. Dashboards persist in a named Docker
  volume. Port 3000. Also provisions alerting (`grafana/provisioning/alerting/{policies,rules}.yaml`)
  with rules for scrape-target-down, container-stopped, and container-restarted, notifying via a
  Telegram contact point. The contact point (`contact-points.yaml`) is generated at deploy time from
  `contact-points.yaml.template` by `grafana-up.sh`, substituting the bot token/chat id from
  `grafana-alerting-params.env` — not committed, since Grafana's own `$__env{}` provisioning macro
  coerces a numeric-looking chat id into a JSON number and fails schema validation.

## Adding a new marketplace parser

1. Create a class in `parser.parsers.page` extending `AbstractPageParser` (Jsoup-only) or
   `SeleniumAbstractPageParser` (needs JS rendering). Annotate `@Component` — the factory
   auto-discovers all `AbstractPageParser` beans.
2. Implement `getDomainName()` to return a host segment that appears in target URLs, plus
   `getElementCardsList` and `getItemFromCard`. For Selenium parsers also implement
   `expectedCondition()`.
3. Add a corresponding test in `page-parser/src/test/.../page/` following the existing
   `*PageTest` pattern.