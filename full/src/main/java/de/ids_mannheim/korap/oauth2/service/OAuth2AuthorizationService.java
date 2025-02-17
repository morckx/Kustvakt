package de.ids_mannheim.korap.oauth2.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.dao.AuthorizationDao;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.oauth2.entity.Authorization;
import de.ids_mannheim.korap.oauth2.entity.OAuth2Client;

/** Describes business logic behind OAuth2 authorization requests.
 * 
 * @author margaretha
 *
 */
@Service(value = "authorizationService")
public class OAuth2AuthorizationService {

    public static Logger jlog =
            LogManager.getLogger(OAuth2AuthorizationService.class);

    public static boolean DEBUG = false;
    
    @Autowired
    protected OAuth2ClientService clientService;
    @Autowired
    protected OAuth2ScopeServiceImpl scopeService;
    @Autowired
    private AuthorizationDao authorizationDao;
    @Autowired
    private UrlValidator redirectURIValidator;

    @Autowired
    protected FullConfiguration config;

    /**
     * Authorization code request does not require client
     * authentication, but only checks if the client id exists.
     * 
     * @param username
     * @param clientId
     * @param redirectUri
     * @param scopeSet
     * @param code
     * @param authenticationTime
     *            user authentication time
     * @param nonce
     * @return
     * @throws KustvaktException
     */
    public String createAuthorization (String username, String clientId,
            String redirectUri, Set<String> scopeSet, String code,
            ZonedDateTime authenticationTime, String nonce)
            throws KustvaktException {

        if (scopeSet == null || scopeSet.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "scope is required", OAuth2Error.INVALID_SCOPE);
        }
        Set<AccessScope> scopes = scopeService.convertToAccessScope(scopeSet);

        authorizationDao.storeAuthorizationCode(clientId, username, code,
                scopes, redirectUri, authenticationTime, nonce);
        return String.join(" ", scopeSet);
    }

    @Deprecated
    protected void checkResponseType (String responseType)
            throws KustvaktException {
        if (responseType == null || responseType.isEmpty()) {
            throw new KustvaktException(StatusCodes.MISSING_PARAMETER,
                    "response_type is missing.", OAuth2Error.INVALID_REQUEST);
        }
        else if (!responseType.equals("code")) {
            throw new KustvaktException(StatusCodes.NOT_SUPPORTED,
                    "unsupported response_type: " + responseType,
                    OAuth2Error.INVALID_REQUEST);
        }
    }

    /**
     * If the request contains a redirect_uri parameter, the server
     * must confirm it is a valid redirect URI.
     * 
     * If there is no redirect_uri parameter in the request, and only
     * one URI was registered, the server uses the redirect URL that
     * was previously registered.
     * 
     * If no redirect URL has been registered, this is an error.
     * 
     * @param client
     *            an OAuth2Client
     * @param hasRedirectUri
     *            true if request contains redirect_uri, false
     *            otherwise
     * @param redirectUri
     *            the redirect_uri value
     * @return a client's redirect URI
     * @throws KustvaktException
     */
    public String verifyRedirectUri (OAuth2Client client, String redirectUri)
            throws KustvaktException {

        String registeredUri = client.getRedirectURI();
        
        if (redirectUri != null && !redirectUri.isEmpty()) {
            // check if the redirect URI the same as that in DB
            if (!redirectURIValidator.isValid(redirectUri) ||
                    (registeredUri != null && !registeredUri.isEmpty()
                    && !redirectUri.equals(registeredUri))) {
                throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                        "Invalid redirect URI", OAuth2Error.INVALID_REQUEST);
            }
        }
        // redirect_uri is not required in client registration
        else if (registeredUri != null && !registeredUri.isEmpty()) {
                redirectUri = registeredUri;
        }
        else {
            throw new KustvaktException(StatusCodes.MISSING_REDIRECT_URI,
                    "Missing parameter: redirect URI",
                    OAuth2Error.INVALID_REQUEST);
        }

        return redirectUri;
    }

    public Authorization retrieveAuthorization (String code)
            throws KustvaktException {
        return authorizationDao.retrieveAuthorizationCode(code);
    }

    public Authorization verifyAuthorization (Authorization authorization,
            String clientId, String redirectURI) throws KustvaktException {

        // EM: can Kustvakt be specific about the invalid grant error
        // description?
        if (!authorization.getClientId().equals(clientId)) {
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Invalid authorization", OAuth2Error.INVALID_GRANT);
        }
        if (authorization.isRevoked()) {
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Invalid authorization", OAuth2Error.INVALID_GRANT);
        }

        if (isExpired(authorization.getExpiryDate())) {
            throw new KustvaktException(StatusCodes.INVALID_AUTHORIZATION,
                    "Authorization expired", OAuth2Error.INVALID_GRANT);
        }

        String authorizedUri = authorization.getRedirectURI();
        if (authorizedUri != null && !authorizedUri.isEmpty()) {
            if (redirectURI == null || redirectURI.isEmpty()) {
                throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                        "Missing redirect URI", OAuth2Error.INVALID_GRANT);
            }    
            if (!authorizedUri.equals(redirectURI)) {
                throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                        "Invalid redirect URI", OAuth2Error.INVALID_GRANT);
            }
        }
        else if (redirectURI != null && !redirectURI.isEmpty()) {
            throw new KustvaktException(StatusCodes.INVALID_REDIRECT_URI,
                    "Invalid redirect URI", OAuth2Error.INVALID_GRANT);
        }

        authorization.setRevoked(true);
        authorization = authorizationDao.updateAuthorization(authorization);

        return authorization;
    }

    public void addTotalAttempts (Authorization authorization)
            throws KustvaktException {
        int totalAttempts = authorization.getTotalAttempts() + 1;
        if (totalAttempts == config.getMaxAuthenticationAttempts()) {
            authorization.setRevoked(true);
        }
        authorization.setTotalAttempts(totalAttempts);
        authorizationDao.updateAuthorization(authorization);
    }

    private boolean isExpired (ZonedDateTime expiryDate) {
        ZonedDateTime now =
                ZonedDateTime.now(ZoneId.of(Attributes.DEFAULT_TIME_ZONE));
        if (DEBUG) {
            jlog.debug("createdDate: " + expiryDate);
            jlog.debug("expiration: " + expiryDate + ", now: " + now);
        }
        if (expiryDate.isAfter(now)) {
            return false;
        }
        return true;
    }
}
