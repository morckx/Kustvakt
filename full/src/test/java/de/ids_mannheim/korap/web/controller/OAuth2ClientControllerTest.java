package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.spi.container.ContainerRequest;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2ClientType;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.input.OAuth2ClientJson;

/**
 * @author margaretha
 *
 */
public class OAuth2ClientControllerTest extends OAuth2TestBase {

    private String username = "OAuth2ClientControllerTest";
    private String userAuthHeader;

    public OAuth2ClientControllerTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
    }

    private void checkWWWAuthenticateHeader (ClientResponse response) {
        Set<Entry<String, List<String>>> headers =
                response.getHeaders().entrySet();

        for (Entry<String, List<String>> header : headers) {
            if (header.getKey().equals(ContainerRequest.WWW_AUTHENTICATE)) {
                assertEquals("Basic realm=\"Kustvakt\"",
                        header.getValue().get(0));
            }
        }
    }

    private OAuth2ClientJson createOAuth2ClientJson (String name,
            OAuth2ClientType type, String description) {
        OAuth2ClientJson client = new OAuth2ClientJson();
        if (name != null) {
            client.setName(name);
        }
        client.setType(type);
        if (description != null) {
            client.setDescription(description);
        }
        return client;

    }

    @Test
    public void testRetrieveClientInfo () throws KustvaktException {
        // public client
        JsonNode clientInfo = retrieveClientInfo(publicClientId, "system");
        assertEquals(publicClientId, clientInfo.at("/client_id").asText());
        assertEquals("public client plugin with redirect uri",
                clientInfo.at("/client_name").asText());
        assertNotNull(clientInfo.at("/client_description"));
        assertNotNull(clientInfo.at("/client_url"));
        assertEquals("PUBLIC", clientInfo.at("/client_type").asText());
        assertEquals("system", clientInfo.at("/registered_by").asText());

        // confidential client
        clientInfo = retrieveClientInfo(confidentialClientId, "system");
        assertEquals(confidentialClientId, clientInfo.at("/client_id").asText());
        assertEquals("non super confidential client",
                clientInfo.at("/client_name").asText());
        assertNotNull(clientInfo.at("/client_url"));
        assertNotNull(clientInfo.at("/redirect_uri"));
        assertEquals(false, clientInfo.at("/super").asBoolean());
        assertEquals("CONFIDENTIAL", clientInfo.at("/client_type").asText());

        // super client
        clientInfo = retrieveClientInfo(superClientId, "system");
        assertEquals(superClientId, clientInfo.at("/client_id").asText());
        assertEquals("super confidential client",
                clientInfo.at("/client_name").asText());
        assertNotNull(clientInfo.at("/client_url"));
        assertNotNull(clientInfo.at("/redirect_uri"));
        assertEquals("CONFIDENTIAL", clientInfo.at("/client_type").asText());
        assertTrue(clientInfo.at("/super").asBoolean());
    }
    
    @Test
    public void testRegisterConfidentialClient () throws KustvaktException {
        ClientResponse response = registerConfidentialClient(username);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        String clientSecret = node.at("/client_secret").asText();
        assertNotNull(clientId);
        assertNotNull(clientSecret);
        assertFalse(clientId.contains("a"));

        testListConfidentialClient(username, clientId);
        testConfidentialClientInfo(clientId, username);
        testResetConfidentialClientSecret(clientId, clientSecret);
        deregisterConfidentialClient(username, clientId);
    }
    
    @Test
    public void testRegisterClientNameTooShort ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        OAuth2ClientJson clientJson =
                createOAuth2ClientJson("R", OAuth2ClientType.PUBLIC, null);

        ClientResponse response = registerClient(username, clientJson);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("client_name must contain at least 3 characters",
                node.at("/error_description").asText());
        assertEquals("invalid_request", node.at("/error").asText());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientEmptyName () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        OAuth2ClientJson clientJson =
                createOAuth2ClientJson("", OAuth2ClientType.PUBLIC, null);

        ClientResponse response = registerClient(username, clientJson);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("client_name must contain at least 3 characters",
                node.at("/error_description").asText());
        assertEquals("invalid_request", node.at("/error").asText());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientMissingName ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        OAuth2ClientJson clientJson =
                createOAuth2ClientJson(null, OAuth2ClientType.PUBLIC, null);

        ClientResponse response = registerClient(username, clientJson);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("client_name is null",
                node.at("/error_description").asText());
        assertEquals("invalid_request", node.at("/error").asText());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientMissingDescription ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson("R client",
                OAuth2ClientType.PUBLIC, null);

        ClientResponse response = registerClient(username, clientJson);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("client_description is null",
                node.at("/error_description").asText());
        assertEquals("invalid_request", node.at("/error").asText());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientMissingType ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        OAuth2ClientJson clientJson =
                createOAuth2ClientJson("R client", null, null);

        ClientResponse response = registerClient(username, clientJson);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("client_type is null",
                node.at("/error_description").asText());
        assertEquals("invalid_request", node.at("/error").asText());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testRegisterClientInvalidRedirectURI ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // invalid hostname
        String redirectUri = "https://test.public.client/redirect";
        OAuth2ClientJson clientJson =
                createOAuth2ClientJson("OAuth2PublicClient",
                        OAuth2ClientType.PUBLIC, "A public test client.");
        clientJson.setRedirectURI(redirectUri);
        ClientResponse response = registerClient(username, clientJson);
        testInvalidRedirectUri(response.getEntity(String.class), false,
                response.getStatus());

        // localhost is not allowed
        redirectUri = "http://localhost:1410";
        clientJson.setRedirectURI(redirectUri);
        response = registerClient(username, clientJson);
        testInvalidRedirectUri(response.getEntity(String.class), false,
                response.getStatus());
        
        // fragment is not allowed
        redirectUri = "https://public.client.com/redirect.html#bar";
        clientJson.setRedirectURI(redirectUri);
        response = registerClient(username, clientJson);
        testInvalidRedirectUri(response.getEntity(String.class), false,
                response.getStatus());
    }
    
    @Test
    public void testRegisterPublicClientWithRefreshTokenExpiry ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        OAuth2ClientJson clientJson =
                createOAuth2ClientJson("OAuth2PublicClient",
                        OAuth2ClientType.PUBLIC, "A public test client.");
        clientJson.setRefreshTokenExpiry(31535000);
        ClientResponse response = registerClient(username, clientJson);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals("invalid_request", node.at("/error").asText());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testRegisterConfidentialClientWithRefreshTokenExpiry ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        int expiry = 31535000;
        OAuth2ClientJson clientJson =
                createOAuth2ClientJson("OAuth2 Confidential Client",
                        OAuth2ClientType.CONFIDENTIAL, "A confidential client.");
        clientJson.setRefreshTokenExpiry(expiry);
        ClientResponse response = registerClient(username, clientJson);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String clientId = node.at("/client_id").asText();
        JsonNode clientInfo = retrieveClientInfo(clientId, username);
        assertEquals(expiry, clientInfo.at("/refresh_token_expiry").asInt());
        
        deregisterConfidentialClient(username, clientId);
    }
    
    @Test
    public void testRegisterConfidentialClientWithInvalidRefreshTokenExpiry ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2 Confidential Client", OAuth2ClientType.CONFIDENTIAL,
                "A confidential client.");
        clientJson.setRefreshTokenExpiry(31537000);
        ClientResponse response = registerClient(username, clientJson);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(
                "Maximum refresh token expiry is 31536000 seconds (1 year)",
                node.at("/error_description").asText());
        assertEquals("invalid_request", node.at("/error").asText());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
    
    @Test
    public void testRegisterClientInvalidURL ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        // invalid hostname
        String url = "https://test.public.client";
        OAuth2ClientJson clientJson =
                createOAuth2ClientJson("OAuth2PublicClient",
                        OAuth2ClientType.PUBLIC, "A public test client.");
        clientJson.setUrl(url);
        ClientResponse response = registerClient(username, clientJson);
        testInvalidUrl(response.getEntity(String.class), response.getStatus());

        // localhost is not allowed
        url = "http://localhost:1410";
        clientJson.setRedirectURI(url);
        response = registerClient(username, clientJson);
        testInvalidUrl(response.getEntity(String.class), response.getStatus());
    }
    
    private void testInvalidUrl (String entity, 
            int status) throws KustvaktException {
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Invalid URL",
                node.at("/error_description").asText());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), status);
    }

    @Test
    public void testRegisterPublicClient () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        String redirectUri = "https://public.client.com/redirect";
        OAuth2ClientJson clientJson =
                createOAuth2ClientJson("OAuth2PublicClient",
                        OAuth2ClientType.PUBLIC, "A public test client.");
        // http and fragment are allowed
        clientJson.setUrl("http://public.client.com/index.html#bar");
        clientJson.setRedirectURI(redirectUri);

        ClientResponse response = registerClient(username, clientJson);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());

        testRegisterClientUnauthorizedScope(clientId);
        testResetPublicClientSecret(clientId);
        testAccessTokenAfterDeregistration(clientId, null, "");
    }

    private void testRegisterClientUnauthorizedScope (String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        String userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
        String code = requestAuthorizationCode(clientId, userAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                clientId, clientSecret, code);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));

        assertEquals("match_info search", node.at("/scope").asText());

        String accessToken = node.at("/access_token").asText();

        OAuth2ClientJson clientJson = createOAuth2ClientJson("R client",
                OAuth2ClientType.PUBLIC, null);

        response = resource().path(API_VERSION).path("oauth2").path("client")
                .path("register")
                .header(Attributes.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(clientJson).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
        assertEquals("Scope register_client is not authorized",
                node.at("/errors/0/1").asText());
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testRegisterClientUsingPlainJson ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException, IOException {

        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("json/oauth2_public_client.json");
        String json = IOUtils.toString(is, Charset.defaultCharset());

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("register")
                .header(Attributes.AUTHORIZATION,
                        HttpAuthorizationHandler
                                .createBasicAuthorizationHeaderValue(username,
                                        "password"))
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .entity(json).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());

        testResetPublicClientSecret(clientId);
        testAccessTokenAfterDeregistration(clientId, null, "");
    }

    @Test
    public void testRegisterDesktopApp () throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        OAuth2ClientJson clientJson = createOAuth2ClientJson(
                "OAuth2DesktopClient", OAuth2ClientType.PUBLIC,
                "This is a desktop test client.");

        ClientResponse response = registerClient(username, clientJson);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId = node.at("/client_id").asText();
        assertNotNull(clientId);
        assertTrue(node.at("/client_secret").isMissingNode());

        testDeregisterPublicClientMissingUserAuthentication(clientId);
        testDeregisterPublicClientMissingId();
        testDeregisterPublicClient(clientId, username);
    }

    @Test
    public void testRegisterMultipleDesktopApps ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        // First client
        OAuth2ClientJson clientJson =
                createOAuth2ClientJson("OAuth2DesktopClient1",
                        OAuth2ClientType.PUBLIC, "A desktop test client.");

        ClientResponse response = registerClient(username, clientJson);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        String clientId1 = node.at("/client_id").asText();
        assertNotNull(clientId1);
        assertTrue(node.at("/client_secret").isMissingNode());

        // Second client
        clientJson = createOAuth2ClientJson("OAuth2DesktopClient2",
                OAuth2ClientType.PUBLIC, "Another desktop test client.");

        response = registerClient(username, clientJson);

        entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(entity);
        String clientId2 = node.at("/client_id").asText();
        assertNotNull(clientId2);
        assertTrue(node.at("/client_secret").isMissingNode());

        testResetPublicClientSecret(clientId1);
        testAccessTokenAfterDeregistration(clientId1, null,
                "https://OAuth2DesktopClient1.com");
        testResetPublicClientSecret(clientId2);
        testAccessTokenAfterDeregistration(clientId2, null,
                "https://OAuth2DesktopClient2.com");
    }

    private void testAccessTokenAfterDeregistration (String clientId,
            String clientSecret, String redirectUri) throws KustvaktException {
        String userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");

        String code = requestAuthorizationCode(clientId, redirectUri, userAuthHeader);
        
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                clientId, clientSecret, code, redirectUri);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken = node.at("/access_token").asText();

        response = searchWithAccessToken(accessToken);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        code = requestAuthorizationCode(clientId, redirectUri, userAuthHeader);
        testDeregisterPublicClient(clientId, username);

        response = requestTokenWithAuthorizationCodeAndForm(clientId,
                clientSecret, code, redirectUri);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(OAuth2Error.INVALID_CLIENT.toString(),
                node.at("/error").asText());

        response = searchWithAccessToken(accessToken);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is invalid",
                node.at("/errors/0/1").asText());
    }

    private void testDeregisterPublicClientMissingUserAuthentication (
            String clientId) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").path(clientId)
                .delete(ClientResponse.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.AUTHORIZATION_FAILED,
                node.at("/errors/0/0").asInt());
    }

    private void testDeregisterPublicClientMissingId ()
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("deregister")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(),
                response.getStatus());
    }

    private void testDeregisterPublicClient (String clientId, String username)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("deregister").path(clientId)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .delete(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    private void testResetPublicClientSecret (String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("reset")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_REQUEST, node.at("/error").asText());
        assertEquals("Operation is not allowed for public clients",
                node.at("/error_description").asText());
    }

    private String testResetConfidentialClientSecret (String clientId,
            String clientSecret) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("reset")
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(clientId, node.at("/client_id").asText());

        String newClientSecret = node.at("/client_secret").asText();
        assertTrue(!clientSecret.equals(newClientSecret));

        return newClientSecret;
    }

    private void requestAuthorizedClientList (String userAuthHeader)
            throws KustvaktException {
        MultivaluedMap<String, String> form = getSuperClientForm();
        form.add("authorized_only", "true");

        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("client").path("list")
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        // System.out.println(entity);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.size());
        assertEquals(confidentialClientId, node.at("/0/client_id").asText());
        assertEquals(publicClientId, node.at("/1/client_id").asText());

        assertEquals("non super confidential client",
                node.at("/0/client_name").asText());
        assertEquals("CONFIDENTIAL", node.at("/0/client_type").asText());
        assertFalse(node.at("/0/client_url").isMissingNode());
        assertFalse(node.at("/0/client_description").isMissingNode());
    }

    @Test
    public void testListPublicClient () throws KustvaktException {
        String clientName = "OAuth2DoryClient";
        OAuth2ClientJson json = createOAuth2ClientJson(clientName,
                OAuth2ClientType.PUBLIC, "Dory's client.");
        registerClient("dory", json);

        JsonNode node = listUserRegisteredClients("dory");
        assertEquals(1, node.size());
        assertEquals(clientName, node.at("/0/client_name").asText());
        assertEquals(OAuth2ClientType.PUBLIC.name(),
                node.at("/0/client_type").asText());
        assertTrue(node.at("/0/permitted").asBoolean());
        assertFalse(node.at("/0/registration_date").isMissingNode());
        assertTrue(node.at("/refresh_token_expiry").isMissingNode());
        
        String clientId = node.at("/0/client_id").asText();
        testDeregisterPublicClient(clientId, "dory");
    }
    
    private void testListConfidentialClient (String username, String clientId)
            throws UniformInterfaceException, ClientHandlerException,
            KustvaktException {
        JsonNode node = listUserRegisteredClients(username);
        assertEquals(1, node.size());
        assertEquals(clientId, node.at("/0/client_id").asText());
        assertEquals("OAuth2ClientTest", node.at("/0/client_name").asText());
        assertEquals(OAuth2ClientType.CONFIDENTIAL.name(),
                node.at("/0/client_type").asText());
        assertNotNull(node.at("/0/client_description"));
        assertEquals(clientURL, node.at("/0/client_url").asText());
        assertEquals(clientRedirectUri,
                node.at("/0/client_redirect_uri").asText());
        assertNotNull(node.at("/0/registration_date"));

        assertEquals(defaultRefreshTokenExpiry,
                node.at("/0/refresh_token_expiry").asInt());
        assertTrue(node.at("/0/permitted").asBoolean());
        assertTrue(node.at("/0/source").isMissingNode());
    }
    
    @Test
    public void testListUserClients () throws KustvaktException {
        String username = "pearl";
        String password = "pwd";
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, password);

        // super client
        ClientResponse response = requestTokenWithPassword(superClientId,
                clientSecret, username, password);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // client 1
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(publicClientId, "",
                code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken = node.at("/access_token").asText();

        // client 2
        code = requestAuthorizationCode(confidentialClientId, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        String refreshToken = node.at("/refresh_token").asText();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        requestAuthorizedClientList(userAuthHeader);
        testListAuthorizedClientWithMultipleRefreshTokens(userAuthHeader);
        testListAuthorizedClientWithMultipleAccessTokens(userAuthHeader);
        testListWithClientsFromAnotherUser(userAuthHeader);

        // revoke client 1
        testRevokeAllTokenViaSuperClient(publicClientId, userAuthHeader,
                accessToken);

        // revoke client 2
        node = JsonUtils.readTree(response.getEntity(String.class));
        accessToken = node.at("/access_token").asText();
        refreshToken = node.at("/refresh_token").asText();
        testRevokeAllTokenViaSuperClient(confidentialClientId, userAuthHeader,
                accessToken);
        testRequestTokenWithRevokedRefreshToken(confidentialClientId,
                clientSecret, refreshToken);
    }

    private void testListAuthorizedClientWithMultipleRefreshTokens (
            String userAuthHeader) throws KustvaktException {
        // client 2
        String code =
                requestAuthorizationCode(confidentialClientId, userAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        requestAuthorizedClientList(userAuthHeader);
    }

    private void testListAuthorizedClientWithMultipleAccessTokens (
            String userAuthHeader) throws KustvaktException {
        // client 1
        String code = requestAuthorizationCode(publicClientId, userAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, "", code);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        requestAuthorizedClientList(userAuthHeader);
    }

    private void testListWithClientsFromAnotherUser (String userAuthHeader)
            throws KustvaktException {

        String aaaAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("aaa", "pwd");

        // client 1
        String code = requestAuthorizationCode(publicClientId, aaaAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, "", code);

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken1 = node.at("/access_token").asText();

        // client 2
        code = requestAuthorizationCode(confidentialClientId, aaaAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);

        node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken2 = node.at("/access_token").asText();
        String refreshToken = node.at("/refresh_token").asText();

        requestAuthorizedClientList(aaaAuthHeader);
        requestAuthorizedClientList(userAuthHeader);

        testRevokeAllTokenViaSuperClient(publicClientId, aaaAuthHeader,
                accessToken1);
        testRevokeAllTokenViaSuperClient(confidentialClientId, aaaAuthHeader,
                accessToken2);
        testRequestTokenWithRevokedRefreshToken(confidentialClientId,
                clientSecret, refreshToken);
    }
    
    private void testRevokeAllTokenViaSuperClient (String clientId,
            String userAuthHeader, String accessToken)
            throws KustvaktException {
        // check token before revoking
        ClientResponse response = searchWithAccessToken(accessToken);
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertTrue(node.at("/matches").size() > 0);

        MultivaluedMap<String, String> form = getSuperClientForm();
        form.add("client_id", clientId);

        response = resource().path(API_VERSION).path("oauth2").path("revoke")
                .path("super").path("all")
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertEquals("SUCCESS", response.getEntity(String.class));

        response = searchWithAccessToken(accessToken);
        node = JsonUtils.readTree(response.getEntity(String.class));
        assertEquals(StatusCodes.INVALID_ACCESS_TOKEN,
                node.at("/errors/0/0").asInt());
        assertEquals("Access token is invalid",
                node.at("/errors/0/1").asText());
    }
}
