package de.ids_mannheim.korap.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.dao.OAuth2ClientDao;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.service.OAuth2InitClientService;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.controller.OAuth2TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Initial Super Client Test")
class InitialSuperClientTest extends OAuth2TestBase {

    @Autowired
    private FullConfiguration config;

    @Autowired
    private OAuth2ClientDao clientDao;

    private String path = KustvaktConfiguration.DATA_FOLDER + "/" + OAuth2InitClientService.TEST_OUTPUT_FILENAME;

    @Test
    @DisplayName("Test Creating Initial Super Client")
    void testCreatingInitialSuperClient() throws IOException, KustvaktException {
        assertTrue(config.createInitialSuperClient());
        File f = new File(path);
        assertTrue(f.exists());
        JsonNode node = JsonUtils.readFile(path, JsonNode.class);
        String superClientId = node.at("/client_id").asText();
        String superClientSecret = node.at("/client_secret").asText();
        OAuth2Client superClient = clientDao.retrieveClientById(superClientId);
        assertTrue(superClient.isSuper());
        testLogin(superClientId, superClientSecret);
        removeSuperClientFile();
    }

    private void testLogin(String superClientId, String superClientSecret) throws KustvaktException {
        Response response = requestTokenWithPassword(superClientId, superClientSecret, "username", "password");
        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertTrue(!node.at("/access_token").isMissingNode());
        assertTrue(!node.at("/refresh_token").isMissingNode());
        assertTrue(!node.at("/expires_in").isMissingNode());
        assertEquals(node.at("/scope").asText(), "all");
        assertEquals(node.at("/token_type").asText(), "Bearer");
    }

    private void removeSuperClientFile() {
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
    }
}
