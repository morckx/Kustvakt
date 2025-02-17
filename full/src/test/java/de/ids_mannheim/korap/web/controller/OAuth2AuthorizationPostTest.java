package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.glassfish.jersey.uri.UriComponent;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("OAuth2 Authorization Post Test")
class OAuth2AuthorizationPostTest extends OAuth2TestBase {

    public String userAuthHeader;

    public OAuth2AuthorizationPostTest() throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler.createBasicAuthorizationHeaderValue("dory", "password");
    }

    private Response requestAuthorizationCode(Form form, String authHeader) throws KustvaktException {
        return target().path(API_VERSION).path("oauth2").path("authorize").request().header(Attributes.AUTHORIZATION, authHeader).header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32").header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED).post(Entity.form(form));
    }

    @Test
    @DisplayName("Test Authorize Confidential Client")
    void testAuthorizeConfidentialClient() throws KustvaktException {
        Form form = new Form();
        form.param("response_type", "code");
        form.param("client_id", confidentialClientId);
        form.param("state", "thisIsMyState");
        form.param("scope", "search");
        Response response = requestAuthorizationCode(form, userAuthHeader);
        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(), response.getStatus());
        URI redirectUri = response.getLocation();
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(redirectUri).build().getQueryParams();
        assertNotNull(params.getFirst("code"));
        assertEquals(params.getFirst("state"), "thisIsMyState");
    }

    @Test
    @DisplayName("Test Request Token Authorization Confidential")
    void testRequestTokenAuthorizationConfidential() throws KustvaktException {
        Form authForm = new Form();
        authForm.param("response_type", "code");
        authForm.param("client_id", confidentialClientId);
        authForm.param("scope", "search");
        Response response = requestAuthorizationCode(authForm, userAuthHeader);
        URI redirectUri = response.getLocation();
        MultivaluedMap<String, String> params = UriComponent.decodeQuery(redirectUri, true);
        String code = params.get("code").get(0);
        String scopes = params.get("scope").get(0);
        assertEquals(scopes, "search");
        response = requestTokenWithAuthorizationCodeAndForm(confidentialClientId, clientSecret, code);
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(), node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }
}
