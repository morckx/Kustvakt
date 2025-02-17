package de.ids_mannheim.korap.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.core.Response;

import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author margaretha, diewald
 */
@DisplayName("Statistics Controller Test")
class StatisticsControllerTest extends SpringJerseyTest {

    @Test
    @DisplayName("Test Get Statistics No Resource")
    void testGetStatisticsNoResource() throws IOException, KustvaktException {
        String corpusQuery = "corpusSigle=WPD15";
        Response response = target().path(API_VERSION).path("statistics").queryParam("corpusQuery", corpusQuery).request().get();
        assert Status.OK.getStatusCode() == response.getStatus();
        assertEquals(response.getHeaders().getFirst("X-Index-Revision"), "Wes8Bd4h1OypPqbWF5njeQ==");
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.get("documents").asInt(), 0);
        assertEquals(node.get("tokens").asInt(), 0);
    }

    @Test
    @DisplayName("Test Statistics With Cq")
    void testStatisticsWithCq() throws KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").queryParam("cq", "textType=Abhandlung & corpusSigle=GOE").request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(138180, node.at("/tokens").asInt());
        assertEquals(5687, node.at("/sentences").asInt());
        assertEquals(258, node.at("/paragraphs").asInt());
        assertTrue(node.at("/warnings").isMissingNode());
    }

    @Test
    @DisplayName("Test Statistics With Cq And Corpus Query")
    void testStatisticsWithCqAndCorpusQuery() throws KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").queryParam("cq", "textType=Abhandlung & corpusSigle=GOE").queryParam("corpusQuery", "textType=Autobiographie & corpusSigle=GOE").request().method("GET");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String query = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(query);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(138180, node.at("/tokens").asInt());
        assertEquals(5687, node.at("/sentences").asInt());
        assertEquals(258, node.at("/paragraphs").asInt());
        assertTrue(node.at("/warnings").isMissingNode());
    }

    @Test
    @DisplayName("Test Get Statistics Withcorpus Query 1")
    void testGetStatisticsWithcorpusQuery1() throws IOException, KustvaktException {
        String corpusQuery = "corpusSigle=GOE";
        Response response = target().path(API_VERSION).path("statistics").queryParam("corpusQuery", corpusQuery).request().get();
        assert Status.OK.getStatusCode() == response.getStatus();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.get("documents").asInt(), 11);
        assertEquals(node.get("tokens").asInt(), 665842);
        assertEquals(StatusCodes.DEPRECATED, node.at("/warnings/0/0").asInt());
        assertEquals(node.at("/warnings/0/1").asText(), "Parameter corpusQuery is deprecated in favor of cq.");
    }

    @Test
    @DisplayName("Test Get Statistics Withcorpus Query 2")
    void testGetStatisticsWithcorpusQuery2() throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").queryParam("corpusQuery", "creationDate since 1810").request().get();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assert Status.OK.getStatusCode() == response.getStatus();
        assertEquals(node.get("documents").asInt(), 7);
        assertEquals(node.get("tokens").asInt(), 279402);
        assertEquals(node.get("sentences").asInt(), 11047);
        assertEquals(node.get("paragraphs").asInt(), 489);
    }

    @Test
    @DisplayName("Test Get Statistics With Wrongcorpus Query")
    void testGetStatisticsWithWrongcorpusQuery() throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").queryParam("corpusQuery", "creationDate geq 1810").request().get();
        assert Status.BAD_REQUEST.getStatusCode() == response.getStatus();
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/errors/0/0").asInt(), 302);
        assertEquals(node.at("/errors/0/1").asText(), "Could not parse query >>> (creationDate geq 1810) <<<.");
        assertEquals(node.at("/errors/0/2").asText(), "(creationDate geq 1810)");
    }

    @Test
    @DisplayName("Test Get Statistics With Wrongcorpus Query 2")
    void testGetStatisticsWithWrongcorpusQuery2() throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").queryParam("corpusQuery", "creationDate >= 1810").request().get();
        String ent = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/errors/0/0").asInt(), 305);
        assertEquals(node.at("/errors/0/1").asText(), "Operator >= is not acceptable.");
        assertEquals(node.at("/errors/0/2").asText(), ">=");
    }

    @Test
    @DisplayName("Test Get Statistics Withoutcorpus Query")
    void testGetStatisticsWithoutcorpusQuery() throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(11, node.at("/documents").asInt());
        assertEquals(665842, node.at("/tokens").asInt());
        assertEquals(25074, node.at("/sentences").asInt());
        assertEquals(772, node.at("/paragraphs").asInt());
    }

    @Test
    @DisplayName("Test Get Statistics With Koral Query")
    void testGetStatisticsWithKoralQuery() throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").request().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).post(Entity.json("{ \"collection\" : {\"@type\": " + "\"koral:doc\", \"key\": \"availability\", \"match\": " + "\"match:eq\", \"type\": \"type:regex\", \"value\": " + "\"CC-BY.*\"} }"));
        assertEquals(response.getHeaders().getFirst("X-Index-Revision"), "Wes8Bd4h1OypPqbWF5njeQ==");
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(2, node.at("/documents").asInt());
        assertEquals(72770, node.at("/tokens").asInt());
        assertEquals(2985, node.at("/sentences").asInt());
        assertEquals(128, node.at("/paragraphs").asInt());
    }

    @Test
    @DisplayName("Test Get Statistics With Empty Collection")
    void testGetStatisticsWithEmptyCollection() throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").request().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).post(Entity.json("{}"));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(node.at("/errors/0/0").asInt(), de.ids_mannheim.korap.util.StatusCodes.MISSING_COLLECTION);
        assertEquals(node.at("/errors/0/1").asText(), "Collection is not found");
    }

    @Test
    @DisplayName("Test Get Statistics With Incorrect Json")
    void testGetStatisticsWithIncorrectJson() throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").request().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).post(Entity.json("{ \"collection\" : }"));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String ent = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED, node.at("/errors/0/0").asInt());
        assertEquals(node.at("/errors/0/1").asText(), "Failed deserializing json object: { \"collection\" : }");
    }

    @Test
    @DisplayName("Test Get Statistics Without Koral Query")
    void testGetStatisticsWithoutKoralQuery() throws IOException, KustvaktException {
        Response response = target().path(API_VERSION).path("statistics").request().post(Entity.json(""));
        String ent = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(ent);
        assertEquals(11, node.at("/documents").asInt());
        assertEquals(665842, node.at("/tokens").asInt());
        assertEquals(25074, node.at("/sentences").asInt());
        assertEquals(772, node.at("/paragraphs").asInt());
    }
}
