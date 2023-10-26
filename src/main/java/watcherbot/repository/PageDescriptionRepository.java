package watcherbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import watcherbot.description.PageDescription;

@Repository
public interface PageDescriptionRepository extends JpaRepository<PageDescription, Integer> {
}
