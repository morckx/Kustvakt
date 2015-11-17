package de.ids_mannheim.korap.resource.rewrite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.user.User;

/**
 * @author hanl
 * @date 25/09/2015
 */
public class IdWriter implements RewriteTask.RewriteKoralToken {

    private int counter;

    public IdWriter() {
        super();
        this.counter = 0;

    }

    @Override
    public JsonNode preProcess(KoralNode node, KustvaktConfiguration config,
            User user) {
        if (node.get("@type").equals("koral:token")) {
            String s = extractToken(node.rawNode());
            if (s != null && !s.isEmpty())
                node.put("idn", s + "_" + counter++);
        }

        return node.rawNode();
    }

    @Deprecated
    private JsonNode addId(JsonNode node) {
        if (node.isObject()) {
            ObjectNode o = (ObjectNode) node;
            String s = extractToken(node);
            if (s != null && !s.isEmpty())
                o.put("idn", s + "_" + counter++);
        }
        return node;
    }

    // fixme: koral token --> how does grouping behave?!
    private String extractToken(JsonNode token) {
        JsonNode wrap = token.path("wrap");
        if (!wrap.isMissingNode())
            return wrap.path("key").asText();
        return null;
    }

}
