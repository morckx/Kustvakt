package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.BeanInjectable;
import de.ids_mannheim.korap.config.BeansFactory;
import de.ids_mannheim.korap.config.ContextHolder;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.handlers.DocumentDao;
import de.ids_mannheim.korap.resources.Document;
import de.ids_mannheim.korap.user.User;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * @author hanl
 * @date 12/11/2015
 */
//todo : test
public class DocMatchRewrite implements RewriteTask.IterableRewriteAt,
        BeanInjectable {

    private DocumentDao docDao;
    private Cache cache;


    public DocMatchRewrite () {
        this.cache = CacheManager.getInstance().getCache("documents");
    }


    @Override
    public void insertBeans (ContextHolder beans) {
        this.docDao = BeansFactory.getTypeFactory().getTypedBean(
                beans.getResourceProvider(), Document.class);
    }


    @Override
    public JsonNode postProcess (KoralNode node) throws KustvaktException {
        Document doc = null;
        if (this.docDao == null)
            throw new RuntimeException("Document dao must be set!");

        if (node.has("docID")) {
            String docID = node.get("docID");
            Element e = this.cache.get(docID);
            if (e == null) {
                doc = docDao.findbyId(docID, null);
                if (doc != null)
                    this.cache.put(new Element(docID, doc));
            }
            else
                doc = (Document) e.getObjectValue();

            if (doc != null && doc.isDisabled())
                node.removeNode();
        }
        return node.rawNode();
    }


    @Override
    public String path () {
        return "matches";
    }


    @Override
    public JsonNode preProcess (KoralNode node, KustvaktConfiguration config,
            User user) {
        return null;
    }
}
