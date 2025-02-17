package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.user.Userdata;
import de.ids_mannheim.korap.utils.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hanl
 * @date 09/12/2014
 */
public class Scopes {

    public enum Scope {
        openid, profile, email, queries, account, preferences, search

    }

    private static final String[] profile = { Attributes.EMAIL,
            Attributes.FIRSTNAME, Attributes.LASTNAME, Attributes.INSTITUTION,
            Attributes.ADDRESS, Attributes.PHONE, Attributes.GENDER,
            Attributes.COUNTRY };

    private static final String[] OPENID_CONNECT = { Scope.profile.toString() };

    private static final Enum[] SERVICE_DEFAULTS = { Scope.account,
            Scope.preferences, Scope.search, Scope.queries };


    public static Scopes getProfileScopes (Userdata values) {
        Scopes r = new Scopes();
        for (String key : profile) {
            Object v = values.get(key);
            if (v != null)
                r.values.put(key, v);
        }
        return r;
    }


    /**
     * expects space separated values
     * 
     * @param scopes
     * @return
     */
    //todo: test
    public static Scope[] mapScopes (String scopes) {
        List<Enum> s = new ArrayList<>();
        for (String value : scopes.split(" "))
            s.add(Scope.valueOf(value.toLowerCase()));
        return s.toArray(new Scope[s.size()]);
    }


    public static Scopes mapScopes (String scopes, Userdata details) {
        Scopes m = new Scopes();
        if (scopes != null && !scopes.isEmpty()) {
            Scope[] scopearr = mapScopes(scopes);
            for (Scope s : scopearr) {
                Object v = details.get(s.toString());
                if (v != null)
                    m.values.put(s.toString(), v);
            }
            if (scopes.contains(Scope.profile.toString()))
                m.values.putAll(Scopes.getProfileScopes(details).values);
            m.values.put(Attributes.SCOPE, scopes);
        }
        return m;
    }

    private Map<String, Object> values;


    private Scopes () {
        this.values = new HashMap<>();
    }


    public String toEntity () throws KustvaktException {
        if (this.values.isEmpty())
            return "";
        return JsonUtils.toJSON(this.values);
    }


    public Map<String, Object> toMap () {
        return new HashMap<>(this.values);
    }

}
