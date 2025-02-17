package de.ids_mannheim.korap.misc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import javax.annotation.PostConstruct;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import de.ids_mannheim.korap.KrillCollection;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.CollectionQueryProcessor;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.web.SearchKrill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author hanl
 * @date 14/01/2016
 */
@DisplayName("Local Query Test")
class LocalQueryTest extends SpringJerseyTest {

    private static String index;

    @Autowired
    KustvaktConfiguration config;

    @PostConstruct
    public void setup() throws Exception {
        index = config.getIndexDir();
    }

    @Test
    @DisplayName("Test Query")
    void testQuery() throws KustvaktException {
        String qstring = "creationDate since 1786 & creationDate until 1788";
        // qstring = "creationDate since 1765 & creationDate until 1768";
        // qstring = "textType = Aphorismus";
        // qstring = "title ~ \"Werther\"";
        SearchKrill krill = new SearchKrill(index);
        KoralCollectionQueryBuilder coll = new KoralCollectionQueryBuilder();
        coll.with(qstring);
        String stats = krill.getStatistics(coll.toJSON());
        assert stats != null && !stats.isEmpty() && !stats.equals("null");
    }

    @Test
    @DisplayName("Test Coll Query")
    void testCollQuery() throws IOException, KustvaktException {
        String qstring = "creationDate since 1800 & creationDate until 1820";
        CollectionQueryProcessor processor = new CollectionQueryProcessor();
        processor.process(qstring);
        String s = JsonUtils.toJSON(processor.getRequestMap());
        KrillCollection c = new KrillCollection(s);
        c.setIndex(new SearchKrill(index).getIndex());
        long docs = c.numberOf("documents");
        assert docs > 0 && docs < 15;
    }

    @Test
    @DisplayName("Test Coll Query 2")
    void testCollQuery2() throws IOException {
        String query = "{\"@context\":\"http://korap.ids-mannheim.de/ns/koral/0.3/context.jsonld\",\"errors\":[],\"warnings\":[],\"messages\":[],\"collection\":{\"@type\":\"koral:docGroup\",\"operation\":\"operation:and\",\"operands\":[{\"@type\":\"koral:doc\",\"key\":\"creationDate\",\"type\":\"type:date\",\"value\":\"1786\",\"match\":\"match:geq\"},{\"@type\":\"koral:doc\",\"key\":\"creationDate\",\"type\":\"type:date\",\"value\":\"1788\",\"match\":\"match:leq\"}]},\"query\":{},\"meta\":{}}";
        KrillCollection c = new KrillCollection(query);
        c.setIndex(new SearchKrill(index).getIndex());
        long sent = c.numberOf("base/sentences");
        long docs = c.numberOf("documents");
        assertNotNull(sent);
        assertNotNull(docs);
    }
}
