package watcherbot.request;

import java.io.IOException;

public class TelegramRateLimitException extends IOException {
    private final Integer retryAfterSeconds;

    public TelegramRateLimitException(String itemUrl, Integer retryAfterSeconds, Throwable cause) {
        super(String.format("Rate limited by Telegram while sending item %s", itemUrl), cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
