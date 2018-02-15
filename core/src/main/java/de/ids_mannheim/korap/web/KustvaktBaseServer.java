package de.ids_mannheim.korap.web;

import javax.naming.InitialContext;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoaderListener;

import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

import de.ids_mannheim.korap.config.KustvaktConfiguration;
import lombok.Getter;
import lombok.Setter;

/**
 * @author hanl
 * @date 01/06/2015
 */
@Component
public abstract class KustvaktBaseServer {

    protected static KustvaktConfiguration config;
    
    protected static String rootPackages;
    protected static KustvaktArgs kargs;

    public KustvaktBaseServer () {
        KustvaktConfiguration.loadLogger();
    }


    protected KustvaktArgs readAttributes (String[] args) {
        KustvaktArgs kargs = new KustvaktArgs();
        for (int i = 0; i < args.length; i++) {
            switch ((args[i])) {
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

    protected void setupJndi (Server server, WebAppContext webapp){}

    protected void start(){
        
        if (kargs.port == -1){
            kargs.setPort(config.getPort());
        }

        Server server = new Server();
        
        ServletContextHandler contextHandler = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.setInitParameter("contextConfigLocation",
                "classpath:" + kargs.getConfig());
        
        ServletContextListener listener = new ContextLoaderListener();
        contextHandler.addEventListener(listener);

        ServletHolder servletHolder = new ServletHolder(new SpringServlet());
        servletHolder.setInitParameter(
                "com.sun.jersey.config.property.packages", rootPackages);
        servletHolder.setInitOrder(1);
        contextHandler.addServlet(servletHolder, config.getBaseURL());

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(kargs.port);
        connector.setIdleTimeout(60000);
        
        server.setHandler(contextHandler);
        
//        ClassList classlist = ClassList.setServerDefault(server);
//        classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration",
//                "org.eclipse.jetty.plus.webapp.EnvConfiguration",
//                "org.eclipse.jetty.plus.webapp.PlusConfiguration");
        
//        WebAppContext webapp = new WebAppContext();
//        webapp.setDescriptor(KustvaktBaseServer.class.getResource("/WEB-INF/web.xml").toString());
//        webapp.setContextPath("/");
//        webapp.setWar("./kustvakt.war");
//        server.setHandler(webapp);
//        setupJndi(server, webapp);
//        HandlerList handlerList = new HandlerList();
//        handlerList.setHandlers(new Handler[]{webapp,contextHandler});
//        server.setHandler(handlerList);
        
        server.setConnectors(new Connector[] { connector });
        try {
            server.start();
//            InitialContext ic = new InitialContext();
//            Object object = ic.lookup("mail/Session");
            server.join();
        }
        catch (Exception e) {
            System.out.println("Server could not be started!");
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Setter
    public static class KustvaktArgs {

        @Getter
        private String config;
        private int port;
        private String[] rootPackages;
        private boolean init;


        public KustvaktArgs () {
            this.port = -1;
            this.config = null;
            this.init = false;
        }

    }
}