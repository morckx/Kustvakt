/*
 *   user authentication via LDAP
 */

package de.ids_mannheim.korap.authentication;

import com.nimbusds.jose.JOSEException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.server.EmbeddedLdapServer;
import org.apache.commons.text.StringSubstitutor;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.*;

import static de.ids_mannheim.korap.server.EmbeddedLdapServer.loadProp;


/**
 * LDAP Login
 *
 * @author bodmer, margaretha, kupietz
 * @see APIAuthentication
 */
public class LdapAuth3 extends APIAuthentication {

    public static final int LDAP_AUTH_ROK = 0;
    public static final int LDAP_AUTH_RCONNECT = 1; // cannot connect to LDAP Server
    public static final int LDAP_AUTH_RINTERR = 2; // internal error: cannot verify User+Pwd.
    /* cannot be distinguished, currently
    public static final int LDAP_AUTH_RUNKNOWN = 3; // User Account or Pwd unknown;
    public static final int LDAP_AUTH_RLOCKED = 4; // User Account locked;
    public static final int LDAP_AUTH_RNOTREG = 5; // User known, but has not registered to KorAP/C2 Service yet;
     */
    public static final int LDAP_AUTH_RNOEMAIL = 6; // cannot obtain email for sUserDN
    public static final int LDAP_AUTH_RNAUTH = 7; // User Account or Pwd unknown, or not authorized
    final static Boolean DEBUGLOG = false;        // log debug output.

    public LdapAuth3(FullConfiguration config) throws JOSEException {
        super(config);
    }

    public static String getErrMessage(int code) {
        switch (code) {
            case LDAP_AUTH_ROK:
                return "LDAP Authentication successful.";
            case LDAP_AUTH_RCONNECT:
                return "LDAP Authentication: connecting to LDAP Server failed!";
            case LDAP_AUTH_RINTERR:
                return "LDAP Authentication failed due to an internal error!";
/* cannot be distinguished, currently
            case LDAP_AUTH_RUNKNOWN:
                return "LDAP Authentication failed due to unknown user or password!";
            case LDAP_AUTH_RLOCKED:
                return "LDAP Authentication: known user is locked!";
            case LDAP_AUTH_RNOTREG:
                return "LDAP Authentication: known user has not registered yet!";
*/
            case LDAP_AUTH_RNOEMAIL:
                return "LDAP Authentication: known user, but cannot obtain email!";
            case LDAP_AUTH_RNAUTH:
                return "LDAP Authentication: unknown user or password, or user is locked or not authorized!";
            default:
                return "LDAP Authentication failed with unknown error code!";
        }
    }

    public static int login(String sUserDN, String sUserPwd, String ldapConfigFilename) throws LDAPException {

        sUserDN = Filter.encodeValue(sUserDN);
        sUserPwd = Filter.encodeValue(sUserPwd);

        Map<String, String> ldapConfig;
        try {
            ldapConfig = loadProp(ldapConfigFilename);
        } catch (IOException e) {
            System.out.println("Error: LDAPAuth.login: cannot load Property file!");
            return LDAP_AUTH_RINTERR;
        }

        assert ldapConfig != null;
        final boolean ldapS = Boolean.parseBoolean(ldapConfig.getOrDefault("ldapS", "false"));
        final String ldapHost = ldapConfig.getOrDefault("ldapHost", "localhost");
        final int ldapPort = Integer.parseInt(ldapConfig.getOrDefault("ldapPort", (ldapS ? "636" : "389")));
        final String ldapBase = ldapConfig.getOrDefault("ldapBase", "dc=example,dc=com");
        final String sLoginDN = ldapConfig.getOrDefault("sLoginDN", "cn=admin,dc=example,dc=com");
        final String ldapFilter = ldapConfig.getOrDefault("ldapFilter", "(&(|(&(mail=${username})(idsC2Password=${password}))(&(idsC2Profile=${username})(idsC2Password=${password})))(&(idsC2=TRUE)(|(idsStatus=1)(|(idsStatus=0)(xidsStatus=\00)))))");
        final String sPwd = ldapConfig.getOrDefault("pwd", "");
        final String trustStorePath = ldapConfig.getOrDefault("trustStore", "");
        final String additionalCipherSuites = ldapConfig.getOrDefault("additionalCipherSuites", "");
        final boolean useEmbeddedServer = Boolean.parseBoolean(ldapConfig.getOrDefault("useEmbeddedServer", "false"));

        if (useEmbeddedServer && EmbeddedLdapServer.server == null) {
            try {
                EmbeddedLdapServer.start(ldapConfigFilename);
            } catch (GeneralSecurityException | UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }

        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("username", sUserDN);
        valuesMap.put("password", sUserPwd);
        StringSubstitutor sub = new StringSubstitutor(valuesMap);
        String ldapFilterInstance = sub.replace(ldapFilter);

        if (DEBUGLOG) {
            //System.out.printf("LDAP Version      = %d.\n", LDAPConnection.LDAP_V3);
            System.out.printf("LDAP Host & Port  = '%s':%d.\n", ldapHost, ldapPort);
            System.out.printf("Login User = '%s'\n", sUserDN);
        }

        // LDAP Connection:
        if (DEBUGLOG) System.out.println("LDAPS " + ldapS);

        LDAPConnection lc;

        if (ldapS) {
            try {
                SSLUtil sslUtil;
                if (trustStorePath != null && !trustStorePath.isEmpty()) {
                    sslUtil = new SSLUtil(new TrustStoreTrustManager(trustStorePath));
                } else {
                    sslUtil = new SSLUtil(new TrustAllTrustManager());
                }
                if (additionalCipherSuites != null && !additionalCipherSuites.isEmpty()) {
                    addSSLCipherSuites(additionalCipherSuites);
                }
                SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();
                lc = new LDAPConnection(socketFactory);
            } catch (GeneralSecurityException e) {
                System.err.printf("Error: login: Connecting to LDAPS Server: failed: '%s'!\n", e);
                return ldapTerminate(null, LDAP_AUTH_RCONNECT);
            }
        } else {
            lc = new LDAPConnection();
        }
        try {
            lc.connect(ldapHost, ldapPort);
            if (DEBUGLOG && ldapS) System.out.println("LDAPS Connection = OK\n");
            if (DEBUGLOG && !ldapS) System.out.println("LDAP Connection = OK\n");
        } catch (LDAPException e) {
            String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
            System.err.printf("Error: login: Connecting to LDAP Server: failed: '%s'!\n", fullStackTrace);
            return ldapTerminate(lc, LDAP_AUTH_RCONNECT);
        }


        if (DEBUGLOG) System.out.printf("Debug: isConnected=%d\n", lc.isConnected() ? 1 : 0);

        try {
            // bind to server:
            if (DEBUGLOG) System.out.printf("Binding with '%s' ...\n", sLoginDN);
            lc.bind(sLoginDN, sPwd);
            if (DEBUGLOG) System.out.print("Binding: OK.\n");
        } catch (LDAPException e) {
            System.err.printf("Error: login: Binding failed: '%s'!\n", e);
            return ldapTerminate(lc, LDAP_AUTH_RINTERR);
        }

        if (DEBUGLOG) System.out.printf("Debug: isConnected=%d\n", lc.isConnected() ? 1 : 0);

        if (DEBUGLOG) System.out.printf("Finding user '%s'...\n", sUserDN);

        SearchResult srchRes;
        try {
            // SCOPE_SUB = Scope Subtree.
            if (DEBUGLOG) System.out.printf("Finding Filter: '%s'.\n", ldapFilterInstance);

            srchRes = lc.search(ldapBase, SearchScope.SUB, ldapFilterInstance);

            if (DEBUGLOG) System.out.printf("Finding '%s': %d entries.\n", sUserDN, srchRes.getEntryCount());
        } catch (LDAPSearchException e) {
            System.err.printf("Error: login: Search for User failed: '%s'!\n", e);
            return ldapTerminate(lc, LDAP_AUTH_RNAUTH);
        }

        if (srchRes.getEntryCount() == 0) {
            if (DEBUGLOG) System.out.printf("Finding '%s': no entry found!\n", sUserDN);
            return ldapTerminate(lc, LDAP_AUTH_RNAUTH);
        }

        return ldapTerminate(lc, LDAP_AUTH_ROK); // OK.
    }

    public static int ldapTerminate(LDAPConnection lc, int ret) {
        if (DEBUGLOG) System.out.println("Terminating...");

        if (lc != null) {
            lc.close(null);
        }
        if (DEBUGLOG) System.out.println("closing connection: done.\n");
        return ret;
    }

    private static void addSSLCipherSuites(String ciphersCsv) {
        // add e.g. TLS_RSA_WITH_AES_256_GCM_SHA384
        Set<String> ciphers = new HashSet<>();
        ciphers.addAll(SSLUtil.getEnabledSSLCipherSuites());
        ciphers.addAll(Arrays.asList(ciphersCsv.split(", *")));
        SSLUtil.setEnabledSSLCipherSuites(ciphers);
    }

    @Override
    public TokenType getTokenType() {
        return TokenType.API;
    }

}
