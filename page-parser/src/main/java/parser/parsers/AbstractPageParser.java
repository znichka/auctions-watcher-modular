package parser.parsers;

import parser.data.ItemDescription;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.java.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log
public abstract class AbstractPageParser {
    // Optional so that parsers instantiated outside Spring (unit tests) still work.
    @Autowired(required = false)
    protected MeterRegistry meterRegistry;

    // Pre-registered in initMetrics() so every series exists from startup with value 0.
    // The alert rules divide failures by total attempts, which silently matches nothing
    // if a domain's counter only springs into existence on its first success.
    private Counter parseSuccessCounter;
    private Counter parseFailureCounter;
    private Counter itemsCounter;

    @PostConstruct
    void initMetrics() {
        if (meterRegistry == null) return;
        String domain = getDomainName();
        parseSuccessCounter = Counter.builder("parser.page.parse")
                .description("Page parse attempts, by outcome")
                .tag("domain", domain)
                .tag("outcome", "success")
                .register(meterRegistry);
        parseFailureCounter = Counter.builder("parser.page.parse")
                .description("Page parse attempts, by outcome")
                .tag("domain", domain)
                .tag("outcome", "failure")
                .register(meterRegistry);
        itemsCounter = Counter.builder("parser.page.items")
                .description("Item cards parsed off watched pages (not de-duplicated)")
                .tag("domain", domain)
                .register(meterRegistry);
    }

    protected abstract Elements getElementCardsList(Document doc);

    protected abstract ItemDescription getItemFromCard(Element card);

    public abstract String getDomainName();

    protected Document getDocument(String url) throws IOException {
        return Jsoup.connect(url)
//                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
//                    .userAgent("Opera")
                .referrer("http://www.google.com")
                .timeout(1000*10)
                .get();
    }

    public final List<ItemDescription> getAllItems(String url) {
        List<ItemDescription> items = new ArrayList<>();
        try {
            Document doc = getDocument(url);
            Elements cards = getElementCardsList(doc);
            for (Element card : cards){
                try {
                    ItemDescription item = getItemFromCard(card);
                    if (item != null) items.add(item);
                } catch (Exception e) {
                    log.warning("Error while parsing an item on a page, skipping. Url: "+ url);
                    log.warning(e.getMessage());
                }
            }
            // A bot-block page parses "successfully" into zero cards, so the item count -
            // not just the absence of an exception - is what distinguishes a working
            // source from a silently blocked one.
            if (parseSuccessCounter != null) parseSuccessCounter.increment();
            if (itemsCounter != null) itemsCounter.increment(items.size());
        } catch (Exception e) {
            log.warning("Error while parsing a page, skipping. Url: "+ url);
            log.warning(e.getMessage());
            if (parseFailureCounter != null) parseFailureCounter.increment();
        }
        return items;
    }
}
