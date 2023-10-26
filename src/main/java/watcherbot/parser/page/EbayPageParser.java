package watcherbot.parser.page;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import watcherbot.description.ItemDescription;
import watcherbot.parser.AbstractPageParser;

@Component
public class EbayPageParser extends AbstractPageParser {
    @Override
    public Elements getElementCardsList(Document doc) {
//        Element count = doc.getElementsByClass("srp-controls__count-heading").first().selectFirst("span");
        Element count = doc.getElementsByClass("srp-controls__count-heading").first();
        count = (count.selectFirst("span") != null) ? count.selectFirst("span") : count;

        if (count.text().equals("0")) return new Elements() ;
        return doc.getElementsByClass("s-item__wrapper");
    }

    @Override
    public ItemDescription getItemFromCard(Element card) {
        Element item = card.getElementsByClass("s-item__image").first();
        item = item.getElementsByTag("a").first();

        String itemUrl = item.attr("href");
        itemUrl = itemUrl.substring(0 ,itemUrl.indexOf('?'));

        String id = itemUrl.substring(itemUrl.length()-12);

        Element imgElement = card.getElementsByClass("s-item__image-wrapper").first();
        imgElement = imgElement.getElementsByTag("img").first();

        String photoUrl = imgElement.attr("src");
        String caption = imgElement.attr("alt");

        return new ItemDescription(id, itemUrl, photoUrl, caption);
    }

    @Override
    public String getDomainName() {
        return "ebay";
    }
}
