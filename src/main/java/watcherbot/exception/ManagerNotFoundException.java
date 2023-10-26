package watcherbot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.NOT_FOUND,
        reason="Bot manager not found")
public class ManagerNotFoundException extends RuntimeException {
}
