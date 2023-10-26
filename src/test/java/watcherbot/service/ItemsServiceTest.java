package watcherbot.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import watcherbot.description.ItemDescription;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import({ItemsService.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemsServiceTest {
    @Autowired
    ItemsService itemsService;

    @Test
    void insertIfUnique() {
        assertTrue(itemsService.insertIfUnique("1", "aaa", "", 1));
        assertTrue(itemsService.insertIfUnique("2", "bbb", "",  1));

        assertFalse(itemsService.insertIfUnique("1", "aaa","",  1));
        assertFalse(itemsService.insertIfUnique("1", "bbb","",  1));
        assertFalse(itemsService.insertIfUnique("2", "aaa","",  1));
    }
}