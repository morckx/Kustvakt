package de.ids_mannheim.korap.oauth2.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.constant.TokenType;
import de.ids_mannheim.korap.dao.AdminDao;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Error;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import de.ids_mannheim.korap.oauth2.dao.AccessScopeDao;
import de.ids_mannheim.korap.oauth2.entity.AccessScope;
import de.ids_mannheim.korap.security.context.TokenContext;

@Service
public class OAuth2ScopeService {

    @Autowired
    private AccessScopeDao accessScopeDao;

    @Autowired
    private AdminDao adminDao;
    
    /**
     * Converts a set of scope strings to a set of {@link AccessScope}
     * 
     * @param scopes
     * @return
     * @throws KustvaktException
     */
    public Set<AccessScope> convertToAccessScope (Set<String> scopes)
            throws KustvaktException {

        List<AccessScope> definedScopes = accessScopeDao.retrieveAccessScopes();
        Set<AccessScope> requestedScopes =
                new HashSet<AccessScope>(scopes.size());
        int index;
        for (String scope : scopes) {
            index = definedScopes.indexOf(new AccessScope(scope));
            if (index == -1) {
                throw new KustvaktException(StatusCodes.INVALID_SCOPE,
                        scope + " is an invalid scope",
                        OAuth2Error.INVALID_SCOPE);
            }
            else {
                requestedScopes.add(definedScopes.get(index));
            }
        }
        return requestedScopes;
    }

    public String convertAccessScopesToString (Set<AccessScope> scopes) {
        Set<String> set = convertAccessScopesToStringSet(scopes);
        return String.join(" ", set);
    }

    public Set<String> convertAccessScopesToStringSet (
            Set<AccessScope> scopes) {
        Set<String> set = scopes.stream().map(scope -> scope.toString())
                .collect(Collectors.toSet());
        return set;
    }

    /**
     * Simple reduction of requested scopes, i.e. excluding any scopes
     * that are not default scopes for a specific authorization grant.
     * 
     * @param scopes
     * @param defaultScopes
     * @return accepted scopes
     */
    public Set<String> filterScopes (Set<String> scopes,
            Set<String> defaultScopes) {
        Stream<String> stream = scopes.stream();
        Set<String> filteredScopes =
                stream.filter(scope -> defaultScopes.contains(scope))
                        .collect(Collectors.toSet());
        return filteredScopes;
    }

    public void verifyScope (TokenContext context, OAuth2Scope requestScope)
            throws KustvaktException {
        if (!adminDao.isAdmin(context.getUsername())
                && context.getTokenType().equals(TokenType.BEARER)) {
            Map<String, Object> parameters = context.getParameters();
            String scope = (String) parameters.get(Attributes.SCOPE);
            if (!scope.contains(requestScope.toString())) {
                throw new KustvaktException(StatusCodes.AUTHORIZATION_FAILED,
                        "Scope " + requestScope + " is not authorized");
            }
        }
    }
}
