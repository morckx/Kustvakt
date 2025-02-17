package de.ids_mannheim.korap.oauth2.oltu;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.oltu.oauth2.as.request.OAuthRequest;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.common.validators.OAuthValidator;

/**
 * A custom request based on {@link OAuthRequest}. It defines token
 * revocation request that should have been sent from a super client.
 * 
 * @author margaretha
 *
 */
public class OAuth2RevokeTokenSuperRequest{
    protected HttpServletRequest request;
    protected OAuthValidator<HttpServletRequest> validator;
    protected Map<String, Class<? extends OAuthValidator<HttpServletRequest>>> validators =
            new HashMap<String, Class<? extends OAuthValidator<HttpServletRequest>>>();
    
    public OAuth2RevokeTokenSuperRequest () {
        // TODO Auto-generated constructor stub
    }

    public OAuth2RevokeTokenSuperRequest (HttpServletRequest request)
            throws OAuthSystemException, OAuthProblemException {
        this.request = request;
        validate();
    }

    protected void validate ()
            throws OAuthSystemException, OAuthProblemException {
        validator = initValidator();
        validator.validateMethod(request);
        validator.validateContentType(request);
        validator.validateRequiredParameters(request);
        // for super client authentication
        validator.validateClientAuthenticationCredentials(request);
    }
    protected OAuthValidator<HttpServletRequest> initValidator ()
            throws OAuthProblemException, OAuthSystemException {
        return OAuthUtils.instantiateClass(RevokeTokenSuperValidator.class);
    }

    public String getParam (String name) {
        return request.getParameter(name);
    }
    
    public String getToken () {
        return getParam("token");
    }
    
    public String getSuperClientId () {
        return request.getParameter(RevokeTokenSuperValidator.SUPER_CLIENT_ID);
    }

    public String getSuperClientSecret () {
        return request
                .getParameter(RevokeTokenSuperValidator.SUPER_CLIENT_SECRET);
    }
}
