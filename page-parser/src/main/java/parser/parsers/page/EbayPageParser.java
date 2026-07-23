package parser.parsers.page;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.stereotype.Component;
import parser.data.ItemDescription;
import parser.parsers.SeleniumAbstractPageParser;

@Component
public class EbayPageParser extends SeleniumAbstractPageParser {
    public EbayPageParser() {
        // eBay blocks a fresh session that navigates straight to a search/category URL
        // (returns a bot-block "Error Page") but allows it once the session has visited
        // the eBay homepage first to pick up cookies.
        warmup = true;
    }

    @Override
    protected ExpectedCondition<?> expectedCondition() {
        return ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".srp-results")),
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".brwrvr__item-card"))
        );
    }

    @Override
    public Elements getElementCardsList(Document doc) {
        // Search results page (/sch/)
        // li.s-card also matches a hidden accessibility decoy card (wrapped in
        // div.s-clipped[aria-hidden=true], fake id/url like .../itm/123456) - exclude it.
        Elements searchCards = new Elements();
        for (Element card : doc.select("li.s-card")) {
            if (card.closest(".s-clipped") == null) searchCards.add(card);
        }
        if (!searchCards.isEmpty()) {
            Element count = doc.getElementsByClass("srp-controls__count-heading").first();
            if (count != null) {
                Element span = count.selectFirst("span");
                String text = span != null ? span.text() : count.text();
                if (text.equals("0")) return new Elements();
            }
            return searchCards;
        }

        // Category page (/b/)
        return doc.getElementsByClass("brwrvr__item-card");
    }

    @Override
    public ItemDescription getItemFromCard(Element card) {
        if (card.hasClass("brwrvr__item-card")) {
            return getItemFromCategoryCard(card);
        }
        return getItemFromSearchCard(card);
    }

    private ItemDescription getItemFromSearchCard(Element card) {
        String id = card.attr("data-listingid");

        Element link = card.selectFirst("a.s-card__link");
        String itemUrl = null;
        if (link != null) {
            itemUrl = link.attr("href");
            int queryIdx = itemUrl.indexOf('?');
            if (queryIdx > 0) itemUrl = itemUrl.substring(0, queryIdx);
        }

        Element imgElement = card.selectFirst("img.s-card__image");
        String photoUrl = imgElement != null ? imgElement.attr("src") : null;
        String caption = imgElement != null ? imgElement.attr("alt") : null;

        return new ItemDescription(id, itemUrl, photoUrl, caption);
    }

    private ItemDescription getItemFromCategoryCard(Element card) {
        Element link = card.selectFirst("a.brwrvr__item-card__image-link");
        if (link == null) return null; // skip "Shop on eBay" placeholder cards

        String itemUrl = link.attr("href");
        int queryIdx = itemUrl.indexOf('?');
        if (queryIdx > 0) itemUrl = itemUrl.substring(0, queryIdx);

        // Item id is the path segment after /itm/
        String id = null;
        int itmIdx = itemUrl.indexOf("/itm/");
        if (itmIdx >= 0) {
            id = itemUrl.substring(itmIdx + 5);
            int slashIdx = id.indexOf('/');
            if (slashIdx > 0) id = id.substring(0, slashIdx);
        }

        Element imgElement = card.selectFirst("img.brwrvr__item-card__image");
        String photoUrl = null;
        if (imgElement != null) {
            // images are lazy-loaded: real url is in data-src, src is a placeholder gif
            photoUrl = imgElement.hasAttr("data-src") ? imgElement.attr("data-src") : imgElement.attr("src");
        }

        Element titleElement = card.selectFirst("h3.bsig__title__text");
        String caption = titleElement != null ? titleElement.text()
                : (imgElement != null ? imgElement.attr("alt") : null);

        return new ItemDescription(id, itemUrl, photoUrl, caption);
    }

    @Override
    public String getDomainName() {
        return "ebay";
    }
}
