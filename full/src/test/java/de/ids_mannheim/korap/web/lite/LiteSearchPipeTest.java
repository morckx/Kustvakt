package de.ids_mannheim.korap.web.lite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.ids_mannheim.korap.config.LiteJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Lite Search Pipe Test")
class LiteSearchPipeTest extends LiteJerseyTest {

    private ClientAndServer mockServer;

    private MockServerClient mockClient;

    private int port = 6070;

    private String pipeJson, pipeWithParamJson;

    private String glemmUri = "http://localhost:" + port + "/glemm";

    public LiteSearchPipeTest() throws IOException {
        pipeJson = IOUtils.toString(ClassLoader.getSystemResourceAsStream("pipe-output/test-pipes.jsonld"), StandardCharsets.UTF_8);
        pipeWithParamJson = IOUtils.toString(ClassLoader.getSystemResourceAsStream("pipe-output/with-param.jsonld"), StandardCharsets.UTF_8);
    }

    @BeforeEach
    void startMockServer() {
        mockServer = startClientAndServer(port);
        mockClient = new MockServerClient("localhost", mockServer.getPort());
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop();
    }

    @Test
    @DisplayName("Test Mock Server")
    void testMockServer() throws IOException {
        mockClient.reset().when(request().withMethod("POST").withPath("/test").withHeader(new Header("Content-Type", "application/json; charset=utf-8"))).respond(response().withHeader(new Header("Content-Type", "application/json; charset=utf-8")).withBody("{test}").withStatusCode(200));
        URL url = new URL("http://localhost:" + port + "/test");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        String json = "{\"name\" : \"dory\"}";
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = json.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        assertEquals(200, connection.getResponseCode());
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
        assertEquals(br.readLine(), "{test}");
    }

    @Test
    @DisplayName("Test Search With Pipes")
    void testSearchWithPipes() throws IOException, KustvaktException, URISyntaxException {
        mockClient.reset().when(request().withMethod("POST").withPath("/glemm").withHeaders(new Header("Content-Type", "application/json; charset=utf-8"), new Header("Accept", "application/json"))).respond(response().withHeader(new Header("Content-Type", "application/json; charset=utf-8")).withBody(pipeJson).withStatusCode(200));
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", glemmUri).request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/query/wrap/key").size());
        node = node.at("/query/wrap/rewrites");
        assertEquals(2, node.size());
        assertEquals(node.at("/0/src").asText(), "Glemm");
        assertEquals(node.at("/0/operation").asText(), "operation:override");
        assertEquals(node.at("/0/scope").asText(), "key");
        assertEquals(node.at("/1/src").asText(), "Kustvakt");
        assertEquals(node.at("/1/operation").asText(), "operation:injection");
        assertEquals(node.at("/1/scope").asText(), "foundry");
    }

    @Test
    @DisplayName("Test Search With Url Encoded Pipes")
    void testSearchWithUrlEncodedPipes() throws IOException, KustvaktException {
        mockClient.reset().when(request().withMethod("POST").withPath("/glemm").withHeaders(new Header("Content-Type", "application/json; charset=utf-8"), new Header("Accept", "application/json"))).respond(response().withHeader(new Header("Content-Type", "application/json; charset=utf-8")).withBody(pipeJson).withStatusCode(200));
        glemmUri = URLEncoder.encode(glemmUri, "utf-8");
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", glemmUri).request().get();
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/query/wrap/key").size());
    }

    @Test
    @DisplayName("Test Search With Multiple Pipes")
    void testSearchWithMultiplePipes() throws KustvaktException {
        mockClient.reset().when(request().withMethod("POST").withPath("/glemm").withQueryStringParameter("param").withHeaders(new Header("Content-Type", "application/json; charset=utf-8"), new Header("Accept", "application/json"))).respond(response().withHeader(new Header("Content-Type", "application/json; charset=utf-8")).withBody(pipeWithParamJson).withStatusCode(200));
        String glemmUri2 = glemmUri + "?param=blah";
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", glemmUri + "," + glemmUri2).request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(3, node.at("/query/wrap/key").size());
    }

    @Test
    @DisplayName("Test Search With Unknown URL")
    void testSearchWithUnknownURL() throws IOException, KustvaktException {
        String url = target().getUri().toString() + API_VERSION + "/test/tralala";
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", url).request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
        assertEquals(node.at("/warnings/0/3").asText(), "404 Not Found");
    }

    @Test
    @DisplayName("Test Search With Unknown Host")
    void testSearchWithUnknownHost() throws KustvaktException {
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", "http://glemm").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
        assertEquals(node.at("/warnings/0/3").asText(), "glemm");
    }

    @Test
    @DisplayName("Test Search Unsupported Media Type")
    void testSearchUnsupportedMediaType() throws KustvaktException {
        mockClient.reset().when(request().withMethod("POST").withPath("/non-json-pipe")).respond(response().withStatusCode(415));
        String pipeUri = "http://localhost:" + port + "/non-json-pipe";
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", pipeUri).request().get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
        assertEquals(node.at("/warnings/0/3").asText(), "415 Unsupported Media Type");
    }

    @Test
    @DisplayName("Test Search With Multiple Pipe Warnings")
    void testSearchWithMultiplePipeWarnings() throws KustvaktException {
        String url = target().getUri().toString() + API_VERSION + "/test/tralala";
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", url + "," + "http://glemm").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/warnings").size());
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
        assertEquals(url, node.at("/warnings/0/2").asText());
        assertEquals(node.at("/warnings/0/3").asText(), "404 Not Found");
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/1/0").asInt());
        assertEquals(node.at("/warnings/1/2").asText(), "http://glemm");
        assertEquals(node.at("/warnings/1/3").asText(), "glemm");
    }

    @Test
    @DisplayName("Test Search With Invalid Json Response")
    void testSearchWithInvalidJsonResponse() throws KustvaktException {
        mockClient.reset().when(request().withMethod("POST").withPath("/invalid-response").withHeaders(new Header("Content-Type", "application/json; charset=utf-8"), new Header("Accept", "application/json"))).respond(response().withBody("{blah:}").withStatusCode(200).withHeaders(new Header("Content-Type", "application/json; charset=utf-8")));
        String pipeUri = "http://localhost:" + port + "/invalid-response";
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", pipeUri).request().get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED, node.at("/errors/0/0").asInt());
    }

    @Test
    @DisplayName("Test Search With Plain Text Response")
    void testSearchWithPlainTextResponse() throws KustvaktException {
        mockClient.reset().when(request().withMethod("POST").withPath("/plain-text").withHeaders(new Header("Content-Type", "application/json; charset=utf-8"), new Header("Accept", "application/json"))).respond(response().withBody("blah").withStatusCode(200));
        String pipeUri = "http://localhost:" + port + "/plain-text";
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", pipeUri).request().get();
        String entity = response.readEntity(String.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.DESERIALIZATION_FAILED, node.at("/errors/0/0").asInt());
    }

    @Test
    @DisplayName("Test Search With Multiple And Unknown Pipes")
    void testSearchWithMultipleAndUnknownPipes() throws KustvaktException {
        mockClient.reset().when(request().withMethod("POST").withPath("/glemm").withHeaders(new Header("Content-Type", "application/json; charset=utf-8"), new Header("Accept", "application/json"))).respond(response().withHeader(new Header("Content-Type", "application/json; charset=utf-8")).withBody(pipeJson).withStatusCode(200));
        Response response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", "http://unknown" + "," + glemmUri).request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        JsonNode node = JsonUtils.readTree(entity);
        assertEquals(2, node.at("/query/wrap/key").size());
        assertTrue(node.at("/warnings").isMissingNode());
        response = target().path(API_VERSION).path("search").queryParam("q", "[orth=der]").queryParam("ql", "poliqarp").queryParam("pipes", glemmUri + ",http://unknown").request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        entity = response.readEntity(String.class);
        node = JsonUtils.readTree(entity);
        assertEquals(StatusCodes.PIPE_FAILED, node.at("/warnings/0/0").asInt());
    }
}
