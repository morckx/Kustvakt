package de.ids_mannheim.korap.web.filter;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.authentication.AuthenticationManager;
import de.ids_mannheim.korap.authentication.http.AuthorizationData;
import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.context.KustvaktContext;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.utils.TimeUtils;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;

/** Authentication filter extracts an authentication token from 
 * authorization header and uses an authentication provider 
 * with respect to the token type to create a token context as
 * a security context.
 * 
 * @author hanl, margaretha
 * @date 28/01/2014
 * @last update 12/2017
 */
@Component
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter
        implements ContainerRequestFilter {

    private static Logger jlog = LogManager.getLogger(AuthenticationFilter.class);
    
    @Autowired
    private HttpAuthorizationHandler authorizationHandler;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    @Override
    public void filter (ContainerRequestContext request) {
        String host = request.getHeaderString(ContainerRequest.HOST);
        String ua = request.getHeaderString(ContainerRequest.USER_AGENT);

        String authorization =
                request.getHeaderString(ContainerRequest.AUTHORIZATION);

        if (authorization != null && !authorization.isEmpty()) {
            TokenContext context = null;
            AuthorizationData authData;
            try {
                authData = authorizationHandler
                        .parseAuthorizationHeaderValue(authorization);

                switch (authData.getAuthenticationScheme()) {
                    // EM: For testing only, must be disabled for
                    // production
                    case BASIC:
                        context = authenticationManager.getTokenContext(
                                TokenType.BASIC, authData.getToken(), host, ua);
                        break;
                    // EM: has not been tested yet
                    // case SESSION:
                    // context =
                    // authenticationManager.getTokenContext(
                    // TokenType.SESSION, authData.getToken(), host,
                    // ua);
                    // break;

                    // OAuth2 authentication scheme
                    case BEARER:
                        context = authenticationManager.getTokenContext(
                                TokenType.BEARER, authData.getToken(), host,
                                ua);
                        break;
                    // EM: JWT token-based authentication scheme
                    case API:
                        jlog.warn("Authentication filter using token API");
                        throw new KustvaktException(
                                StatusCodes.AUTHENTICATION_FAILED,
                                "Authentication API is no longer supported.");
                    default:
                        throw new KustvaktException(
                                StatusCodes.AUTHENTICATION_FAILED,
                                "Authentication scheme is not supported.");
                }
                checkContext(context, request);
                request.setSecurityContext(new KustvaktContext(context));
            }
            catch (KustvaktException e) {
                throw kustvaktResponseHandler.throwit(e);
            }
        }
    }


    private void checkContext (TokenContext context, ContainerRequestContext request)
            throws KustvaktException {
        if (context == null) {
            throw new KustvaktException(StatusCodes.AUTHENTICATION_FAILED,
                    "Context is null.");
        }
        else if (!context.isValid()) {
            throw new KustvaktException(StatusCodes.AUTHENTICATION_FAILED,
                    "Context is not valid: "
                            + "missing username, password or authentication scheme.");
        }
        else if (context.isSecureRequired() && !request.getSecurityContext().isSecure()) {
            throw new KustvaktException(StatusCodes.AUTHENTICATION_FAILED,
                    "Request is not secure.");
        }
        else if (TimeUtils.isExpired(context.getExpirationTime())) {
            throw new KustvaktException(StatusCodes.EXPIRED,
                    "Access token is expired");
        }
    }
}
