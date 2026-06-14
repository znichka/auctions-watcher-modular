package parser.parsers.page;

import parser.data.ItemDescription;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.stereotype.Component;
import parser.parsers.SeleniumAbstractPageParser;

@Component
public class AvitoPageParser extends SeleniumAbstractPageParser {

    @Override
    protected ExpectedCondition<WebElement> expectedCondition() {
        return ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[data-marker=item]"));
    }

    @Override
    public Elements getElementCardsList(Document doc) {
        return doc.select("div[data-marker=item]");
    }

    @Override
    public ItemDescription getItemFromCard(Element card) {
        String id = card.attr("data-item-id");

        Element linkElement = card.selectFirst("a[data-marker=item-title]");
        String caption = linkElement.text();
        String itemUrl = "https://www.avito.ru" + linkElement.attr("href");
        if (itemUrl.contains("?slocation")) {
            itemUrl = itemUrl.substring(0, itemUrl.indexOf("?slocation"));
        }

        Element imageElement = card.selectFirst("[data-marker=item-photo] img");
        String imageUrl = imageElement != null ? imageElement.attr("src") : null;

        return new ItemDescription(id, itemUrl, imageUrl, caption);
    }

    @Override
    public String getDomainName() {
        return "avito";
    }
}
