package watcherbot.description;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.*;

class ConfigDescriptionTest {
    @Test
    void testPageConfig() throws IOException {
        Path path = Path.of("src/test/resources/test-page-description.json");
        File file = new File(path.toUri());
        assertTrue(file.exists());
        PageDescription description = new ObjectMapper().readValue(file, PageDescription.class);
        JsonNode json = new ObjectMapper().readTree(file);

        assertNotNull(description);
        assertEquals(description.getNotify(), 24);
        assertEquals(description.getPeriod(), json.get("period").asInt());
        assertEquals(description.getUrl(), json.get("url").asText());
        assertEquals(description.getDescription(), json.get("description").asText());

//        assertThatJson(description).isEqualTo(Files.readString(path));
    }

    @Test
    void testWatcherBotsConfig() throws IOException {
        Path path = Path.of("src/test/resources/test-watcher-bot-description.json");
        File file = new File(path.toUri());
        assertTrue(file.exists());
        ManagerDescription description = new ObjectMapper().readValue(file, ManagerDescription.class);
        assertNotNull(description);
        assertThatJson(description).isEqualTo(Files.readString(path));
    }

    @Test
    void testConfigDescription() throws IOException {
        Path path = Path.of("src/test/resources/test-config-description.json");
        File file = new File(path.toUri());
        assertTrue(file.exists());
        ConfigDescription description = new ObjectMapper().readValue(file, ConfigDescription.class);
        assertNotNull(description);
        assertThatJson(description).isEqualTo(Files.readString(path));
    }


}