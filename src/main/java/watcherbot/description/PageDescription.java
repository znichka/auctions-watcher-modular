package watcherbot.description;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Getter
@Entity
@Table(name = "pages")
public class PageDescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column
    private String url;
    @Column
    private String description;
    @Column
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
