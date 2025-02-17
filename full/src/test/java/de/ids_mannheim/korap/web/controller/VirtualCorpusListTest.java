package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Virtual Corpus List Test")
class VirtualCorpusListTest extends VirtualCorpusTestBase {

    @Test
    @DisplayName("Test List VC Nemo")
    void testListVCNemo() throws ProcessingException, KustvaktException {
        JsonNode node = testListOwnerVC("nemo");
        assertEquals(1, node.size());
        node = listSystemVC("nemo");
        assertEquals(1, node.size());
        node = listVC("nemo");
        assertEquals(3, node.size());
    }

    @Test
    @DisplayName("Test List VC Pearl")
    void testListVCPearl() throws ProcessingException, KustvaktException {
        JsonNode node = testListOwnerVC("pearl");
        assertEquals(0, node.size());
        node = listVC("pearl");
        assertEquals(2, node.size());
    }

    @Test
    @DisplayName("Test List VC Dory")
    void testListVCDory() throws ProcessingException, KustvaktException {
        JsonNode node = testListOwnerVC("dory");
        assertEquals(2, node.size());
        node = listVC("dory");
        assertEquals(4, node.size());
    }

    @Test
    @DisplayName("Test List Available VC Guest")
    void testListAvailableVCGuest() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").request().get();
        testResponseUnauthorized(response, "guest");
    }

    @Disabled
    @Deprecated
    @Test
    @DisplayName("Test List Available VC By Other User")
    void testListAvailableVCByOtherUser() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").path("~dory").request().header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("pearl", "pass")).get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(StatusCodes.AUTHORIZATION_FAILED, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(), "Unauthorized operation for user: pearl");
        checkWWWAuthenticateHeader(response);
    }

    @Disabled
    @Deprecated
    @Test
    @DisplayName("Test List User VC")
    void testListUserVC() throws ProcessingException, KustvaktException {
        Response response = target().path(API_VERSION).path("vc").queryParam("username", "dory").request().header(Attributes.AUTHORIZATION, HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("admin", "pass")).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(4, node.size());
    }
}
