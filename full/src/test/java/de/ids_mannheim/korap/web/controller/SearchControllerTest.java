package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import javax.ws.rs.core.MediaType;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * @author margaretha, hanl
 * @lastUpdate 22/03/2018
 *
 */
public class SearchControllerTest extends SpringJerseyTest {

    @Autowired
    private HttpAuthorizationHandler handler;
    

    @Test
    public void testSearchQueryPublicCorpora () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp")
                .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("availability", node.at("/collection/key").asText());
        assertEquals("CC-BY.*", node.at("/collection/value").asText());
        assertEquals("availability(FREE)",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
    }

	
    @Test
    public void testSearchQueryFailure () throws KustvaktException{
        ClientResponse response = resource()
			.path("search").queryParam("q", "[orth=der")
			.queryParam("ql", "poliqarp")
			.queryParam("cq", "corpusSigle=WPD | corpusSigle=GOE")
			.queryParam("count", "13")
			.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());

        String ent = response.getEntity(String.class);
		JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(302, node.at("/errors/0/0").asInt());
        assertEquals(302, node.at("/errors/1/0").asInt());
		assertTrue(node.at("/errors/2").isMissingNode());
		assertFalse(node.at("/collection").isMissingNode());
        assertEquals(13, node.at("/meta/count").asInt());
    }


    @Test
    public void testSearchQueryWithMeta () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("cutoff", "true")
                .queryParam("count", "5").queryParam("page", "1")
                .queryParam("context", "40-t,30-t").get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertTrue(node.at("/meta/cutOff").asBoolean());
        assertEquals(5, node.at("/meta/count").asInt());
        assertEquals(0, node.at("/meta/startIndex").asInt());
        assertEquals("token", node.at("/meta/context/left/0").asText());
        assertEquals(40, node.at("/meta/context/left/1").asInt());
        assertEquals(30, node.at("/meta/context/right/1").asInt());
        assertEquals(-1, node.at("/meta/totalResults").asInt());
    }

    @Test
    public void testSearchQueryFreeExtern () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=die]")
                .queryParam("ql", "poliqarp")
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("availability", node.at("/collection/key").asText());
        assertEquals("CC-BY.*", node.at("/collection/value").asText());
        assertEquals("availability(FREE)",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
    }
    
    @Test
    public void testSearchQueryFreeIntern () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=die]")
                .queryParam("ql", "poliqarp")
                .header(HttpHeaders.X_FORWARDED_FOR, "172.27.0.32")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("koral:doc", node.at("/collection/@type").asText());
        assertEquals("availability", node.at("/collection/key").asText());
        assertEquals("CC-BY.*", node.at("/collection/value").asText());
        assertEquals("availability(FREE)",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
    }
    
    
    @Test
    public void testSearchQueryExternAuthorized () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=die]")
                .queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
//        System.out.println(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("ACA.*", node.at("/collection/operands/1/operands/0/value").asText());
        assertEquals("QAO-NC", node.at("/collection/operands/1/operands/1/value").asText());
        assertEquals("operation:or", node.at("/collection/operation").asText());
        assertEquals("availability(PUB)",
                node.at("/collection/rewrites/0/scope").asText());
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
    }

    @Test
    public void testSearchQueryInternAuthorized () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=die]")
                .queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .header(HttpHeaders.X_FORWARDED_FOR, "172.27.0.32")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        //EM: no rewrite is needed
//        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
//        assertEquals("QAO.*", node.at("/collection/operands/0/value").asText());
//        assertEquals("ACA.*",
//                node.at("/collection/operands/1/operands/0/value").asText());
//        assertEquals("CC-BY.*",
//                node.at("/collection/operands/1/operands/1/value").asText());
//        assertEquals("operation:or", node.at("/collection/operation").asText());
//        assertEquals("availability(ALL)",
//                node.at("/collection/rewrites/0/scope").asText());
//        assertEquals("operation:insertion",
//                node.at("/collection/rewrites/0/operation").asText());
    }

 // EM: shouldn't this case gets CorpusAccess.PUB ? 
    @Test
    @Ignore
    public void testSearchQueryWithCollectionQueryAuthorizedWithoutIP () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=das]")
                .queryParam("ql", "poliqarp")
                .queryParam("cq", "textClass=politik & corpusSigle=BRZ10")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());

        JsonNode node = JsonUtils.readTree(response.getEntity(String.class));
        assertNotNull(node);
        assertEquals("operation:insertion",
                node.at("/collection/rewrites/0/operation").asText());
        assertEquals("availability(PUB)",
                node.at("/collection/rewrites/0/scope").asText());
        // EM: double AND operations
        assertEquals("availability",
                node.at("/collection/operands/0/key").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("textClass",
                node.at("/collection/operands/1/operands/0/key").asText());
        assertEquals("corpusSigle",
                node.at("/collection/operands/1/operands/1/key").asText());
    }
    
    @Test
    @Ignore
    public void testSearchQueryAuthorizedWithoutIP () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=die]")
                .queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("ACA.*", node.at("/collection/operands/1/value").asText());
        assertEquals("operation:or", node.at("/collection/operation").asText());
        assertEquals("availability(PUB)",
                node.at("/collection/rewrites/0/scope").asText());
    }
    
    

    @Test
    @Ignore
    public void testSearchForPublicCorpusWithStringId () throws KustvaktException {
        ClientResponse response = resource()
                .path("corpus").path("GOE").path("search")
                .queryParam("q", "blau").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and",
                node.at("/collection/operation").asText());
        assertEquals("availability",
                node.at("/collection/operands/0/key").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("corpusSigle",
                node.at("/collection/operands/1/key").asText());
        assertEquals("GOE", node.at("/collection/operands/1/value").asText());
        assertNotEquals(0, node.path("matches").size());
    }


    @Test
    @Ignore
    public void testSearchForVirtualCollectionWithStringId () throws KustvaktException{
        ClientResponse response = resource()
                .path("collection").path("GOE-VC").path("search")
                .queryParam("q", "blau").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and",
                node.at("/collection/operation").asText());
        assertNotEquals(0, node.at("/collection/operands").size());
        assertEquals("corpusSigle",
                node.at("/collection/operands/0/key").asText());
        assertEquals("GOE", node.at("/collection/operands/0/value").asText());
        assertEquals("creationDate",
                node.at("/collection/operands/1/key").asText());
        assertEquals("1810-01-01",
                node.at("/collection/operands/1/value").asText());
        assertEquals(1, node.at("/meta/totalResults").asInt());
    }

    @Test
    @Ignore
    public void testSearchForCorpusWithStringIdUnauthorized () throws KustvaktException {
        ClientResponse response = resource()
                .path("corpus").path("WPD15").path("search")
                .queryParam("q", "blau").queryParam("ql", "poliqarp")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode error = JsonUtils.readTree(ent).get("errors").get(0);
        assertEquals(101, error.get(0).asInt());
        assertEquals("[Cannot found public Corpus with ids: [WPD15]]",
                error.get(2).asText());
    }


    @Test
    @Ignore
    public void testSearchForSpecificCorpus () throws KustvaktException{
        ClientResponse response = resource()
                .path("corpus").path("GOE").path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .header(Attributes.AUTHORIZATION,
                        handler.createBasicAuthorizationHeaderValue("kustvakt", "kustvakt2015"))
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertNotNull(node);
        assertEquals("koral:docGroup", node.at("/collection/@type").asText());
        assertEquals("operation:and",
                node.at("/collection/operation").asText());
        assertEquals("availability",
                node.at("/collection/operands/0/key").asText());
        assertEquals("CC-BY.*",
                node.at("/collection/operands/0/value").asText());
        assertEquals("corpusSigle",
                node.at("/collection/operands/1/key").asText());
        assertEquals("GOE", node.at("/collection/operands/1/value").asText());
    }




    @Test
    public void testSearchSentenceMeta () throws KustvaktException{
        ClientResponse response = resource()
                .path("search").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp").queryParam("context", "sentence")
                .get(ClientResponse.class);
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("base/s:s", node.at("/meta/context").asText());
        assertNotEquals("${project.version}", "/meta/version");
    }


    @Test
    public void testSearchSimpleCQL () throws KustvaktException{
        QuerySerializer s = new QuerySerializer();
        s.setQuery("(der) or (das)", "CQL");

        ClientResponse response = resource()
                .path("search").post(ClientResponse.class, s.toJSON());
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);

        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        //        assertEquals(17027, node.at("/meta/totalResults").asInt());
    }


    @Test
    public void testSearchRawQuery () throws KustvaktException{
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=der]", "poliqarp");
        s.setCollection("corpusSigle=GOE");

        s.setQuery("Wasser", "poliqarp");
//        System.out.println(s.toJSON());
        ClientResponse response = resource()
                .path("search").post(ClientResponse.class, s.toJSON());
        assertEquals(ClientResponse.Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.getEntity(String.class);


        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertNotEquals(0, node.path("matches").size());
        //        assertEquals(10993, node.at("/meta/totalResults").asInt());
    }

}
