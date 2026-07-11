package watcherbot.description;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Embeddable
@Schema(description = "Telegram bot credentials used to deliver notifications")
public class TelegramBotCredentials {
    @Schema(description = "Telegram bot API token issued by @BotFather", example = "123456789:AAExampleTokenValue")
    private String token;
    @Schema(description = "Telegram chat id notifications are sent to (numeric; negative for group chats)", example = "1234567890")
    private String chatId;
}
