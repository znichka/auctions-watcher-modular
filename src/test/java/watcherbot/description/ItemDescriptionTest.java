package watcherbot.description;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class ItemDescriptionTest {

    @Test
    void getPhotoHash() throws NoSuchAlgorithmException, IOException {
        ItemDescription item1 = new ItemDescription();
        item1.setPhotoUrl("https://b.itemimg.com/i/289756499.0.jpg");
        ItemDescription item2 = new ItemDescription();
        item2.setPhotoUrl("https://b.itemimg.com/i/288334525.0.jpg");
        ItemDescription item3 = new ItemDescription();
        item3.setPhotoUrl("https://b.itemimg.com/i/288334525.0.jpg");
        ItemDescription item4 = new ItemDescription();
        item4.setPhotoUrl("https://b.itemimg.com/i/288334552.0.jpg");

        assertEquals(item1.getPhotoHash(), item2.getPhotoHash());
        assertEquals(item2.getPhotoHash(), item3.getPhotoHash());
        assertNotEquals(item3.getPhotoHash(), item4.getPhotoHash());

    }
}