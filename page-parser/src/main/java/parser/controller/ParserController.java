package parser.controller;

import jakarta.websocket.server.PathParam;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import parser.data.ItemDescription;
import parser.parsers.PageParserFactory;

import java.util.ArrayList;
import java.util.List;

@Log
@RestController
public class ParserController {
    @Autowired
    PageParserFactory pageParserFactory;

    @GetMapping("/health")
    @ResponseBody
    String healthcheck() {
        return "ok";
    }

    @GetMapping("/parse")
    @ResponseBody
     List<ItemDescription> getItems(@PathParam("url") String url) {
        System.out.println(url);
        List<ItemDescription> result = new ArrayList<>();
        try {
            result = pageParserFactory.getParserFor(url).getAllItems(url);
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        return result;
    }
}
