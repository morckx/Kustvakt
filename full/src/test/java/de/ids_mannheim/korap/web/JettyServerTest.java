package de.ids_mannheim.korap.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author margaretha
 */
@DisplayName("Jetty Server Test")
class JettyServerTest {

    @BeforeAll
    static void testServerStarts() throws Exception {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8000);
        server.setConnectors(new Connector[]{connector});
        HandlerList handlers = new HandlerList();
        handlers.addHandler(new ShutdownHandler("secret", false, true));
        server.setHandler(handlers);
        server.start();
    }

    @Test
    @DisplayName("Test Shutdown")
    void testShutdown() throws IOException {
        URL url = new URL("http://localhost:8000/shutdown?token=secret");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        assertEquals(200, connection.getResponseCode());
    }
}
