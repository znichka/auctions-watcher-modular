package watcherbot.parser;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.OperationNotSupportedException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Log
public class PageParserFactory {
    private final Map<String, AbstractPageParser> parsers;

    @Autowired
    public PageParserFactory(List<AbstractPageParser> availibleParsers) {
        parsers = availibleParsers.stream().collect(Collectors.toMap(AbstractPageParser::getDomainName, Function.identity()));
    }

    @SneakyThrows
    public AbstractPageParser getParserFor(String url) {
        URL urlWrapper = new URL(url);
        String[] hostParts = urlWrapper.getHost().split("\\.");

        for (String hostPart : hostParts) {
            Optional<String> domainName = parsers.keySet().stream().filter(hostPart::equalsIgnoreCase).findFirst();
            if (domainName.isPresent())
                return parsers.get(domainName.get());
        }
        throw new OperationNotSupportedException(String.format("Host %s is not supported at the moment", urlWrapper.getHost()));
    }
}
