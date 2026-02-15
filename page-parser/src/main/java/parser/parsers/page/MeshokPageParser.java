package parser.parsers.page;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.stereotype.Component;
import parser.data.ItemDescription;
import parser.parsers.SeleniumAbstractPageParser;

@Component
public class MeshokPageParser extends SeleniumAbstractPageParser {
    @Override
    protected ExpectedCondition<WebElement> expectedCondition() {
        return ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[class^=itemCard]"));
    }

    @Override
    public Elements getElementCardsList(Document doc) {
        return doc.select("div[class^=itemCard]");
    }

    @Override
    public ItemDescription getItemFromCard(Element card) {
////        Element cardImage = card.select(".m-item-card-image").first();
////        if (cardImage != null) {
//            Element itemTitleElement = card.select("div[class^=itemTitle]").first();
//            String id = itemTitleElement.attr("data-itemcard");
//
//            String itemUrl = "http://meshok.net" + card.selectFirst("a").attr("href");
//            Element imgElement = card.selectFirst("img");
//            String photoUrl = null;
//            if (imgElement != null) photoUrl = "https://meshok.net" + imgElement.attr("src");
//            String caption = itemTitleElement.text();
//
//            return new ItemDescription(id, itemUrl, photoUrl, caption);
////        }
////        return null;

        Element titleElement = card.selectFirst("a.itemTitle_e29bb");
        String id = titleElement != null ? titleElement.attr("data-itemcard") : null;
        String itemUrl = titleElement != null ? "http://meshok.net" + titleElement.attr("href") : null;
        String caption = titleElement != null ? titleElement.text() : null;

        Element imgElement = card.selectFirst("a.image_e29bb img");
        String photoUrl = null;
        if (imgElement != null) {
            photoUrl = "https://meshok.net" + imgElement.attr("src");
        }

        return new ItemDescription(id, itemUrl, photoUrl, caption);
    }

    @Override
    public String getDomainName() {
        return "meshok";
    }
}
