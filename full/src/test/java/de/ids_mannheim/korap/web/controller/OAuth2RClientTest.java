package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

public class OAuth2RClientTest extends OAuth2TestBase {

    private String username = "OAuth2ClientControllerTest";
    private String userAuthHeader;

    public OAuth2RClientTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("R-user", "password");
    }

    public OAuth2ClientJson createOAuth2RClient () {
        OAuth2ClientJson client = new OAuth2ClientJson();
        client.setName("R client");
        client.setType(OAuth2ClientType.PUBLIC);
        client.setDescription("An R client with httr web server.");
        client.setRedirectURI("http://localhost:1410");
        return client;
    }

    @Test
    public void testRClientWithLocalhost ()
            throws ProcessingException, KustvaktException, IOException {
        // Register client
        OAuth2ClientJson clientJson = createOAuth2RClient();
        Response response = registerClient(username, clientJson);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(response.readEntity(String.class));
        String clientId = node.at("/client_id").asText();

        // send authorization
        String code = testAuthorize(clientId);

        // send token request
        response =
                requestTokenWithAuthorizationCodeAndForm(clientId, null, code);
        
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        
        String entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);

        // testing
        String accessToken = node.at("/access_token").asText();
        testSearchWithOAuth2Token(accessToken);

        // cleaning up
        deregisterClient(username, clientId);

        testSearchWithRevokedAccessToken(accessToken);
    }

    private String testAuthorize (String clientId) throws KustvaktException {

        Response response = requestAuthorizationCode("code", clientId, "",
                "search", "", userAuthHeader);

        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());

        URI redirectUri = response.getLocation();

        assertEquals("http", redirectUri.getScheme());
        assertEquals("localhost", redirectUri.getHost());
        assertEquals(1410, redirectUri.getPort());

        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(redirectUri).build().getQueryParams();
        String code = params.getFirst("code");
        assertNotNull(code);
        assertEquals("search", params.getFirst("scope"));
        return code;
    }

}
