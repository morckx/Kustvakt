package de.ids_mannheim.korap.config;

import java.io.IOException;
import java.util.EnumSet;

import org.springframework.beans.factory.annotation.Autowired;

import de.ids_mannheim.korap.annotation.AnnotationParser;
import de.ids_mannheim.korap.annotation.FreeResourceParser;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.constant.OAuth2Scope;
import de.ids_mannheim.korap.oauth2.dao.AccessScopeDao;
import de.ids_mannheim.korap.util.QueryException;

/**
 * Initializes values in the database from kustvakt configuration and
 * performs named VC caching.
 * 
 * @author margaretha
 *
 */
public class Initializator {

    @Autowired
    private AccessScopeDao accessScopeDao;
    @Autowired
    private NamedVCLoader loader;
    @Autowired
    private AnnotationParser annotationParser;
    @Autowired
    private FreeResourceParser resourceParser;
    
    public Initializator () {}

    public void init () throws IOException, QueryException, KustvaktException {
        setInitialAccessScope();
        loader.loadVCToCache();
    }

    public void initAnnotation ()
            throws IOException, QueryException, KustvaktException {
        setInitialAccessScope();
        loader.loadVCToCache();
        annotationParser.run();
        resourceParser.run();
    }

    public void initTest () {
        setInitialAccessScope();
    }

    private void setInitialAccessScope () {
        EnumSet<OAuth2Scope> scopes = EnumSet.allOf(OAuth2Scope.class);
        accessScopeDao.storeAccessScopes(scopes);
    }
}
