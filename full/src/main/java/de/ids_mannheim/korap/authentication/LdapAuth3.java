/* - Klasse zum Implementieren einer Benutzer-Authentifikation mittels LDAP
 *   in der IDM-Datenbank (Identit�tsmanagement) von Eric Seubert, IDS.
 * - externe Bibliothek ist Novel JLDAP.
 * 27.01.17/FB
 *
 * Sourcen:
 * - https://www.novell.com/documentation/developer/samplecode/jldap_sample/VerifyPassword.java.html
 * - https://www.novell.com/documentation/developer/samplecode/jldap_sample/LDAPOIDs.java.html
 * - https://www.novell.com/documentation/developer/jldap/jldapenu/data/a90352e.html
 * WICHTIG:
 * - Novell-Bibliothek liefert 0 Treffer, wenn man nacheinander sucht!
 *   Grund daf�r nicht gefunden.
 *
 * Version von unboundID - 19.04.17/FB
 *
 * UnboundID LDAP SDK For Java � 3.2.1
 * The UnboundID LDAP SDK for Java is a fast, comprehensive, and easy-to-use Java API for 
 * communicating with LDAP directory servers and performing related tasks like reading and writing LDIF, 
 * encoding and decoding data using base64 and ASN.1 BER, and performing secure communication. This package 
 * contains the Standard Edition of the LDAP SDK, which is a complete, general-purpose library for 
 * communicating with LDAPv3 directory servers. 
 * TODO:
 * - gesichertes Login mit gesch�tztem Passwort.
 * - Passwort des Admin verschl�sseln.
 */
 
package de.ids_mannheim.korap.authentication;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import com.nimbusds.jose.JOSEException;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.constant.TokenType;


/**
 * LDAP Login Tests
 * 
 * @author bodmer, margaretha
 * @see APIAuthentication
 */
public class LdapAuth3 extends APIAuthentication {

    /* For SSL Connection to LDAP, see: https://www.novell.com/documentation/developer/jldap/jldapenu/data/cchcbejj.html.
	 * and use DEFAULT_SSL_PORT.
     * For now, plain text connection is used.
	 * FB
	 */
	final static Boolean DEBUGLOG 	= false;		// log debug output.
	final static String attC2 		= "idsC2";		// if value == TRUE: registered for COSMAS II (KorAP) Service.
	final static String attStatus	= "idsStatus";	// value must be 0..2, 3 = locked account.
	final static String attEmail	= "mail";		// attribute whose value is the requested email.
	final static int ldapPort 		= 389; 			//LDAPConnection.DEFAULT_PORT;
	// final static int ldapVersion	= LDAPConnection.LDAP_V3;
	final static String ldapHost 	= "ldap.ids-mannheim.de";
	final static String ldapBase	= "dc=ids-mannheim,dc=de";
	final static String sLoginDN 	= "cn=casaling,dc=ids-mannheim,dc=de";
	static String sPwd 				= null;

	/**
	 * return codes for functions of this class:
	 */

	public static final int LDAP_AUTH_ROK		= 0; 
	public static final int LDAP_AUTH_RCONNECT	= 1; // cannot connect to LDAP Server
	public static final int LDAP_AUTH_RINTERR	= 2; // internal error: cannot verify User+Pwd.
	public static final int LDAP_AUTH_RUNKNOWN	= 3; // User Account or Pwd unknown;
	public static final int LDAP_AUTH_RLOCKED	= 4; // User Account locked;
	public static final int LDAP_AUTH_RNOTREG	= 5; // User known, but has not registered to KorAP/C2 Service yet;
	public static final int LDAP_AUTH_RNOEMAIL	= 6; // cannot obtain email for sUserDN.

    public LdapAuth3 (FullConfiguration config) throws JOSEException {
        super(config);
    }	
    
	
	@Override
	public TokenType getTokenType () {
	    return TokenType.API;
	}
	 
	/**
	 * getErrMessage:
	 * returns String Message for LDAP_AUTH_Rxxx code.
	 * @date 20.04.17/FB
	 * @param code
	 * @return Message in string form.
	 */
	public static String getErrMessage(int code)
	
	{
	switch(code)
		{
	case LDAP_AUTH_ROK: 
		return "LDAP Authentication successfull.";
	case LDAP_AUTH_RCONNECT:
		return "LDAP Authentication: connecting to LDAP Server failed!";
	case LDAP_AUTH_RINTERR: 
		return "LDAP Authentication failed due to an internal error!";
	case LDAP_AUTH_RUNKNOWN:
		return "LDAP Authentication failed due to unknown user or password!";
	case LDAP_AUTH_RLOCKED:
		return "LDAP Authentication: known user is locked!";
	case LDAP_AUTH_RNOTREG:
		return "LDAP Authentication: known user has not registered yet for COSMAS II/KorAP!";
	case LDAP_AUTH_RNOEMAIL:
		return "LDAP Authentication: known user, but cannot obtain email!";
	default:
		return "LDAP Authentication failed with unknown error code!";
		}
	} // getErrMessage
	
	/**
	 * ldapCode2StatusCode:
	 * - converts a LDAP_AUTH_xxx Error Code to an Error Code of StatusCode.java.
	 * @param base : Base value inside of StatusCode.java reserved for LDAP_AUTH Error Codes.
	 * @param ldapErrCode : the LDAP_AUTH Error code
	 * @return the StatusCode in the range reserved for LDAP_AUTH Errors.
	 * @date 21.04.17/FB
	 */
	public int ldapCode2StatusCode(int base, int ldapErrCode)
	
	{
	return base + ldapErrCode;	
	} // ldapCode2StatusCode
	
	/*
	 *  load properties for LDAP Handling.
	 *  17.02.17/FB
	 */
	
	static String loadProp(String sConfFile) throws IOException
	
	{
		String sPwd = null;
		FileInputStream in;
		Properties prop;
		
        try {
            in = new FileInputStream(sConfFile);
            }
        catch( IOException ex )
            {
        	System.err.printf("Error: LDAP.loadProp: cannot load Property file '%s'!\n", sConfFile); 
            ex.printStackTrace();
            return null;
            }

        if( DEBUGLOG ) System.out.println("Debug: loaded: " + sConfFile);
	    
        prop = new Properties();
        Enumeration<?> e;
        
        try {
            prop.load(in);
            e = prop.propertyNames();

            while( e.hasMoreElements() )
                {
                String key = (String)e.nextElement();
                String val = prop.getProperty(key);
                if( key.compareTo("pwd") == 0 )
                	return val; 
                
                //System.out.println("Property '" + key + "' = '" + val + "'.");
                }
             }
          catch( IOException ex )
             {
             ex.printStackTrace();
             }

		return sPwd;

	} // loadProp

	/**
	 * ldapLogin
	 * Arguments:
	 * sUserDN  : either COSMAS II specific Account Name or IDS wide (IDM) account name;
	 * sUserPwd : either COSMAS II specific Password     or IDS wide (IDM) password;
	 * return   : 0 = OK, User Account + Pwd are OK, no restrictions;
	 *            1 = internal error: cannot verify User+Pwd;
	 *            2 = User Account or Pwd unknown;
	 *            3 = User Account locked;
	 *            4 = User known, but has not registered to KorAP/C2 Service yet;
	 * LDAP Attributes that are checked (definition by Eric Seubert, 02.02.17):
	 *  idsC2 = TRUE  -> Zugang zu C2 (registriert und zugelassen)
	 *  idsC2 = FALSE (bzw Attribut nicht vorhanden) 
	 *		            -> kein Zugang zu C2 (nicht zugelassen, egal ob registriert oder nicht)
	 *
	 *	idsStatus = 0 -> Nutzerkennung OK;
	 *	idsStatus = 1 -> Nutzer ist kein aktiver IDS-Mitarbeiter
	 *  idsStatus = 3 -> Nutzer ist LDAP-weit gesperrt
	 */

	public static int login(String sUserDN, String sUserPwd, String ldapConfig) throws LDAPException

	{
	String sUserC2DN	= sUserDN;
	String sUserC2Pwd	= sUserPwd;

	/* login with e-mail - 15.09.21/FB:
	 */
	String ldapFilter = String.format("(|(&(mail=%s)(userPassword=%s))(&(uid=%s)(userPassword=%s))(&(idsC2Profile=%s)(idsC2Password=%s)))",
			sUserDN, sUserPwd, sUserDN, sUserPwd, sUserC2DN, sUserC2Pwd);
	/* without e-mail login:
	 * String ldapFilter = String.format("(|(&(uid=%s)(userPassword=%s))(&(idsC2Profile=%s)(idsC2Password=%s)))",
												 sUserDN, sUserPwd, sUserC2DN, sUserC2Pwd);
	 */
	SearchResult srchRes = null;

	try{
		sPwd = loadProp(ldapConfig);
		}
	catch( IOException e )
		{
		System.out.println("Error: LDAPAuth.login: cannot load Property file!");
		return LDAP_AUTH_RINTERR;
		}
															
	if( DEBUGLOG )
		{
		//System.out.printf("LDAP Version      = %d.\n", LDAPConnection.LDAP_V3);
		System.out.printf("LDAP Host & Port  = '%s':%d.\n", ldapHost, ldapPort);
		System.out.printf("Login User & Pwd  = '%s' + '%s'\n", sUserDN, sUserPwd);
		}

	// LDAP Connection:
	if( DEBUGLOG ) System.out.println("");

	LDAPConnection lc = new LDAPConnection();
	try {
		// connect to LDAP Server:
		lc.connect(ldapHost, ldapPort);
		if( DEBUGLOG ) System.out.println("LDAP Connection = OK\n");
		}
	catch( LDAPException e) 	
		{
		System.err.printf("Error: login: Connecting to LDAP Server: failed: '%s'!\n", e.toString());
		return ldapTerminate(lc, LDAP_AUTH_RCONNECT);
		}

	if( DEBUGLOG ) 
		System.out.printf("Debug: isConnected=%d\n", lc.isConnected() ? 1 : 0);
	
	try {
		// bind to server:
		if( DEBUGLOG ) System.out.printf("Binding with '%s' + '%s'...\n", sLoginDN, sPwd);
		lc.bind(sLoginDN, sPwd);
		if( DEBUGLOG ) System.out.printf("Binding: OK.\n");
		}
	catch( LDAPException e )
		{
		System.err.printf("Error: login: Binding failed: '%s'!\n", e.toString());
		return ldapTerminate(lc, LDAP_AUTH_RINTERR);
		}

	if( DEBUGLOG ) 
		System.out.printf("Debug: isConnected=%d\n", lc.isConnected() ? 1 : 0);
		
	if( DEBUGLOG ) System.out.printf("Finding user '%s'...\n", sUserDN);
	try{
		// SCOPE_SUB = Scope Subtree.
		if( DEBUGLOG ) System.out.printf("Finding Filter: '%s'.\n", ldapFilter);

		// hier werden alle Attribute abgefragt:
		//srchRes = lc.search(ldapBase, SearchScope.SUB, ldapFilter, null);
		// wir fragen nur diese Attribute ab:
		srchRes = lc.search(ldapBase, SearchScope.SUB, ldapFilter, attStatus, attC2);

		if( DEBUGLOG ) System.out.printf("Finding '%s': %d entries.\n", sUserDN, srchRes.getEntryCount());
		}
	catch( LDAPSearchException e )
		{
		System.err.printf("Error: login: Search for User failed: '%s'!\n", e.toString());
		return ldapTerminate(lc, LDAP_AUTH_RUNKNOWN);
		}

	if( srchRes.getEntryCount() == 0 )
		{
		if( DEBUGLOG ) System.out.printf("Finding '%s': no entry found!\n", sUserDN);
		return ldapTerminate(lc, LDAP_AUTH_RUNKNOWN);
		}

	if( DEBUGLOG ) System.out.println("Display results:");

	Boolean
		bStatus = false,
		bC2     = false;
	
	// Attribute pr�fen:
	for (SearchResultEntry e : srchRes.getSearchEntries())
		{
		for( Attribute attr : e.getAttributes() )
			{
			Integer val;

			if( DEBUGLOG ) 
				System.out.printf(" att: '%s'='%s'.\n", attr.getName(), attr.getValue());

			// checking pertinent attribut/value pairs:
			// "idsStatus": values 0=OK, 1=inaktiv=OK, 2-3 = locked account.
			if( attr.getName().equals(attStatus) )
				{
				if( (val = attr.getValueAsInteger()) == null || (val != 0 && val != 1) )
					{
					if( DEBUGLOG ) System.out.printf("idsStatus = '%s' -> User locked!\n", attr.getValue());
					return ldapTerminate(lc, LDAP_AUTH_RLOCKED);
					}
				if( DEBUGLOG ) System.out.printf(" att: '%s'='%s': OK.\n", attr.getName(), attr.getValue());
				bStatus = true;
				}

			// "c2IDS" must be set to "TRUE" = User known, but has not yet registered to C2 Service -> KorAP Service.
			if( attr.getName().equals(attC2) ) 
				{
				if( attr.getValue().equals("FALSE") )
					{
					if( DEBUGLOG ) 
						System.out.printf("idsC2 = '%s'-> User known, but has not registered C2/KorAP Service yet!\n", 
							attr.getValue());
					return ldapTerminate(lc, LDAP_AUTH_RNOTREG);
					}
				if( DEBUGLOG ) 
					System.out.printf(" att: idsC2 = '%s'-> registered User: OK.\n", attr.getValue());
				bC2 = true;
				}
			}

		if( DEBUGLOG ) System.out.println();
		}

	if( bStatus == true && bC2 == true )
		{
		return ldapTerminate(lc, LDAP_AUTH_ROK); // OK.
		}
	else
		return ldapTerminate(lc, LDAP_AUTH_RNOTREG); // Attribute konnten nicht gepr�ft werden.
	
	} // ldapLogin

	/**
	 *                getEmail():
	 * 
	 * Arguments:
	 * sUserDN  	: either COSMAS II specific Account Name or IDS wide (IDM) account name;
	 * ldapConfig	: path+file name of LDAP configuration file.
	 * 
	 * Returns		: the requested Email of sUserDN.
	 * Notices:
	 * - no password needed. Assuming that sUserDN is already authorized and active.
	 * 
	 * 
	 * 16.09.21/FB
	 */

	public static String getEmail(String sUserDN, String ldapConfig) throws LDAPException

	{
	final String func = "LdapAuth3.getEmail";
	
	// sUSerDN is either C2/KorAP specific account name or the IDS wide account name:
	String ldapFilter = String.format("(|(uid=%s)(idsC2Profile=%s))", sUserDN, sUserDN);

	SearchResult srchRes = null;

	try{
		sPwd = loadProp(ldapConfig);
		}
	catch( IOException e )
		{
		System.err.printf("Error: %s: cannot load Property file '%s'!", func, ldapConfig);
		return null;
		}
															
	if( DEBUGLOG )
		{
		//System.out.printf("LDAP Version      = %d.\n", LDAPConnection.LDAP_V3);
		System.out.printf("%s: LDAP Host & Port  = '%s':%d.\n", func, ldapHost, ldapPort);
		System.out.printf("%s: User Account      = '%s'\n", func, sUserDN);
		}

	// LDAP Connection:
	if( DEBUGLOG ) System.out.println("");

	LDAPConnection 
		lc = new LDAPConnection();
	
	try {
		// connect to LDAP Server:
		lc.connect(ldapHost, ldapPort);
		if( DEBUGLOG ) System.out.println("LDAP Connection = OK\n");
		}
	catch( LDAPException e) 	
		{
		System.err.printf("Error: %s: Connecting to LDAP Server: failed: '%s'!\n", func, e.toString());
		ldapTerminate(lc, LDAP_AUTH_RCONNECT);
		return null;
		}

	if( DEBUGLOG ) 
		System.out.printf("Debug: isConnected=%d\n", lc.isConnected() ? 1 : 0);
	
	try {
		// bind to server:
		if( DEBUGLOG ) System.out.printf("Binding with '%s' + '%s'...\n", sLoginDN, sPwd);
		lc.bind(sLoginDN, sPwd);
		if( DEBUGLOG ) System.out.printf("Binding: OK.\n");
		}
	catch( LDAPException e )
		{
		System.err.printf("Error: %s: Binding failed: '%s'!\n", func, e.toString());
		ldapTerminate(lc, LDAP_AUTH_RINTERR);
		return null;
		}

	if( DEBUGLOG ) 
		System.out.printf("Debug: isConnected=%d\n", lc.isConnected() ? 1 : 0);
		
	if( DEBUGLOG ) System.out.printf("Finding user '%s'...\n", sUserDN);
	try{
		// SCOPE_SUB = Scope Subtree.
		if( DEBUGLOG ) System.out.printf("Finding Filter: '%s'.\n", ldapFilter);

		// requested attribute is attEmail:
		srchRes = lc.search(ldapBase, SearchScope.SUB, ldapFilter, attEmail);

		if( DEBUGLOG ) System.out.printf("Finding '%s': %d entries.\n", sUserDN, srchRes.getEntryCount());
		}
	catch( LDAPSearchException e )
		{
		System.err.printf("Error: %s: Search for User '%s' failed: '%s'!\n", func, sUserDN, e.toString());
		ldapTerminate(lc, LDAP_AUTH_RUNKNOWN);
		return null;
		}

	if( srchRes.getEntryCount() == 0 )
		{
		if( DEBUGLOG ) System.out.printf("Error: %s: account '%s': 0 entries found!\n", func, sUserDN);
		ldapTerminate(lc, LDAP_AUTH_RUNKNOWN);
		return null;
		}

	if( DEBUGLOG ) System.out.printf("Debug: %s: Extract email from results.\n", func);

	// Now get email from result.
	// expected: 1 result with 1 attribute value:
	
	SearchResultEntry 
		e;
	Attribute
		attr;
	String
		email;
	
	if( (e = srchRes.getSearchEntries().get(0)) != null &&
		(attr = e.getAttribute(attEmail)) != null && 
		(email = attr.getValue()) != null )
		{
		// return email:
		if( DEBUGLOG ) 
			System.out.printf("Debug: %s: account '%s' has email = '%s'.\n", func, sUserDN, email);
		ldapTerminate(lc, LDAP_AUTH_ROK); // OK.
		return email;
		}
	
	// cannot obtain email from result:
	System.err.printf("Error: %s: account '%s': no attribute '%s' for email found!\n", func, sUserDN, attEmail);
	
	ldapTerminate(lc, LDAP_AUTH_RNOEMAIL); // no email found.
	return null;
	} // getEmail

/**
 * ldapTerminate
 */
 
public static int ldapTerminate(LDAPConnection lc, int ret)

	{
	if( DEBUGLOG ) System.out.println("Terminating...");
	/*
	try{
		lc.finalize();
		if( DEBUGLOG ) System.out.println("Debug: finalize: OK.");
		}
	catch( LDAPException e )
		{
		System.out.printf("finalize failed: '%s'!\n", e.toString());
		}
	*/

	lc.close(null);
	if( DEBUGLOG ) System.out.println("closing connection: done.\n");
	return ret;
	} // ldapTerminate

}

