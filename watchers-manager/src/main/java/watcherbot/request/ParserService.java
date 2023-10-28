package watcherbot.request;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import watcherbot.description.ItemDescription;
import watcherbot.description.PageDescription;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Component
@Log
@Scope("prototype")
public class ParserService {
    @Value("${docker.parser.url}")
    String PARSER_ENDPOINT;

    @Autowired
    RestTemplate restTemplate;

    public List<ItemDescription> getItems(PageDescription pageDescription) {
        return getItems(pageDescription.getUrl());
    }

    public List<ItemDescription> getItems(String url) {
        try {
            var map = new HashMap<String, String>();
            map.put("url", url);

            ResponseEntity<ItemDescription[]> response = restTemplate.getForEntity(PARSER_ENDPOINT, ItemDescription[].class, map);
            ItemDescription[] itemDescriptions = response.getBody();

            if (itemDescriptions == null) itemDescriptions = new ItemDescription[0];
            return Arrays.asList(itemDescriptions);
        } catch (Exception e) {
            log.severe("Getting items failed for url " + url);
            log.severe(e.getMessage());
            return List.of();
        }
    }
}
