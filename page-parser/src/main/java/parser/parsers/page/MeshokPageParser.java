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
        // As of 2026-09, the card link is a bare `a[class^=wrapper]` (no `data-itemcard`
        // attribute any more) and the title is a `div[class^=itemTitle]`, not an `<a>`. The
        // numeric item id now lives on the "watch item" button's `itemid` attribute instead.
        Element linkElement = card.selectFirst("a[class^=wrapper]");
        String itemUrl = linkElement != null ? "https://meshok.net" + linkElement.attr("href") : null;

        Element idElement = card.selectFirst("[itemid]");
        String id = idElement != null ? idElement.attr("itemid") : null;

        Element titleElement = card.selectFirst("div[class^=itemTitle]");
        String caption = titleElement != null ? titleElement.text() : null;

        Element imgElement = card.selectFirst("div[class^=image] img");
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
