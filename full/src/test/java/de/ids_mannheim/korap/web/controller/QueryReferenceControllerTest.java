package de.ids_mannheim.korap.web.controller;

import static org.junit.Assert.assertEquals;

import org.apache.http.entity.ContentType;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;

import de.ids_mannheim.korap.authentication.http.HttpAuthorizationHandler;
import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

public class QueryReferenceControllerTest extends SpringJerseyTest{

    private String testUser = "qRefControllerTest";
    
    @Test
    public void testCreatePrivateQuery () throws KustvaktException {
        String json = "{\"type\": \"PRIVATE\""
                + ",\"queryType\": \"QUERY\""
                + ",\"queryLanguage\": \"poliqarp\""
                + ",\"query\": \"der\"}";
        
        String qName="new_query";
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~"+testUser).path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(testUser, "pass"))
                .header(HttpHeaders.X_FORWARDED_FOR, "149.27.0.32")
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON)
                .put(ClientResponse.class, json);

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        JsonNode node = testRetrieveQueryByName(testUser, testUser, qName);
        System.out.println(node);
    }
    
    
    private JsonNode testRetrieveQueryByName (String username, String qCreator,
            String qName) throws UniformInterfaceException,
            ClientHandlerException, KustvaktException {
        ClientResponse response = resource().path(API_VERSION).path("query")
                .path("~" + qCreator).path(qName)
                .header(Attributes.AUTHORIZATION, HttpAuthorizationHandler
                        .createBasicAuthorizationHeaderValue(username, "pass"))
                .get(ClientResponse.class);
        String entity = response.getEntity(String.class);
//         System.out.println(entity);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        return JsonUtils.readTree(entity);
    }
    
}
