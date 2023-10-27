package watcherbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import watcherbot.description.ManagerDescription;

@Repository
public interface ManagerDescriptionRepository extends JpaRepository<ManagerDescription, Integer> {
}
