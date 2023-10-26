package watcherbot.description;

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
public class ManagerDescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter
    int id;

    @Embedded
    TelegramBotCredentials credentials;

    @Column
    String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
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
