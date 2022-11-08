package de.ids_mannheim.korap.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.config.LiteJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.query.serialize.QuerySerializer;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.SearchKrill;

public class LiteSearchControllerTest extends LiteJerseyTest {

    @Autowired
    private SearchKrill searchKrill;
    @Autowired
    private KustvaktConfiguration config;

//  EM: The API is disabled
    @Ignore   
    @Test
    public void testGetJSONQuery () throws KustvaktException {
        Response response = target().path(API_VERSION).path("query")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("context", "sentence").queryParam("count", "13")
                .request()
                .method("GET");
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("opennlp", node.at("/query/wrap/foundry").asText());
        assertEquals("sentence", node.at("/meta/context").asText());
        assertEquals("13", node.at("/meta/count").asText());
    }
    
//  EM: The API is disabled
    @Ignore
    @Test
    public void testbuildAndPostQuery () throws KustvaktException {
        Response response = target().path(API_VERSION).path("query")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("cq", "corpusSigle=WPD | corpusSigle=GOE")
                .request()
                .method("GET");
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());

        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);

        response = target().path(API_VERSION).path("search")
                .request()
                .post(Entity.json(query));

        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String matches = response.readEntity(String.class);
        JsonNode match_node = JsonUtils.readTree(matches);
        assertNotEquals(0, match_node.path("matches").size());
    }

    @Test
    public void testApiWelcomeMessage () {
        Response response = target().path(API_VERSION).path("")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String message = response.readEntity(String.class);
        assertEquals(
            "Wes8Bd4h1OypPqbWF5njeQ==",
            response.getHeaders().getFirst("X-Index-Revision")
            );
        assertEquals(message, config.getApiWelcomeMessage());
    }

    @Test
    public void testQueryGet () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("context", "sentence").queryParam("count", "13")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("base/s:s", node.at("/meta/context").asText());
        assertEquals("13", node.at("/meta/count").asText());
        assertNotEquals(0, node.at("/matches").size());
    }

    @Test
    public void testQueryFailure () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das").queryParam("ql", "poliqarp")
                .queryParam("cq", "corpusSigle=WPD | corpusSigle=GOE")
                .queryParam("count", "13")
                .request()
                .get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String query = response.readEntity(String.class);

        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals(302, node.at("/errors/0/0").asInt());
        assertEquals(302, node.at("/errors/1/0").asInt());
        assertTrue(node.at("/errors/2").isMissingNode());
        assertFalse(node.at("/collection").isMissingNode());
        assertEquals(13, node.at("/meta/count").asInt());
    }

    @Test
    public void testFoundryRewrite () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("context", "sentence").queryParam("count", "13")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("opennlp", node.at("/query/wrap/foundry").asText());
    }

//  EM: The API is disabled
    @Test
    @Ignore
    public void testQueryPost () throws KustvaktException {
        QuerySerializer s = new QuerySerializer();
        s.setQuery("[orth=das]", "poliqarp");

        Response response = target().path(API_VERSION).path("search")
                .request()
                .post(Entity.json(s.toJSON()));
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertNotEquals(0, node.at("/matches").size());
    }

    @Test
    public void testParameterField () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("fields", "author,docSigle")
                .queryParam("context", "sentence").queryParam("count", "13")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertNotEquals(0, node.at("/matches").size());
        assertEquals("[\"author\",\"docSigle\"]",
                node.at("/meta/fields").toString());
    }

    @Test
    public void testMatchInfoGetWithoutSpans () throws KustvaktException {
        Response response = target().path(API_VERSION)
                .path("corpus/GOE/AGA/01784/p36-46(5)37-45(2)38-42/matchInfo")
                .queryParam("foundry", "*").queryParam("spans", "false")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals("match-GOE/AGA/01784-p36-46(5)37-45(2)38-42",
                node.at("/matchID").asText());
        assertEquals("Belagerung von Mainz", node.at("/title").asText());
    };

    @Test
    public void testMatchInfoGetWithoutHighlights () throws KustvaktException {
        Response response = target().path(API_VERSION)
                .path("corpus/GOE/AGA/01784/p36-46(5)37-45(2)38-42/matchInfo")
                .queryParam("foundry", "xy").queryParam("spans", "false")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals(
                "<span class=\"context-left\"></span><span class=\"match\">der alte freie Weg nach Mainz war gesperrt, ich mußte über die Schiffbrücke bei Rüsselsheim; in Ginsheim ward <mark>gefüttert; der Ort ist sehr zerschossen; dann über die Schiffbrücke</mark> auf die Nonnenaue, wo viele Bäume niedergehauen lagen, sofort auf dem zweiten Teil der Schiffbrücke über den größern Arm des Rheins.</span><span class=\"context-right\"></span>",
                node.at("/snippet").asText());
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals("match-GOE/AGA/01784-p36-46(5)37-45(2)38-42",
                node.at("/matchID").asText());
        assertEquals("Belagerung von Mainz", node.at("/title").asText());
    };

    @Test
    public void testMatchInfoWithoutExtension () throws KustvaktException {
        Response response = target().path(API_VERSION)
                .path("corpus/GOE/AGA/01784/p36-46(5)37-45(2)38-42")
                .queryParam("foundry", "-").queryParam("spans", "false")
                .queryParam("expand","false")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals("match-GOE/AGA/01784-p36-46(5)37-45(2)38-42",
                node.at("/matchID").asText());
        assertEquals("<span class=\"context-left\"><span class=\"more\"></span></span><span class=\"match\"><mark>gefüttert; der Ort ist sehr zerschossen; dann über die Schiffbrücke</mark></span><span class=\"context-right\"><span class=\"more\"></span></span>",
                node.at("/snippet").asText());
        assertEquals("Belagerung von Mainz", node.at("/title").asText());
    };

    
    @Test
    public void testMatchInfoGetWithHighlights () throws KustvaktException {
        Response response = target().path(API_VERSION)
                .path("corpus/GOE/AGA/01784/p36-46(5)37-45(2)38-42/matchInfo")
                .queryParam("foundry", "xy").queryParam("spans", "false")
                .queryParam("hls", "true")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals(
                "<span class=\"context-left\"></span><span class=\"match\">"
                        + "der alte freie Weg nach Mainz war gesperrt, ich mußte über die "
                        + "Schiffbrücke bei Rüsselsheim; in Ginsheim ward <mark>gefüttert; "
                        + "<mark class=\"class-5 level-0\">der <mark class=\"class-2 level-1\">"
                        + "Ort ist sehr zerschossen; dann</mark> über die Schiffbrücke</mark></mark> "
                        + "auf die Nonnenaue, wo viele Bäume niedergehauen lagen, sofort auf dem "
                        + "zweiten Teil der Schiffbrücke über den größern Arm des Rheins.</span>"
                        + "<span class=\"context-right\"></span>",
                node.at("/snippet").asText());
        assertEquals("match-GOE/AGA/01784-p36-46(5)37-45(2)38-42",
                node.at("/matchID").asText());
        assertEquals("Belagerung von Mainz", node.at("/title").asText());
    };

    @Test
    public void testMatchInfoGet2 () throws KustvaktException {
        Response response = target().path(API_VERSION)

                .path("corpus/GOE/AGA/01784/p36-46/matchInfo")
                .queryParam("foundry", "*")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertNotNull(node);
        assertEquals("GOE/AGA/01784", node.at("/textSigle").asText());
        assertEquals("Belagerung von Mainz", node.at("/title").asText());
    };
    
//  EM: The API is disabled
    @Ignore
    @Test
    public void testCollectionQueryParameter () throws KustvaktException {
        Response response = target().path(API_VERSION).path("query")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("fields", "author, docSigle")
                .queryParam("context", "sentence").queryParam("count", "13")
                .queryParam("cq", "textClass=Politik & corpus=WPD")
                .request()
                .method("GET");
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("Politik",
                node.at("/collection/operands/0/value").asText());
        assertEquals("WPD", node.at("/collection/operands/1/value").asText());

        response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("fields", "author, docSigle")
                .queryParam("context", "sentence").queryParam("count", "13")
                .queryParam("cq", "textClass=Politik & corpus=WPD")
                .request()
                .get();
        // String version =
        // LucenePackage.get().getImplementationVersion();;
        // System.out.println("VERSION "+ version);
        // System.out.println("RESPONSE "+ response);
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        query = response.readEntity(String.class);
        node = JsonUtils.readTree(query);
        assertNotNull(node);
        assertEquals("orth", node.at("/query/wrap/layer").asText());
        assertEquals("Politik",
                node.at("/collection/operands/0/value").asText());
        assertEquals("WPD", node.at("/collection/operands/1/value").asText());
    }

    @Test
    public void testMetaFields () throws KustvaktException {
        Response response =
                target().path(API_VERSION).path("/corpus/GOE/AGA/01784")
                        .request()
                        .method("GET");
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String resp = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(resp);
        // System.err.println(node.toString());

        Iterator<JsonNode> fieldIter = node.at("/document/fields").elements();

        int checkC = 0;
        while (fieldIter.hasNext()) {
            JsonNode field = (JsonNode) fieldIter.next();

            String key = field.at("/key").asText();

            assertEquals("koral:field", field.at("/@type").asText());

            switch (key) {
                case "textSigle":
                    assertEquals("type:string", field.at("/type").asText());
                    assertEquals("GOE/AGA/01784", field.at("/value").asText());
                    checkC++;
                    break;
                case "author":
                    assertEquals("type:text", field.at("/type").asText());
                    assertEquals("Goethe, Johann Wolfgang von",
                            field.at("/value").asText());
                    checkC++;
                    break;
                case "docSigle":
                    assertEquals("type:string", field.at("/type").asText());
                    assertEquals("GOE/AGA", field.at("/value").asText());
                    checkC++;
                    break;
                case "docTitle":
                    assertEquals("type:text", field.at("/type").asText());
                    assertEquals(
                            "Goethe: Autobiographische Schriften II, (1817-1825, 1832)",
                            field.at("/value").asText());
                    checkC++;
                    break;
                case "pubDate":
                    assertEquals("type:date", field.at("/type").asText());
                    assertEquals(1982, field.at("/value").asInt());
                    checkC++;
                    break;
            };
        };
        assertEquals(5, checkC);
    };

    @Test
    public void testSearchWithoutVersion () throws KustvaktException {
        Response response = target().path("api").path("search")
                .queryParam("q", "[orth=der]").queryParam("ql", "poliqarp")
                .request()
                .accept(MediaType.APPLICATION_JSON).get();
        assertEquals(HttpStatus.PERMANENT_REDIRECT_308, response.getStatus());
        URI location = response.getLocation();
        assertEquals("/api/v1.0/search", location.getPath());
    }

    @Test
    public void testSearchWrongVersion () throws KustvaktException {
        Response response = target().path("api").path("v0.2")
                .path("search").queryParam("q", "[orth=der]")
                .queryParam("ql", "poliqarp")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        assertEquals(HttpStatus.PERMANENT_REDIRECT_308, response.getStatus());
        URI location = response.getLocation();
        assertEquals("/api/v1.0/search", location.getPath());
    }

    @Test
    public void testSearchWithIP () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .request()
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertTrue(node.at("/collection").isMissingNode());
    }

    @Test
    public void testSearchWithAuthorizationHeader () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Wasser").queryParam("ql", "poliqarp")
                .request()
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue("test", "pwd"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .get();

        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertTrue(node.at("/collection").isMissingNode());
    }
    
    @Test
    public void testSearchPublicMetadata () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=das]").queryParam("ql", "poliqarp")
                .queryParam("access-rewrite-disabled", "true")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);

        assertTrue(node.at("/matches/0/snippet").isMissingNode());
    }
    
    @Test
    public void testSearchPublicMetadataWithCustomFields () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("fields", "author,title")
                .queryParam("access-rewrite-disabled", "true")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        
        assertTrue(node.at("/matches/0/snippet").isMissingNode());
        assertEquals("Goethe, Johann Wolfgang von",
                node.at("/matches/0/author").asText());
        assertEquals("Italienische Reise",
                node.at("/matches/0/title").asText());
//        assertEquals(3, node.at("/matches/0").size());
    }
    
    @Test
    public void testSearchPublicMetadataWithNonPublicField () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "Sonne").queryParam("ql", "poliqarp")
                .queryParam("fields", "author,title,snippet")
                .queryParam("access-rewrite-disabled", "true")
                .request()
                .get();
        assertEquals(Status.OK.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);

        assertEquals(StatusCodes.NON_PUBLIC_FIELD_IGNORED,
                node.at("/warnings/0/0").asInt());
        assertEquals("The requested non public fields are ignored",
                node.at("/warnings/0/1").asText());
        assertEquals("snippet",
                node.at("/warnings/0/2").asText());
    }
    
    @Test
    public void testSearchWithInvalidPage () throws KustvaktException {
        Response response = target().path(API_VERSION).path("search")
                .queryParam("q", "[orth=die]").queryParam("ql", "poliqarp")
                .queryParam("page", "0")
                .request()
                .get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(),
                response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.INVALID_ARGUMENT, node.at("/errors/0/0").asInt());
        assertEquals("page must start from 1",node.at("/errors/0/1").asText());
    }
    
    @Test
    public void testCloseIndex () throws IOException, KustvaktException {
        searchKrill.getStatistics(null);
        assertEquals(true, searchKrill.getIndex().isReaderOpen());

        Form form = new Form();
        form.param("token", "secret");

        Response response = target().path(API_VERSION).path("index")
                .path("close")
                .request()
                .post(Entity.form(form));

        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertEquals(false, searchKrill.getIndex().isReaderOpen());
    }
}
