package watcherbot.parser.page;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.stereotype.Component;
import watcherbot.description.ItemDescription;
import watcherbot.parser.SeleniumAbstractPageParser;

@Component
public class MeshokPageParser extends SeleniumAbstractPageParser {
    @Override
    protected ExpectedCondition<WebElement> expectedCondition() {
        return ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[class^=itemCardList]"));
    }

    @Override
    public Elements getElementCardsList(Document doc) {
        return doc.select("div[class^=itemCardList]");
    }

    @Override
    public ItemDescription getItemFromCard(Element card) {
        Element cardImage = card.select(".m-item-card-image").first();
        if (cardImage != null) {
            Element itemTitleElement = card.select("div[class^=itemTitle]").first();
            String id = itemTitleElement.attr("data-itemcard");

            String itemUrl = cardImage.attr("href");
            Element imgElement = cardImage.selectFirst("img");
            String photoUrl = null;
            if (imgElement != null) photoUrl = imgElement.attr("src");
            String caption = itemTitleElement.text();
            itemUrl = "http://meshok.net" + itemUrl;

            return new ItemDescription(id, itemUrl, photoUrl, caption);
        }
        return null;
    }

    @Override
    public String getDomainName() {
        return "meshok";
    }
}
