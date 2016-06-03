package de.ids_mannheim.korap.web;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import de.ids_mannheim.korap.config.*;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.web.service.BootableBeanInterface;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author hanl
 * @date 01/06/2015
 */
public class KustvaktBaseServer {

    public static void main (String[] args) throws Exception {
        KustvaktConfiguration.loadLog4jLogger();
        KustvaktBaseServer server = new KustvaktBaseServer();
        KustvaktArgs kargs = server.readAttributes(args);

        if (kargs.config != null)
            BeansFactory.loadFileContext(kargs.config);
        else
            BeansFactory.loadClasspathContext();

        KustvaktCacheManager.init();

        kargs.setRootPackages(new String[] { "de.ids_mannheim.korap.web.service.light" });

        server.startServer(kargs);
    }


    protected KustvaktArgs readAttributes (String[] args) {
        KustvaktArgs kargs = new KustvaktArgs();
        for (int i = 0; i < args.length; i++) {
            switch ((args[i])) {
                case "--debug":
                    kargs.setDebug(true);
                    break;
                case "--config":
                    kargs.setConfig(args[i + 1]);
                    break;
                case "--port":
                    kargs.setPort(Integer.valueOf(args[i + 1]));
                    break;
                case "--help":
                    StringBuffer b = new StringBuffer();

                    b.append("Parameter description: \n")
                            .append("--config  <Path to spring configuration file> : Configuration file\n")
                            .append("--port  <Server port> : Port under which the server is accessible \n")
                            //                            .append("--props  <Path to kustvakt properties> : list of configuration properties\n")
                            .append("--help : This help menu\n");
                    System.out.println(b.toString());
                    System.out.println();
                    break;
                case "--init":
                    kargs.init = true;
                    break;
            }
        }
        return kargs;
    }


    public void runPreStart () {
        Set<Class<? extends BootableBeanInterface>> set = KustvaktClassLoader
                .loadSubTypes(BootableBeanInterface.class);

        List<BootableBeanInterface> list = new ArrayList<>(set.size());
        for (Class cl : set) {
            BootableBeanInterface iface;
            try {
                iface = (BootableBeanInterface) cl.newInstance();
                list.add(iface);
            }
            catch (InstantiationException | IllegalAccessException e) {
                continue;
            }
        }
        System.out.println("Found boot loading interfaces: " + list);
        int track = list.size();
        while (!list.isEmpty()) {
            for (BootableBeanInterface iface : new ArrayList<>(list)) {
                try {
                    iface.load(BeansFactory.getKustvaktContext());
                }
                catch (KustvaktException e) {
                    // don't do anything!
                    System.out.println("An error occurred in class "
                            + iface.getClass().getSimpleName() + "!\n" + e);
                    continue;
                }
                list.remove(iface);
            }
            if (!list.isEmpty()) {
                System.out.println("Following bootup classes raised errors: "
                        + list);
                break;
            }
        }
        AdminSetup.getInstance();
    }


    protected void startServer (KustvaktArgs kargs) {
        if (kargs.init)
            runPreStart();

        if (kargs.port == -1)
            kargs.setPort(BeansFactory.getKustvaktContext().getConfiguration()
                    .getPort());

        System.out.println("Starting Kustvakt Service on port '" + kargs.port
                + "'");
        try {
            // from http://wiki.eclipse.org/Jetty/Tutorial/Embedding_Jetty
            Server server = new Server();
            ServletContextHandler contextHandler = new ServletContextHandler(
                    ServletContextHandler.NO_SESSIONS);
            contextHandler.setContextPath("/");

            SocketConnector connector = new SocketConnector();
            connector.setPort(kargs.port);
            connector.setMaxIdleTime(60000);

            // http://stackoverflow.com/questions/9670363/how-do-i-programmatically-configure-jersey-to-use-jackson-for-json-deserializa
            final ResourceConfig rc = new PackagesResourceConfig(
                    kargs.rootPackages);

            // from http://stackoverflow.com/questions/7421574/embedded-jetty-with-jersey-or-resteasy
            contextHandler.addServlet(new ServletHolder(
                    new ServletContainer(rc)), "/api/*");

            server.setHandler(contextHandler);

            if (kargs.sslContext != null) {
                SslSocketConnector sslConnector = new SslSocketConnector(
                        kargs.sslContext);
                sslConnector.setPort(8443);
                sslConnector.setMaxIdleTime(60000);
                server.setConnectors(new Connector[] { connector, sslConnector });
            }
            else
                server.setConnectors(new Connector[] { connector });

            server.start();
            server.join();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Setter
    public static class KustvaktArgs {

        private boolean debug;
        @Getter
        private String config;
        private int port;
        private SslContextFactory sslContext;
        private String[] rootPackages;
        private boolean init;


        public KustvaktArgs () {
            this.port = -1;
            this.sslContext = null;
            this.debug = false;
            this.config = null;
            this.init = false;
        }

    }
}