package de.ids_mannheim.korap.oauth2.oltu.service;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sun.jersey.api.client.ClientResponse.Status;

import de.ids_mannheim.korap.encryption.RandomCodeGenerator;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;
import de.ids_mannheim.korap.oauth2.service.OAuth2AuthorizationService;

/**
 * OAuth2 authorization service using Apache Oltu
 * 
 * @author margaretha
 *
 */
@Service
public class OltuAuthorizationService extends OAuth2AuthorizationService {

    @Autowired
    private RandomCodeGenerator codeGenerator;

    /**
     * Authorization code request does not require client
     * authentication, but only checks if the client id exists.
     * 
     * @param request
     * @param authzRequest
     * @param username
     * @param authTime
     * @return redirect URI containing authorization code if
     *         successful.
     * 
     * @throws KustvaktException
     * @throws OAuthSystemException
     */
    public String requestAuthorizationCode (HttpServletRequest request,
            OAuthAuthzRequest authzRequest, String username,
            ZonedDateTime authenticationTime)
            throws OAuthSystemException, KustvaktException {

        String clientId = authzRequest.getClientId();
        OAuth2Client client = clientService.authenticateClientId(clientId);

        String redirectUriStr = authzRequest.getRedirectURI();
        String verifiedRedirectUri = verifyRedirectUri(client, redirectUriStr);

        URI redirectURI;
        try {
            redirectURI = new URI(verifiedRedirectUri);
        }
        catch (URISyntaxException e) {
            throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                    "Invalid redirect URI", OAuth2Error.INVALID_REQUEST);
        }

        String scope, code;
        try {
            checkResponseType(authzRequest.getResponseType());
            code = URLEncoder.encode(codeGenerator.createRandomCode(), "UTF-8");
            scope = createAuthorization(username, authzRequest.getClientId(),
                    redirectUriStr, authzRequest.getScopes(), code,
                    authenticationTime, null);
        }
        catch (KustvaktException e) {
            e.setRedirectUri(redirectURI);
            throw e;
        }
        catch (UnsupportedEncodingException e) {
            KustvaktException ke = new KustvaktException(
                    StatusCodes.GENERAL_ERROR, e.getMessage());
            ke.setRedirectUri(redirectURI);
            throw ke;
        }

        OAuthResponse oAuthResponse;
        try {
            oAuthResponse = OAuthASResponse
                    .authorizationResponse(request,
                            Status.FOUND.getStatusCode())
                    .setCode(code).setScope(scope).location(verifiedRedirectUri)
                    .buildQueryMessage();
        }
        catch (OAuthSystemException e) {
            // Should not happen
            KustvaktException ke =
                    new KustvaktException(StatusCodes.OAUTH2_SYSTEM_ERROR,
                            e.getMessage(), OAuth2Error.SERVER_ERROR);
            ke.setRedirectUri(redirectURI);
            throw ke;
        }
        return oAuthResponse.getLocationUri();
    }
}
