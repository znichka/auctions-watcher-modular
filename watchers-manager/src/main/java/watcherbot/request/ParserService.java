package watcherbot.request;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import watcherbot.description.ItemDescription;
import watcherbot.description.PageDescription;

import java.util.Arrays;
import java.util.List;

@Component
@Log
@Scope("prototype")
public class ParserService {
    @Value("${docker.parser.url}")
    String PARSER_ENDPOINT;

    public List<ItemDescription> getItems(PageDescription pageDescription) {
        return getItems(pageDescription.getUrl());
    }

    public List<ItemDescription> getItems(String url) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("url", url);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);

        ResponseEntity<ItemDescription[]> response = restTemplate.exchange(PARSER_ENDPOINT, HttpMethod.GET, requestEntity, ItemDescription[].class);
        ItemDescription[] itemDescriptions = response.getBody();

        if (itemDescriptions == null) itemDescriptions = new ItemDescription[0];
        return Arrays.asList(itemDescriptions);
    }
}
