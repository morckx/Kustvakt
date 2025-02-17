package de.ids_mannheim.korap.exceptions;

import java.util.Properties;

import de.ids_mannheim.korap.config.ConfigLoader;

/**
 * @author hanl, margaretha
 * @date 07/09/2014
 */
public class StatusCodes {

    /**
     * 100 status codes for standard system errors
     */
    public static final int GENERAL_ERROR = 100;
    public static final int NO_RESULT_FOUND = 101;
    public static final int UNSUPPORTED_AUTHENTICATION_SCHEME = 102;
    public static final int UNSUPPORTED_OPERATION = 103;
    public static final int ILLEGAL_ARGUMENT = 104;
    public static final int MISSING_PARAMETER = 105;
    public static final int CONNECTION_ERROR = 106;
    public static final int INVALID_ARGUMENT = 107;
    public static final int NOT_SUPPORTED = 108;
    public static final int NOT_ALLOWED = 109;
    public static final int HTTPS_REQUIRED = 110;
    public static final int INVALID_ALGORITHM = 111;
    public static final int UNSUPPORTED_API_VERSION = 112;
    public static final int NON_UNIQUE_RESULT_FOUND = 113;
    public static final int NO_RESOURCE_FOUND = 114;
    public static final int DEPRECATED = 115;
    public static final int CACHING_VC = 116;
    public static final int NETWORK_ENDPOINT_NOT_AVAILABLE = 117;
    public static final int SEARCH_NETWORK_ENDPOINT_FAILED = 118;
    
    /**
     * 200 status codes general JSON serialization error
     */

    public static final int SERIALIZATION_FAILED = 200;
    public static final int DESERIALIZATION_FAILED = 201;
    public static final int MISSING_ATTRIBUTE = 202;
    public static final int INVALID_ATTRIBUTE = 203;
    public static final int UNSUPPORTED_VALUE = 204;

    /**
     * 300 status codes for query language and serialization
     * see Koral (de.ids_mannheim.korap.query.serialize.util.StatusCodes)
     */

    /**
     *  400 status codes for rewrite functions
     */

    public static final int REWRITE_ERROR_DEFAULT = 400;
    public static final int NON_PUBLIC_FIELD_IGNORED = 401;
    public static final int PIPE_FAILED = 402;
    
//    public static final int UNSUPPORTED_RESOURCE = 402;
    //    public static final int REWRITE_FAILED = 403;
    //public static final int UNSUPPORTED_FOUNDRY = 403;
    //public static final int UNSUPPORTED_CORPUS = 404;
    //public static final int UNSUPPORTED_LAYER = 405;
    // make a distinction between no and invalid vc?
    //public static final int UNSUPPORTED_COLLECTION = 406;
    //public static final int CORPUS_REWRITE = 407;
    //public static final int FOUNDRY_REWRITE = 408;
    //public static final int FOUNDRY_INJECTION = 409;
    //    public static final int MISSING_RESOURCE = 405;
//    public static final int NO_POLICY_TARGET = 406;
//    public static final int NO_POLICY_CONDITION = 407;
//    public static final int NO_POLICY_PERMISSION = 408;
//    public static final int NO_POLICIES = 409;



    /**
     * 500 status codes for access control related components (also
     * policy rewrite)
     */
    // todo: extend according to policy rewrite possible!
    // policy errors


    // database codes
    public static final int DB_GET_FAILED = 500;
    public static final int DB_INSERT_FAILED = 501;
    public static final int DB_DELETE_FAILED = 502;
    public static final int DB_UPDATE_FAILED = 503;

    public static final int DB_GET_SUCCESSFUL = 504;
    public static final int DB_INSERT_SUCCESSFUL = 505;
    public static final int DB_DELETE_SUCCESSFUL = 506;
    public static final int DB_UPDATE_SUCCESSFUL = 507;
    public static final int DB_ENTRY_EXISTS = 508;


    //    public static final int ARGUMENT_VALIDATION_FAILURE = 700;
    // public static final int ARGUMENT_VALIDATION_FAILURE = 701;

    // service status codes
    public static final int CREATE_ACCOUNT_SUCCESSFUL = 700;
    public static final int CREATE_ACCOUNT_FAILED = 701;
    public static final int DELETE_ACCOUNT_SUCCESSFUL = 702;
    public static final int DELETE_ACCOUNT_FAILED = 703;
    public static final int UPDATE_ACCOUNT_SUCCESSFUL = 704;
    public static final int UPDATE_ACCOUNT_FAILED = 705;

    public static final int GET_ACCOUNT_SUCCESSFUL = 706;
    public static final int GET_ACCOUNT_FAILED = 707;


    public static final int STATUS_OK = 1000;
    public static final int NOTHING_CHANGED = 1001;
    public static final int REQUEST_INVALID = 1002;
    
//    public static final int ACCESS_DENIED = 1003;

    
    // User group and member 
    public static final int GROUP_MEMBER_EXISTS = 1601;
    public static final int GROUP_MEMBER_INACTIVE = 1602;
    public static final int GROUP_MEMBER_DELETED = 1603;
    public static final int GROUP_MEMBER_NOT_FOUND = 1604;
    public static final int INVITATION_EXPIRED = 1605;
    public static final int GROUP_DELETED = 1606;
    
    /**
     * 1800 Oauth2 and OpenID
     */

    public static final int OAUTH2_SYSTEM_ERROR = 1800;
    
    public static final int CLIENT_REGISTRATION_FAILED = 1801;
    public static final int CLIENT_DEREGISTRATION_FAILED = 1802;
    public static final int CLIENT_AUTHENTICATION_FAILED = 1803;
    public static final int CLIENT_AUTHORIZATION_FAILED = 1804;
    public static final int CLIENT_NOT_FOUND = 1805;
    public static final int INVALID_REDIRECT_URI = 1806;
    public static final int MISSING_REDIRECT_URI = 1807;
    public static final int INVALID_SCOPE = 1808;
    public static final int INVALID_AUTHORIZATION = 1809;
    public static final int INVALID_REFRESH_TOKEN = 1810;
    
    public static final int UNSUPPORTED_GRANT_TYPE = 1811;
    public static final int UNSUPPORTED_AUTHENTICATION_METHOD = 1812;
    
    public static final int ID_TOKEN_CLAIM_ERROR = 1813;
    public static final int ID_TOKEN_SIGNING_FAILED = 1814;
    public static final int USER_REAUTHENTICATION_REQUIRED = 1815;
    
    public static final int INVALID_REFRESH_TOKEN_EXPIRY = 1816;
    
    /**
     * 1850 Plugins
     */

    public static final int PLUGIN_NOT_PERMITTED = 1850;
    public static final int PLUGIN_HAS_BEEN_INSTALLED = 1851;
    
    
    /**
     * 1900 User account and logins
     */

    public static final int LOGIN_SUCCESSFUL = 1900;
    public static final int ALREADY_LOGGED_IN = 1901;

    public static final int LOGOUT_SUCCESSFUL = 1902;
    public static final int LOGOUT_FAILED = 1903;

    public static final int ACCOUNT_CONFIRMATION_FAILED = 1904;
    public static final int PASSWORD_RESET_FAILED = 1905;

    /**
     * 2000 status and error codes concerning authentication
     * 
     * Response with WWW-Authenticate header will be created 
     * for all KustvaktExceptions with status codes 2001 or greater  
     *  
     * MH: service level messages and callbacks
     */

    @Deprecated
    public static final int INCORRECT_ADMIN_TOKEN = 2000;
    
    public static final int AUTHENTICATION_FAILED = 2001;
    public static final int LOGIN_FAILED = 2002;
    public static final int EXPIRED = 2003;
    public static final int BAD_CREDENTIALS = 2004;
    public static final int ACCOUNT_NOT_CONFIRMED = 2005;
    public static final int ACCOUNT_DEACTIVATED = 2006;

    //    public static final int CLIENT_AUTHORIZATION_FAILED = 2013;
    public static final int AUTHORIZATION_FAILED = 2010;
    public static final int INVALID_ACCESS_TOKEN = 2011;

    // 2020 - 2029 reserviert für LDAP-Fehlercodes - 21.04.17/FB
    public static final int LDAP_BASE_ERRCODE = 2020;

    /**/
    private static StatusCodes codes;

    private final Properties props;

    private StatusCodes () {
        this.props = ConfigLoader.loadProperties("codes.info");
    }


    public static final String getMessage (int code) {
        return getCodes().props.getProperty(String.valueOf(code));
    }

    public static StatusCodes getCodes () {
        if (codes == null) codes = new StatusCodes();
        return codes;
    }

}
