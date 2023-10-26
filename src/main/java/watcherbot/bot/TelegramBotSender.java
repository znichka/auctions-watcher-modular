package watcherbot.bot;

import lombok.extern.java.Log;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import watcherbot.description.ItemDescription;
import watcherbot.description.TelegramBotCredentials;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

@Log
@Component
@Scope("prototype")
public class TelegramBotSender {
    private void send(String response) throws MalformedURLException {
        URL url = new URL(response);
        try {
            URLConnection conn = url.openConnection();
            InputStream is = new BufferedInputStream(conn.getInputStream());
        } catch (IOException e) {
            log.severe(String.format("Error while sending telegram response: %s", response));
        }
    }

    public void sendMessage(TelegramBotCredentials credentials, String message) throws IOException {
        String response = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";
        response = String.format(response, credentials.getToken(), credentials.getChatId(), message);
        send(response);
    }

    public void sendImage(TelegramBotCredentials credentials, String photoUrl, String photoCaption, String itemLink) throws IOException {
        String response = "https://api.telegram.org/bot%s/sendPhoto?chat_id=%s&photo=%s&caption=%s&parse_mode=HTML";
        String caption =  String.format("<a href=\"%s\">%s</a>", itemLink, photoCaption);

        response = String.format(response, credentials.getToken(), credentials.getChatId(), photoUrl, caption);
        send(response);
    }



    public void sendItemDescription(TelegramBotCredentials credentials, ItemDescription item) throws IOException {
        sendImageUpload(credentials, item.getPhotoContents(), item.getCaption(), item.getItemUrl());
    }

    public void sendImageUpload(TelegramBotCredentials credentials, byte[] photoContent, String photoCaption, String itemUrl) {
        String requestUrl = "https://api.telegram.org/bot%s/sendPhoto";
        String caption =  String.format("<a href=\"%s\">%s</a>", itemUrl, photoCaption);

        requestUrl = String.format(requestUrl, credentials.getToken());
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> map= new LinkedMultiValueMap<>();
        map.add("chat_id", credentials.getChatId());
        map.add("caption", caption);
        map.add("parse_mode", "HTML");

        HttpHeaders imageHeaders = new HttpHeaders();
        imageHeaders.setContentType(MediaType.IMAGE_PNG);

        HttpEntity<ByteArrayResource> imageAttachment;
        ByteArrayResource fileAsResource = new ByteArrayResource(photoContent){
            @Override
            public String getFilename(){
                return "filename";
            }
        };
        imageAttachment = new HttpEntity<>(fileAsResource, imageHeaders);

        map.add("photo", imageAttachment);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(map, headers);
        restTemplate.postForEntity(requestUrl, request , String.class);
    }
}
