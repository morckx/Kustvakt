package de.ids_mannheim.korap.web;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.authentication.http.HttpUnauthorizedHandler;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.interfaces.db.AuditingIface;

/** KustvaktResponseHandler includes exceptions regarding authorization. 
 * 
 * @author margaretha
 *
 */
public class FullResponseHandler extends CoreResponseHandler {

    @Autowired
    private HttpUnauthorizedHandler handler;

    public FullResponseHandler (AuditingIface iface) {
        super(iface);
    }

    @Override
    public WebApplicationException throwit (KustvaktException e) {
        Response r;

        if (e.getStatusCode() == StatusCodes.AUTHORIZATION_FAILED
                || e.getStatusCode() >= StatusCodes.AUTHENTICATION_FAILED) {
            String notification = buildNotification(e.getStatusCode(),
                    e.getMessage(), e.getEntity());
            r = handler.createUnauthenticatedResponse(notification);
        }
        else if (e.hasNotification()) {
            r = Response.status(getStatus(e.getStatusCode()))
                    .entity(e.getNotification()).build();
        }
        else {
            r = Response.status(getStatus(e.getStatusCode()))
                    .entity(buildNotification(e)).build();
        }
        return new WebApplicationException(r);
    }

    public WebApplicationException throwit (OAuthProblemException e) {
        OAuthResponse or = null;
        try {
            or = OAuthResponse.errorResponse(e.getResponseStatus()).error(e)
                    .buildJSONMessage();
        }
        catch (OAuthSystemException e1) {
            //            return throwit(new KustvaktException(
            //                    StatusCodes.OAUTH2_SYSTEM_ERROR, e1.getMessage(), e1));
            return throwit(StatusCodes.OAUTH2_SYSTEM_ERROR, e1.getMessage());
        }

        Response r = Response.status(or.getResponseStatus())
                .entity(or.getBody()).build();
        return new WebApplicationException(r);

    }

    //    public WebApplicationException throwAuthenticationException (
    //            String message) {
    //        String notification =
    //                buildNotification(StatusCodes.AUTHORIZATION_FAILED,
    //                        "Authorization failed", message);
    //        return new WebApplicationException(
    //                handler.createUnauthenticatedResponse(notification));
    //    }

    //    public WebApplicationException throwAuthenticationException (
    //            KustvaktException e) {
    //        String notification = buildNotification(e.getStatusCode(),
    //                e.getMessage(), e.getEntity());
    //        return new WebApplicationException(
    //                handler.createUnauthenticatedResponse(notification));
    //    }
}
