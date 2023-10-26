package watcherbot.parser.page;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import watcherbot.description.ItemDescription;
import watcherbot.parser.AbstractPageParser;

@Component
public class AvitoPageParser extends AbstractPageParser {

    @Override
    public Elements getElementCardsList(Document doc) {
        return doc.getElementsByClass("iva-item-root-Nj_hb");
    }

    @Override
    public ItemDescription getItemFromCard(Element card) {
        String id = card.attr("data-item-id");

        Element linkElement = card.getElementsByClass("link-link-MbQDP").first();
        String caption = linkElement.attr("title");
        String itemUrl = "www.avito.ru" + linkElement.attr("href");
        if (itemUrl.contains("?slocation")) {
            itemUrl = itemUrl.substring(0, itemUrl.indexOf("?slocation"));
        }

        Element imageElement = card.getElementsByClass("photo-slider-list-xFf2c").first().selectFirst("li");
        String imageUrl = imageElement.attr("data-marker");
        imageUrl = imageUrl.substring(19);

        return new ItemDescription(id, itemUrl, imageUrl, caption);
    }

    @Override
    public String getDomainName() {
        return "avito";
    }
}
