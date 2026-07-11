package watcherbot.description;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "managers")
@Schema(description = "One Telegram bot: its credentials plus the set of marketplace pages it watches")
public class ManagerDescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    @Schema(description = "Auto-generated manager id", accessMode = Schema.AccessMode.READ_ONLY)
    int id;

    @Embedded
    @Schema(description = "Telegram bot credentials")
    TelegramBotCredentials credentials;

    @Column
    @Schema(description = "Human-readable label for this bot/manager", example = "Antique clocks watcher")
    String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Schema(description = "Pages currently watched by this manager")
    Set<PageDescription> pages = new HashSet<>();

    public ManagerDescription setCredentials(TelegramBotCredentials credentials) {
        this.credentials = credentials;
        return this;
    }

    public ManagerDescription setName(String name) {
        this.name = name;
        return this;
    }

    public ManagerDescription addPages(List<PageDescription> pages) {
        this.pages.addAll(pages);
        return this;
    }

    public ManagerDescription addPage(PageDescription page) {
        this.pages.add(page);
        return this;
    }

    public boolean removePage(PageDescription page) {
        return pages.remove(page);
    }

    public List<PageDescription> getPages() {
        return pages.stream().toList();
    }
}
