package watcherbot.description;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Getter
@Entity
@Table(name = "pages")
@Schema(description = "One watched marketplace URL and its polling settings")
public class PageDescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Auto-generated page id", accessMode = Schema.AccessMode.READ_ONLY)
    private int id;
    @Column
    @Schema(description = "URL of the marketplace search/listing page to watch", example = "https://meshok.net/good/collecting/1?a=antique")
    private String url;
    @Column
    @Schema(description = "Free-text label shown in Telegram notifications for items found on this page", example = "Antique clocks")
    private String description;
    @Column
    @Schema(description = "Polling interval in minutes", example = "15")
    private Integer period;
    @JsonIgnore
    @Column
    private Long notify = 24L;

    public PageDescription setUrl(String url) {
        this.url = url;
        return this;
    }

    public PageDescription setDescription(String description) {
        this.description = description;
        return this;

    }

    public PageDescription setPeriod(Integer period) {
        this.period = period;
        return this;

    }

    public PageDescription setNotify(Long notify) {
        this.notify = notify;
        return this;
    }
}
