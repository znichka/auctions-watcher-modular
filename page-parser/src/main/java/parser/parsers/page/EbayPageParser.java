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
    @Override
    protected ExpectedCondition<?> expectedCondition() {
        return ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".srp-results")),
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".brw-product-card"))
        );
    }

    @Override
    public Elements getElementCardsList(Document doc) {
        // Search results page (/sch/)
        Elements searchCards = doc.select("li.s-card");
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
        return doc.getElementsByClass("brw-product-card");
    }

    @Override
    public ItemDescription getItemFromCard(Element card) {
        if (card.hasClass("brw-product-card")) {
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
        Element link = card.selectFirst("a.brw-product-card__image-link");
        String itemUrl = link != null ? link.attr("href") : null;

        String id = null;
        if (itemUrl != null) {
            int iidIdx = itemUrl.indexOf("iid=");
            if (iidIdx >= 0) {
                id = itemUrl.substring(iidIdx + 4);
                int ampIdx = id.indexOf('&');
                if (ampIdx > 0) id = id.substring(0, ampIdx);
            }
        }

        Element imgElement = card.selectFirst("img.brw-product-card__image");
        String photoUrl = imgElement != null ? imgElement.attr("src") : null;
        String caption = imgElement != null ? imgElement.attr("alt") : null;

        return new ItemDescription(id, itemUrl, photoUrl, caption);
    }

    @Override
    public String getDomainName() {
        return "ebay";
    }
}
