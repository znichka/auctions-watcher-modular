package watcherbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import watcherbot.config.ConfigurationControllerTestConfig;
import watcherbot.description.ManagerDescription;
import watcherbot.description.PageDescription;
import watcherbot.repository.ManagerDescriptionRepository;
import watcherbot.repository.PageDescriptionRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes = {ConfigurationControllerTestConfig.class})
@WebMvcTest(ConfigurationController.class)
class ConfigurationControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ManagerDescriptionRepository managerDescriptionRepository;
    @Autowired
    PageDescriptionRepository pageDescriptionRepository;

    @Test
    void addPage() throws Exception {
        Path path = Path.of("src/test/resources/test-watcher-bot-description.json");
        File file = new File(path.toUri());
        assertTrue(file.exists());

        MvcResult response = mockMvc.perform(post("/bots").contentType(MediaType.APPLICATION_JSON).content(Files.readString(path)))
                .andExpect(status().isOk()).andReturn();

        ManagerDescription managerDescription = new ObjectMapper().readValue(response.getResponse().getContentAsString(), ManagerDescription.class);
        managerDescription.setId(1);

        path = Path.of("src/test/resources/test-page-description.json");
        file = new File(path.toUri());
        assertTrue(file.exists());

        Mockito.when(managerDescriptionRepository.findById(1)).thenReturn(Optional.of(managerDescription));

        PageDescription pageDescription = new ObjectMapper().readValue(Files.readString(path), PageDescription.class);

        mockMvc.perform(post(String.format("/bots/%d/pages", managerDescription.getId())).contentType(MediaType.APPLICATION_JSON).content(Files.readString(path)))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format("/bots/%d/pages", managerDescription.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].url").value(pageDescription.getUrl()))
                .andExpect(jsonPath("$[0].description").value(pageDescription.getDescription()));
    }

    @Test
    void getAllManagers() throws Exception {
        Path path = Path.of("src/test/resources/test-watcher-bot-description.json");
        File file = new File(path.toUri());
        assertTrue(file.exists());

        ManagerDescription managerDescription = new ObjectMapper().readValue(Files.readString(path), ManagerDescription.class);

        mockMvc.perform(post("/bots").contentType(MediaType.APPLICATION_JSON).content(Files.readString(path)))
                        .andExpect(status().isOk());

        managerDescription.setId(1);
        Mockito.when(managerDescriptionRepository.findAll()).thenReturn(List.of(managerDescription));

        mockMvc.perform(get("/bots"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value(managerDescription.getName()));
//                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAllPages() throws Exception {
        Path path = Path.of("src/test/resources/test-watcher-bot-description-with-pages.json");
        File file = new File(path.toUri());
        assertTrue(file.exists());

        MvcResult response = mockMvc.perform(post("/bots").contentType(MediaType.APPLICATION_JSON).content(Files.readString(path)))
                .andExpect(status().isOk()).andReturn();

        ManagerDescription managerDescription = new ObjectMapper().readValue(response.getResponse().getContentAsString(), ManagerDescription.class);
        managerDescription.setId(1);
        Mockito.when(managerDescriptionRepository.findById(1)).thenReturn(Optional.of(managerDescription));

        mockMvc.perform(get(String.format("/bots/%d/pages", managerDescription.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(managerDescription.getPages().size()));

        mockMvc.perform(get("/bots/100/pages"))
                .andExpect(status().isNotFound());

    }

    @Test
    void getNoPages() throws Exception {
        Path path = Path.of("src/test/resources/test-watcher-bot-description.json");
        File file = new File(path.toUri());
        assertTrue(file.exists());

        MvcResult response = mockMvc.perform(post("/bots").contentType(MediaType.APPLICATION_JSON).content(Files.readString(path)))
                .andExpect(status().isOk()).andReturn();

        ManagerDescription managerDescription = new ObjectMapper().readValue(response.getResponse().getContentAsString(), ManagerDescription.class);
        managerDescription.setId(1);
        Mockito.when(managerDescriptionRepository.findById(1)).thenReturn(Optional.of(managerDescription));

        mockMvc.perform(get(String.format("/bots/%d/pages", managerDescription.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deletePage() throws Exception {
        Path path = Path.of("src/test/resources/test-watcher-bot-description-with-pages.json");
        File file = new File(path.toUri());
        assertTrue(file.exists());

        MvcResult response = mockMvc.perform(post("/bots").contentType(MediaType.APPLICATION_JSON).content(Files.readString(path)))
                .andExpect(status().isOk()).andReturn();

        ManagerDescription managerDescription = new ObjectMapper().readValue(response.getResponse().getContentAsString(), ManagerDescription.class);
        Mockito.when(managerDescriptionRepository.findById(0)).thenReturn(Optional.of(managerDescription));

        mockMvc.perform(get(String.format("/bots/%d/pages", managerDescription.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(managerDescription.getPages().size()));

        PageDescription pageDescription = managerDescription.getPages().get(0);
        Mockito.when(pageDescriptionRepository.findById(any())).thenReturn(Optional.of(pageDescription));

        mockMvc.perform(delete(String.format("/bots/%d/pages/%d", managerDescription.getId(), pageDescription.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format("/bots/%d/pages", managerDescription.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(managerDescription.getPages().size()));

    }

    @Test
    void healthcheck() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }


}