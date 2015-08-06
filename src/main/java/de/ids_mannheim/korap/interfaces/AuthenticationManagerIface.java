package de.ids_mannheim.korap.interfaces;

import de.ids_mannheim.korap.exceptions.KorAPException;
import de.ids_mannheim.korap.user.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author hanl
 * @date 15/06/2015
 */
public abstract class AuthenticationManagerIface {

    private Map<String, AuthenticationIface> providers;

    public AuthenticationManagerIface() {
        this.providers = new HashMap<>();
    }

    public void setProviders(Set<AuthenticationIface> providers) {
        for (AuthenticationIface i : providers)
            this.providers.put(i.getIdentifier(), i);
    }

    protected AuthenticationIface getProvider(String key) {
        AuthenticationIface iface;
        if (key == null)
            iface = this.providers.get(Attributes.API_AUTHENTICATION);
        else
            iface = this.providers.get(key.toUpperCase());
        return iface;
    }

    public abstract TokenContext getTokenStatus(String token, String host,
            String useragent) throws KorAPException;

    public abstract User getUser(String username) throws KorAPException;

    public abstract User authenticate(int type, String username,
            String password, Map<String, Object> attributes)
            throws KorAPException;

    public abstract TokenContext createTokenContext(User user,
            Map<String, Object> attr, String provider_key)
            throws KorAPException;

    public abstract void logout(TokenContext context) throws KorAPException;

    public abstract void lockAccount(User user) throws KorAPException;

    public abstract User createUserAccount(Map<String, Object> attributes)
            throws KorAPException;

    public abstract boolean updateAccount(User user) throws KorAPException;

    public abstract boolean deleteAccount(User user) throws KorAPException;

    public abstract UserDetails getUserDetails(User user) throws KorAPException;

    public abstract UserSettings getUserSettings(User user)
            throws KorAPException;

    public abstract void updateUserDetails(User user, UserDetails details)
            throws KorAPException;

    public abstract void updateUserSettings(User user, UserSettings settings)
            throws KorAPException;

    public abstract Object[] validateResetPasswordRequest(String username,
            String email) throws KorAPException;

    public abstract void resetPassword(String uriFragment, String username,
            String newPassphrase) throws KorAPException;

    public abstract void confirmRegistration(String uriFragment,
            String username) throws KorAPException;
}
