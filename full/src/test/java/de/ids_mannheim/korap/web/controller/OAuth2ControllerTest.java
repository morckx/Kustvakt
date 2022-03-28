package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.TokenType;
import org.junit.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.RefreshToken;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha
 *
 */
public class OAuth2ControllerTest extends OAuth2TestBase {

    public String userAuthHeader;

    public OAuth2ControllerTest () throws KustvaktException {
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("dory", "password");
    }

    @Test
    public void testAuthorizeConfidentialClient () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", confidentialClientId);
        form.add("state", "thisIsMyState");

        ClientResponse response =
                requestAuthorizationCode(form, userAuthHeader);

        assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(),
                response.getStatus());
        URI redirectUri = response.getLocation();
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(redirectUri).build().getQueryParams();
        assertNotNull(params.getFirst("code"));
        assertEquals("thisIsMyState", params.getFirst("state"));
    }

    @Test
    public void testAuthorizePublicClient () throws KustvaktException {
        String code = requestAuthorizationCode(publicClientId, clientSecret,
                null, userAuthHeader);
        assertNotNull(code);
    }

    @Test
    public void testAuthorizeInvalidRedirectUri () throws KustvaktException {
        String redirectUri = "https://different.uri/redirect";

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", confidentialClientId);
        form.add("redirect_uri", redirectUri);
        form.add("state", "thisIsMyState");
        ClientResponse response =
                requestAuthorizationCode(form, userAuthHeader);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Invalid redirect URI",
                node.at("/error_description").asText());
        assertEquals("thisIsMyState", node.at("/state").asText());
    }

    @Test
    public void testAuthorizeMissingRequiredParameters ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("state", "thisIsMyState");
        // missing response_type
        ClientResponse response =
                requestAuthorizationCode(form, userAuthHeader);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing response_type parameter value",
                node.at("/error_description").asText());
        assertEquals("thisIsMyState", node.at("/state").asText());

        // missing client_id
        form.add("response_type", "code");
        response = requestAuthorizationCode(form, userAuthHeader);
        entity = response.getEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals("Missing parameters: client_id",
                node.at("/error_description").asText());
    }

    @Test
    public void testAuthorizeInvalidResponseType () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "string");
        form.add("state", "thisIsMyState");

        ClientResponse response =
                requestAuthorizationCode(form, userAuthHeader);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.CodeResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Invalid response_type parameter value",
                node.at("/error_description").asText());
        assertEquals("thisIsMyState", node.at("/state").asText());
    }

    @Test
    public void testAuthorizeInvalidScope () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("response_type", "code");
        form.add("client_id", confidentialClientId);
        form.add("scope", "read_address");
        form.add("state", "thisIsMyState");

        ClientResponse response =
                requestAuthorizationCode(form, userAuthHeader);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        URI location = response.getLocation();
        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(location).build().getQueryParams();
        assertEquals(OAuth2Error.INVALID_SCOPE, params.getFirst("error"));
        assertEquals("read_address+is+an+invalid+scope",
                params.getFirst("error_description"));
        assertEquals("thisIsMyState", params.getFirst("state"));
    }

    @Test
    public void testRequestTokenAuthorizationPublic ()
            throws KustvaktException {
        String code = requestAuthorizationCode(publicClientId, "", null,
                userAuthHeader);

        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                publicClientId, clientSecret, code);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        String accessToken = node.at("/access_token").asText();

        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertEquals(31536000, node.at("/expires_in").asInt());

        testRevokeToken(accessToken, publicClientId, null, ACCESS_TOKEN_TYPE);

        assertTrue(node.at("/refresh_token").isMissingNode());
    }

    @Test
    public void testRequestTokenAuthorizationConfidential ()
            throws KustvaktException {

        MultivaluedMap<String, String> authForm = new MultivaluedMapImpl();
        authForm.add("response_type", "code");
        authForm.add("client_id", confidentialClientId);
        authForm.add("scope", "search");

        ClientResponse response =
                requestAuthorizationCode(authForm, userAuthHeader);
        URI redirectUri = response.getLocation();
        MultivaluedMap<String, String> params =
                UriComponent.decodeQuery(redirectUri, true);
        String code = params.get("code").get(0);
        String scopes = params.get("scope").get(0);

        assertEquals(scopes, "search");

        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());

        testRequestTokenWithUsedAuthorization(code);

        String refreshToken = node.at("/refresh_token").asText();

        testRequestRefreshTokenInvalidScope(confidentialClientId, refreshToken);
        testRequestRefreshTokenInvalidClient(refreshToken);
        testRequestRefreshTokenInvalidRefreshToken(confidentialClientId);

        testRequestRefreshToken(confidentialClientId, clientSecret, refreshToken);
    }

    private void testRequestTokenWithUsedAuthorization (String code)
            throws KustvaktException {
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        String entity = response.getEntity(String.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_GRANT,
                node.at("/error").asText());
        assertEquals("Invalid authorization",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenInvalidAuthorizationCode ()
            throws KustvaktException {
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, "blahblah");
        String entity = response.getEntity(String.class);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
    }

    @Test
    public void testRequestTokenAuthorizationReplyAttack ()
            throws KustvaktException {
        String uri = "https://third.party.com/confidential/redirect";
        MultivaluedMap<String, String> authForm = new MultivaluedMapImpl();
        authForm.add("response_type", "code");
        authForm.add("client_id", confidentialClientId);
        authForm.add("scope", "search");
        authForm.add("redirect_uri", uri);

        ClientResponse response =
                requestAuthorizationCode(authForm, userAuthHeader);
        URI redirectUri = response.getLocation();
        MultivaluedMap<String, String> params =
                UriComponent.decodeQuery(redirectUri, true);
        String code = params.get("code").get(0);

        testRequestTokenAuthorizationInvalidClient(code);
        testRequestTokenAuthorizationInvalidRedirectUri(code);
        testRequestTokenAuthorizationRevoked(code, uri);
    }

    private void testRequestTokenAuthorizationInvalidClient (String code)
            throws KustvaktException {
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, "wrong_secret", code);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_CLIENT, node.at("/error").asText());
    }

    private void testRequestTokenAuthorizationInvalidRedirectUri (String code)
            throws KustvaktException {
        MultivaluedMap<String, String> tokenForm = new MultivaluedMapImpl();
        tokenForm.add("grant_type", "authorization_code");
        tokenForm.add("client_id", confidentialClientId);
        tokenForm.add("client_secret", "secret");
        tokenForm.add("code", code);
        tokenForm.add("redirect_uri", "https://blahblah.com");

        ClientResponse response = requestToken(tokenForm);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_GRANT, node.at("/error").asText());
    }

    private void testRequestTokenAuthorizationRevoked (String code, String uri)
            throws KustvaktException {
        MultivaluedMap<String, String> tokenForm = new MultivaluedMapImpl();
        tokenForm.add("grant_type", "authorization_code");
        tokenForm.add("client_id", confidentialClientId);
        tokenForm.add("client_secret", "secret");
        tokenForm.add("code", code);
        tokenForm.add("redirect_uri", uri);

        ClientResponse response = requestToken(tokenForm);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_GRANT,
                node.at("/error").asText());
        assertEquals("Invalid authorization",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantConfidentialSuper ()
            throws KustvaktException {
        ClientResponse response =
                requestTokenWithDoryPassword(superClientId, clientSecret);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());

        RefreshToken refreshToken = refreshTokenDao
                .retrieveRefreshToken(node.at("/refresh_token").asText());
        Set<AccessScope> scopes = refreshToken.getScopes();
        assertEquals(1, scopes.size());
        assertEquals("[all]", scopes.toString());
    }

    @Test
    public void testRequestTokenPasswordGrantConfidentialNonSuper ()
            throws KustvaktException {
        ClientResponse response = requestTokenWithDoryPassword(
                confidentialClientId, clientSecret);
        String entity = response.getEntity(String.class);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT,
                node.at("/error").asText());
        assertEquals("Password grant is not allowed for third party clients",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantPublic ()
            throws KustvaktException {
        ClientResponse response =
                requestTokenWithDoryPassword(publicClientId, "");
        String entity = response.getEntity(String.class);

        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.UNAUTHORIZED_CLIENT,
                node.at("/error").asText());
        assertEquals("Password grant is not allowed for third party clients",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantAuthorizationHeader ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("client_id", superClientId);
        form.add("username", "dory");
        form.add("password", "password");

        ClientResponse response =
                resource().path(API_VERSION).path("oauth2").path("token")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Basic ZkNCYlFrQXlZekk0TnpVeE1nOnNlY3JldA==")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .entity(form).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }

    /**
     * In case, client_id is specified both in Authorization header
     * and request body, client_id in the request body is ignored.
     * 
     * @throws KustvaktException
     */
    @Test
    public void testRequestTokenPasswordGrantDifferentClientIds ()
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "password");
        form.add("client_id", "9aHsGW6QflV13ixNpez");
        form.add("username", "dory");
        form.add("password", "password");

        ClientResponse response =
                resource().path(API_VERSION).path("oauth2").path("token")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Basic ZkNCYlFrQXlZekk0TnpVeE1nOnNlY3JldA==")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .entity(form).post(ClientResponse.class);
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantMissingClientSecret ()
            throws KustvaktException {
        ClientResponse response =
                requestTokenWithDoryPassword(confidentialClientId, "");
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameters: client_secret",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenPasswordGrantMissingClientId ()
            throws KustvaktException {
        ClientResponse response =
                requestTokenWithDoryPassword(null, clientSecret);
        String entity = response.getEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameters: client_id",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenClientCredentialsGrant ()
            throws KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "client_credentials");
        form.add("client_id", confidentialClientId);
        form.add("client_secret", "secret");
        ClientResponse response = requestToken(form);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        // length?
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
    }

    /**
     * Client credentials grant is only allowed for confidential
     * clients.
     */
    @Test
    public void testRequestTokenClientCredentialsGrantPublic ()
            throws KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "client_credentials");
        form.add("client_id", publicClientId);
        form.add("client_secret", "");
        ClientResponse response = requestToken(form);

        String entity = response.getEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
        assertEquals("Missing parameters: client_secret",
                node.at("/error_description").asText());
    }

    @Test
    public void testRequestTokenClientCredentialsGrantReducedScope ()
            throws KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "client_credentials");
        form.add("client_id", confidentialClientId);
        form.add("client_secret", "secret");
        form.add("scope", "preferred_username client_info");

        ClientResponse response = requestToken(form);
        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        // length?
        assertNotNull(node.at("/access_token").asText());
        assertNotNull(node.at("/refresh_token").asText());
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());
        assertEquals("client_info", node.at("/scope").asText());
    }

    @Test
    public void testRequestTokenMissingGrantType () throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        ClientResponse response = requestToken(form);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.at("/error").asText());
    }

    @Test
    public void testRequestTokenUnsupportedGrant () throws KustvaktException {

        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", "blahblah");

        ClientResponse response =
                resource().path(API_VERSION).path("oauth2").path("token")
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertEquals("Invalid grant_type parameter value",
                node.get("error_description").asText());
        assertEquals(OAuthError.TokenResponse.INVALID_REQUEST,
                node.get("error").asText());
    }

    private void testRequestRefreshTokenInvalidScope (String clientId,
            String refreshToken) throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("scope", "search serialize_query");

        ClientResponse response =
                resource().path(API_VERSION).path("oauth2").path("token")
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_SCOPE, node.at("/error").asText());
    }

    private void testRequestRefreshToken (String clientId, String clientSecret,
            String refreshToken) throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);

        ClientResponse response =
                resource().path(API_VERSION).path("oauth2").path("token")
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node.at("/access_token").asText());
        
        String newRefreshToken = node.at("/refresh_token").asText();
        assertNotNull(newRefreshToken);
        assertEquals(TokenType.BEARER.toString(),
                node.at("/token_type").asText());
        assertNotNull(node.at("/expires_in").asText());

        assertTrue(!newRefreshToken.equals(refreshToken));
        
        testRequestTokenWithRevokedRefreshToken(clientId, clientSecret,
                refreshToken);
        
        testRevokeToken(newRefreshToken, clientId, clientSecret,
                REFRESH_TOKEN_TYPE);
        testRequestTokenWithRevokedRefreshToken(clientId, clientSecret,
                newRefreshToken);
    }

    private void testRequestRefreshTokenInvalidClient (String refreshToken)
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.add("client_id", "iBr3LsTCxOj7D2o0A5m");
        form.add("refresh_token", refreshToken);

        ClientResponse response =
                resource().path(API_VERSION).path("oauth2").path("token")
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_CLIENT, node.at("/error").asText());
    }

    private void testRequestRefreshTokenInvalidRefreshToken (String clientId)
            throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("grant_type", GrantType.REFRESH_TOKEN.toString());
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", "Lia8s8w8tJeZSBlaQDrYV8ion3l");

        ClientResponse response =
                resource().path(API_VERSION).path("oauth2").path("token")
                        .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                        .header(HttpHeaders.CONTENT_TYPE,
                                ContentType.APPLICATION_FORM_URLENCODED)
                        .entity(form).post(ClientResponse.class);

        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(OAuth2Error.INVALID_GRANT, node.at("/error").asText());
    }

    private JsonNode requestTokenList (String userAuthHeader, String tokenType,
            String clientId) throws KustvaktException {
        MultivaluedMap<String, String> form = new MultivaluedMapImpl();
        form.add("super_client_id", superClientId);
        form.add("super_client_secret", clientSecret);
        form.add("token_type", tokenType);

        if (clientId != null && !clientId.isEmpty()){
            form.add("client_id", clientId);
        }
            
        ClientResponse response = resource().path(API_VERSION).path("oauth2")
                .path("token").path("list")
                .header(Attributes.AUTHORIZATION, userAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE,
                        ContentType.APPLICATION_FORM_URLENCODED)
                .entity(form).post(ClientResponse.class);

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        String entity = response.getEntity(String.class);
        return JsonUtils.readTree(entity);
    }
    
    private JsonNode requestTokenList (String userAuthHeader, String tokenType)
            throws KustvaktException {
        return requestTokenList(userAuthHeader, tokenType, null);
    }
    
    @Test
    public void testListRefreshTokenConfidentialClient () throws KustvaktException {
        String username = "gurgle";
        String password = "pwd";
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, password);

        // super client
        ClientResponse response = requestTokenWithPassword(superClientId,
                clientSecret, username, password);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String refreshToken1 = node.at("/refresh_token").asText();

        // client 1
        String code = requestAuthorizationCode(confidentialClientId,
                clientSecret, null, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // client 2
        code = requestAuthorizationCode(confidentialClientId2, clientSecret,
                null, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId2, clientSecret, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // list
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(2, node.size());
        assertEquals(confidentialClientId, node.at("/0/client_id").asText());
        assertEquals(confidentialClientId2, node.at("/1/client_id").asText());

        // client 1
        code = requestAuthorizationCode(confidentialClientId, clientSecret,
                null, userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        // another user
        String darlaAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue("darla", "pwd");

        // test listing clients
        node = requestTokenList(darlaAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(0, node.size());

        // client 1
        code = requestAuthorizationCode(confidentialClientId, clientSecret,
                null, darlaAuthHeader);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        response = requestTokenWithAuthorizationCodeAndForm(
                confidentialClientId, clientSecret, code);

        node = JsonUtils.readTree(response.getEntity(String.class));
        String refreshToken5 = node.at("/refresh_token").asText();

        // list all refresh tokens
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(3, node.size());

        // list refresh tokens from client 1
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE, confidentialClientId);
        assertEquals(2, node.size());
        
        testRevokeToken(refreshToken1, superClientId, clientSecret,
                REFRESH_TOKEN_TYPE);
        testRevokeToken(node.at("/0/token").asText(), confidentialClientId,
                clientSecret, REFRESH_TOKEN_TYPE);
        testRevokeToken(node.at("/1/token").asText(), confidentialClientId2,
                clientSecret, REFRESH_TOKEN_TYPE);

        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(1, node.size());

        testRevokeTokenViaSuperClient(node.at("/0/token").asText(),
                userAuthHeader);
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(0, node.size());

        // try revoking a token belonging to another user
        // should not return any errors
        testRevokeTokenViaSuperClient(refreshToken5, userAuthHeader);
        node = requestTokenList(darlaAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(1, node.size());

        testRevokeTokenViaSuperClient(refreshToken5, darlaAuthHeader);
        node = requestTokenList(darlaAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(0, node.size());
    }


    @Test
    public void testListTokenPublicClient () throws KustvaktException {
        String username = "nemo";
        String password = "pwd";
        userAuthHeader = HttpAuthorizationHandler
                .createBasicAuthorizationHeaderValue(username, password);

        // access token 1
        String code = requestAuthorizationCode(publicClientId, clientSecret,
                null, userAuthHeader);
        ClientResponse response = requestTokenWithAuthorizationCodeAndForm(publicClientId, "",
                code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken1 = node.at("/access_token").asText();

        // access token 2
        code = requestAuthorizationCode(publicClientId, clientSecret, null,
                userAuthHeader);
        response = requestTokenWithAuthorizationCodeAndForm(publicClientId, "",
                code);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        node = JsonUtils.readTree(response.getEntity(String.class));
        String accessToken2 = node.at("/access_token").asText();
        
        // list access tokens
        node = requestTokenList(userAuthHeader, ACCESS_TOKEN_TYPE);
        assertEquals(2, node.size());
        
        // list refresh tokens
        node = requestTokenList(userAuthHeader, REFRESH_TOKEN_TYPE);
        assertEquals(0, node.size());
        
        testRevokeTokenViaSuperClient(accessToken1, userAuthHeader);
        node = requestTokenList(userAuthHeader, ACCESS_TOKEN_TYPE);
//        System.out.println(node);
        assertEquals(1, node.size());
        assertEquals(accessToken2, node.at("/0/token").asText());
        assertTrue(node.at("/0/scope").size()>0);
        assertNotNull(node.at("/0/created_date").asText());
        assertNotNull(node.at("/0/expires_in").asLong());
        assertNotNull(node.at("/0/user_authentication_time").asText());
        
        assertEquals(publicClientId, node.at("/0/client_id").asText());
        assertNotNull(node.at("/0/client_name").asText());
        assertNotNull(node.at("/0/client_description").asText());
        assertNotNull(node.at("/0/client_url").asText());
        
        
        testRevokeTokenViaSuperClient(accessToken2, userAuthHeader);
        node = requestTokenList(userAuthHeader, ACCESS_TOKEN_TYPE);
        assertEquals(0, node.size());
    }
}
