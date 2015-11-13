package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 30/06/2015
 */
public interface RewriteTask {

    interface RewriteBefore extends RewriteTask {
        /**
         * @param node   Json node in KoralNode wrapper
         * @param config {@link KustvaktConfiguration} singleton instance to use default configuration parameters
         * @param user   injected by rewrite handler if available. Might cause {@link NullPointerException} if not checked properly
         * @return
         */
        JsonNode preProcess(KoralNode node, KustvaktConfiguration config,
                User user);
    }

    interface RewriteAfter extends RewriteTask {
        JsonNode postProcess(KoralNode node);
    }

    /**
     * query rewrites get injected the entire query from root containing all child nodes
     * <p/>
     * {@link RewriteQuery} does not allow the deletion of the root node or subnode through KoralNode.
     * The {@link de.ids_mannheim.korap.resource.rewrite.RewriteHandler} will igonore respecitve invalid requests
     */
    interface RewriteQuery extends RewriteBefore, RewriteAfter {
    }

    /**
     * Koral term nodes that are subject to rewrites
     * Be aware that node rewrites are processed before query rewrites. Thus query rewrite may override previous node rewrites
     * <p/>
     * {@link RewriteNode} rewrite supports the deletion of the respective node by simply setting the node invalid in KoralNode
     */
    interface RewriteNode extends RewriteBefore {
    }

    /**
     * koral token nodes that are subject to rewrites
     * Be aware that node rewrites are processed before query rewrites. Thus query rewrite may override previous node rewrites
     * {@link RewriteKoralToken} rewrite DOES NOT support the deletion of the respective node
     */
    interface RewriteKoralToken extends RewriteBefore {
    }
}
