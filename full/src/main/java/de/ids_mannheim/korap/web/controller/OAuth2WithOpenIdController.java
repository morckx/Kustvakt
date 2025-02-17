package de.ids_mannheim.korap.web.controller;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.id.State;
import de.ids_mannheim.korap.web.utils.ResourceFilters;

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.openid.OpenIdConfiguration;
import de.ids_mannheim.korap.oauth2.openid.OpenIdHttpRequestWrapper;
import de.ids_mannheim.korap.oauth2.openid.service.JWKService;
import de.ids_mannheim.korap.oauth2.openid.service.OpenIdAuthorizationService;
import de.ids_mannheim.korap.oauth2.openid.service.OpenIdConfigService;
import de.ids_mannheim.korap.oauth2.openid.service.OpenIdTokenService;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.OpenIdResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.utils.MapUtils;

/**
 * Describes OAuth2 webAPI with OpenId Connect implementation, an
 * additional authentication protocol allowing clients to verify
 * user authentication data represented by ID tokens.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("{version}/oauth2/openid")
@ResourceFilters({ APIVersionFilter.class })
public class OAuth2WithOpenIdController {

    @Autowired
    private OpenIdAuthorizationService authzService;
    @Autowired
    private OpenIdTokenService tokenService;
    @Autowired
    private JWKService jwkService;
    @Autowired
    private OpenIdConfigService configService;
    @Autowired
    private OAuth2ScopeService scopeService;

    @Autowired
    private OpenIdResponseHandler openIdResponseHandler;

    /**
     * Required parameters for OpenID authentication requests:
     * 
     * <ul>
     * <li>scope: MUST contain "openid" for OpenID Connect
     * requests</li>
     * <li>response_type: only "code" is supported</li>
     * <li>client_id: client identifier given by Kustvakt during
     * client registration</li>
     * <li>redirect_uri: MUST match a pre-registered redirect uri
     * during client registration</li>
     * </ul>
     * 
     * Other parameters:
     * 
     * <ul>
     * <li>state (recommended): Opaque value used to maintain state
     * between the request and the callback.</li>
     * <li>response_mode (optional) : mechanism to be used for
     * returning parameters, only "query" is supported</li>
     * <li>nonce (optional): String value used to associate a Client
     * session with an ID Token,
     * and to mitigate replay attacks. </li>
     * <li>display (optional): specifies how the Authorization Server
     * displays the authentication and consent user interface
     * pages. Options: page (default), popup, touch, wap. This
     * parameter is more relevant for Kalamar. </li>
     * <li>prompt (optional): specifies if the Authorization Server
     * prompts the End-User for reauthentication and consent. Defined
     * values: none, login, consent, select_account </li>
     * <li>max_age (optional): maximum Authentication Age.</li>
     * <li>ui_locales (optional): preferred languages and scripts for
     * the user interface represented as a space-separated list of
     * BCP47 [RFC5646] </li>
     * <li>id_token_hint (optional): ID Token previously issued by the
     * Authorization Server being passed as a hint</li>
     * <li>login_hint (optional): hint to the Authorization Server
     * about the login identifier the End-User might use to log
     * in</li>
     * <li>acr_values (optional): requested Authentication Context
     * Class Reference values. </li>
     * </ul>
     * 
     * @see "OpenID Connect Core 1.0 specification"
     * 
     * @param request
     * @param context
     * @param form
     * @return a redirect to client redirect uri
     */
    @POST
    @Path("authorize")
    @ResourceFilters({ AuthenticationFilter.class, BlockingFilter.class })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response requestAuthorizationCode (
            @Context HttpServletRequest request,
            @Context SecurityContext context,
            MultivaluedMap<String, String> form) {

        TokenContext tokenContext = (TokenContext) context.getUserPrincipal();
        String username = tokenContext.getUsername();
        ZonedDateTime authTime = tokenContext.getAuthenticationTime();

        Map<String, String> map = MapUtils.toMap(form);
        State state = authzService.retrieveState(map);
        ResponseMode responseMode = authzService.retrieveResponseMode(map);

        boolean isAuthentication = false;
        if (map.containsKey("scope") && map.get("scope").contains("openid")) {
            isAuthentication = true;
        }

        URI uri = null;
        try {
            scopeService.verifyScope(tokenContext, OAuth2Scope.AUTHORIZE);

            if (isAuthentication) {
                authzService.checkRedirectUriParam(map);
            }
            uri = authzService.requestAuthorizationCode(form, username,
                    isAuthentication, authTime);
        }
        catch (ParseException e) {
            return openIdResponseHandler.createErrorResponse(e, state);
        }
        catch (KustvaktException e) {
            return openIdResponseHandler.createAuthorizationErrorResponse(e,
                    isAuthentication, e.getRedirectUri(), state, responseMode);
        }

        ResponseBuilder builder = Response.temporaryRedirect(uri);
        return builder.build();
    }

    @POST
    @Path("token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response requestAccessToken (
            @Context HttpServletRequest servletRequest,
            MultivaluedMap<String, String> form) {

        Map<String, String> map = MapUtils.toMap(form);
        Method method = Method.valueOf(servletRequest.getMethod());
        URL url = null;
        try {
            url = new URL(servletRequest.getRequestURL().toString());
        }
        catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            OpenIdHttpRequestWrapper httpRequest =
                    new OpenIdHttpRequestWrapper(method, url);
            httpRequest.toHttpRequest(servletRequest, (Map<String, List<String>>) form);

            TokenRequest tokenRequest = TokenRequest.parse(httpRequest);
            AccessTokenResponse tokenResponse =
                    tokenService.requestAccessToken(tokenRequest);
            return openIdResponseHandler.createResponse(tokenResponse,
                    Status.OK);
        }
        catch (ParseException e) {
            return openIdResponseHandler.createErrorResponse(e, null);
        }
        catch (KustvaktException e) {
            return openIdResponseHandler.createTokenErrorResponse(e);
        }
    }

    /**
     * Retrieves Kustvakt public keys of JWK (Json Web Key) set
     * format.
     * 
     * @return json string representation of the public keys
     * 
     * @see "RFC 8017 regarding RSA specifications"
     * @see "RFC 7517 regarding JWK (Json Web Key) and JWK Set"
     */
    @GET
    @Path("jwks")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String requestPublicKeys () {
        return jwkService.generatePublicKeySetJson();
    }

    /**
     * When supporting discovery, must be available at
     * {issuer_uri}/.well-known/openid-configuration
     * 
     * @return
     * 
     * @return
     */
    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public OpenIdConfiguration requestOpenIdConfig () {
        return configService.retrieveOpenIdConfigInfo();
    }
}
