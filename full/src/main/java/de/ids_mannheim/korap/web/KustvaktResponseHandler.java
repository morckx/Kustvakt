package de.ids_mannheim.korap.web;

import java.util.EnumSet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import de.ids_mannheim.korap.constant.AuthenticationScheme;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;

/**
 * KustvaktResponseHandler includes exceptions regarding
 * authorization.
 * 
 * @author margaretha
 *
 */
public class KustvaktResponseHandler extends CoreResponseHandler {

    @Override
    public WebApplicationException throwit (KustvaktException e) {
        Response r;

        // KustvaktException just wraps another exception
        if (e.getStatusCode() == null && e.hasNotification()) {
            r = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getNotification()).build();
        }
        else if (e.getStatusCode() == StatusCodes.USER_REAUTHENTICATION_REQUIRED
                || e.getStatusCode() == StatusCodes.AUTHORIZATION_FAILED
                || e.getStatusCode() >= StatusCodes.AUTHENTICATION_FAILED) {
            String notification = buildNotification(e.getStatusCode(),
                    e.getMessage(), e.getEntity());
            r = createUnauthenticatedResponse(notification);
        }
        else if (e.hasNotification()) {
            r = Response.status(getStatus(e.getStatusCode()))
                    .entity(e.getNotification()).build();
        }
        else {
            String notification = buildNotification(e.getStatusCode(),
                    e.getMessage(), e.getEntity());
            r = Response.status(getStatus(e.getStatusCode()))
                    .entity(notification).build();
        }
        return new WebApplicationException(r);
    }

    public Response createUnauthenticatedResponse (String notification) {
        ResponseBuilder builder = Response.status(Response.Status.UNAUTHORIZED);

        EnumSet<AuthenticationScheme> schemes = EnumSet
                .allOf(AuthenticationScheme.class);
        schemes.remove(AuthenticationScheme.API);
        
        for (AuthenticationScheme s : schemes) {
            builder = builder.header(HttpHeaders.WWW_AUTHENTICATE,
                    s.displayName() + " realm=\"Kustvakt\"");
        }

        return builder.entity(notification).build();
    }
}
