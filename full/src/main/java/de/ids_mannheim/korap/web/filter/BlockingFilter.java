package de.ids_mannheim.korap.web.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;

/**
 * @author hanl
 * @date 11/12/2014
 *       <p/>
 *       endpoint filter to block access to an endpoint, in case no
 *       anonymous access should be allowed!
 */
@Component
@Priority(Priorities.AUTHORIZATION)
public class BlockingFilter implements ContainerRequestFilter {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;

    @Override
    public void filter (ContainerRequestContext request) {
        TokenContext context;

        SecurityContext securityContext = request.getSecurityContext();
        if (securityContext != null) {
            context = (TokenContext) securityContext.getUserPrincipal();
        }
        else {
            throw kustvaktResponseHandler.throwit(new KustvaktException(
                    StatusCodes.UNSUPPORTED_OPERATION));
        }

        if (context == null || context.isDemo()) {
            throw kustvaktResponseHandler.throwit(new KustvaktException(
                    StatusCodes.AUTHORIZATION_FAILED,
                    "Unauthorized operation for user: guest", "guest"));
        }
    }
}
