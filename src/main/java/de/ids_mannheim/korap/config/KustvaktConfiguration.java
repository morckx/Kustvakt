package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.interfaces.EncryptionIface;
import de.ids_mannheim.korap.utils.KustvaktLogger;
import de.ids_mannheim.korap.utils.TimeUtils;
import lombok.Getter;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * if configuration class is extended, loadSubTypes method should be overriden
 *
 * @author hanl
 * @date 05/02/2014
 */

@Getter
public class KustvaktConfiguration {

    private static final Logger jlog = KustvaktLogger
            .getLogger(KustvaktConfiguration.class);
    private String indexDir;
    private int port;
    // todo: make exclusive so that the containg languages can really only be used then
    private List<String> queryLanguages;

    private String serverHost;
    private URL issuer;

    private int maxhits;
    private int returnhits;
    private String keystoreLocation;
    private String keystorePassword;
    private Properties mailProperties;
    private String host;
    private String shibUserMapping;
    private String userConfig;
    private int inactiveTime;
    private int loginAttemptTTL;
    private long loginAttemptNum;
    private boolean allowMultiLogIn;
    private int expiration;
    private int loadFactor;
    private int validationStringLength;
    private int validationEmaillength;
    // fixme: should move to base config?!
    private EncryptionIface.Encryption encryption;
    private byte[] sharedSecret;
    private String adminToken;
    private int longTokenTTL;
    private int tokenTTL;
    private int shortTokenTTL;
    private String[] rewrite_strategies;
    private String passcodeSaltField;

    private String default_pos;
    private String default_lemma;
    private String default_token;
    private String default_dep;
    private String default_const;

    // deprec?!
    private final BACKENDS DEFAULT_ENGINE = BACKENDS.LUCENE;

    /**
     * loading of the properties and mapping to parameter variables
     *
     * @param properties
     * @return
     */
    protected Properties load(Properties properties) {
        properties.list(System.out);

        // if not present, uses classpath log4j.properties
        String log4jconfig = properties.getProperty("log4jconfig", "");
        loadLog4jLogger();
        maxhits = new Integer(properties.getProperty("maxhits", "50000"));
        returnhits = new Integer(properties.getProperty("returnhits", "50000"));
        indexDir = properties.getProperty("lucene.indexDir", "");
        port = new Integer(properties.getProperty("server.port", "8080"));
        // server options
        serverHost = String
                .valueOf(properties.getProperty("server.host", "localhost"));
        String queries = properties.getProperty("korap.ql", "");
        String[] qls = queries.split(",");
        queryLanguages = new ArrayList<>();
        for (String querylang : qls)
            queryLanguages.add(querylang.trim().toUpperCase());
        //        issuer = new URL(korap.getProperty("korap.issuer", ""));

        default_const = properties
                .getProperty("kustvakt.default.const", "mate");
        default_dep = properties.getProperty("kustvakt.default.dep", "mate");
        default_lemma = properties.getProperty("kustvakt.default.lemma", "tt");
        default_pos = properties.getProperty("kustvakt.default.pos", "tt");
        default_token = properties
                .getProperty("kustvakt.default.token", "opennlp");

        // security configuration
        expiration = TimeUtils.convertTimeToSeconds(properties
                .getProperty("security.absoluteTimeoutDuration", "25M"));
        inactiveTime = TimeUtils.convertTimeToSeconds(
                properties.getProperty("security.idleTimeoutDuration", "10M"));
        allowMultiLogIn = Boolean
                .valueOf(properties.getProperty("security.multipleLogIn"));

        loginAttemptNum = Long.parseLong(
                properties.getProperty("security.loginAttemptNum", "3"));
        loginAttemptTTL = TimeUtils.convertTimeToSeconds(
                properties.getProperty("security.authAttemptTTL", "30M"));

        loadFactor = Integer.valueOf(
                properties.getProperty("security.encryption.loadFactor", "15"));
        validationStringLength = Integer.valueOf(properties
                .getProperty("security.validation.stringLength", "150"));
        validationEmaillength = Integer.valueOf(properties
                .getProperty("security.validation.emailLength", "40"));
        encryption = Enum.valueOf(EncryptionIface.Encryption.class,
                properties.getProperty("security.encryption", "BCRYPT"));
        sharedSecret = properties.getProperty("security.sharedSecret", "")
                .getBytes();
        adminToken = properties.getProperty("security.adminToken");

        longTokenTTL = TimeUtils.convertTimeToSeconds(
                properties.getProperty("security.longTokenTTL", "100D"));
        tokenTTL = TimeUtils.convertTimeToSeconds(
                properties.getProperty("security.tokenTTL", "72H"));
        shortTokenTTL = TimeUtils.convertTimeToSeconds(
                properties.getProperty("security.shortTokenTTL", "3H"));

        passcodeSaltField = properties
                .getProperty("security.passcode.salt", "accountCreation");

        return properties;
    }

    /**
     * set properties
     *
     * @param props
     */
    public void setProperties(Properties props) {
        this.load(props);
    }

    /**
     * properties can be overloaded after spring init
     *
     * @param stream
     */
    public void setPropertiesAsStream(InputStream stream) {
        try {

            Properties p = new Properties();
            p.load(stream);
            this.load(p);
        }catch (IOException e) {
            e.printStackTrace();
        }

    }

    public BACKENDS chooseBackend(String value) {
        if (value == null || value.equals("null"))
            return DEFAULT_ENGINE;
        else
            return Enum.valueOf(BACKENDS.class, value.toUpperCase());
    }

    private void loadLog4jLogger() {
        /** loadSubTypes log4j configuration file programmatically */
        Properties log4j = new Properties();
        try {
            File f = new File("./config/log4j.properties");
            if (f.exists()) {
                log4j.load(new FileInputStream(f));
                PropertyConfigurator.configure(log4j);
                jlog.warn(
                        "using local logging properties file ({}) to configure logging system",
                        "./config/log4j.properties");
                return;
            }
        }catch (Exception e) {
            // do nothing
        }
        loadClassLogger();
    }

    private void loadClassLogger() {
        Properties log4j = new Properties();
        jlog.info(
                "using class path logging properties file to configure logging system");

        try {
            log4j.load(KustvaktConfiguration.class.getClassLoader()
                    .getResourceAsStream("log4j.properties"));
        }catch (IOException e) {
            // do nothing
        }

        PropertyConfigurator.configure(log4j);
        jlog.warn(
                "No logger properties detected. Using default logger properties");
    }

    public enum BACKENDS {
        NEO4J, LUCENE
    }

}
