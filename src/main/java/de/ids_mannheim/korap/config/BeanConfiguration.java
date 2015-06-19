package de.ids_mannheim.korap.config;

import de.ids_mannheim.korap.interfaces.AuditingIface;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * User: hanl
 * Date: 10/9/13
 * Time: 11:20 AM
 */
public class BeanConfiguration {

    private static final String config_file = "default-config.xml";

    private static ApplicationContext context = null;

    public static void loadClasspathContext(String... files) {
        if (context == null && files == null)
            context = new ClassPathXmlApplicationContext(config_file);
        else if (context == null)
            context = new ClassPathXmlApplicationContext(files);
    }

    public static void loadFileContext(String filepath) {
        if (context == null)
            context = new FileSystemXmlApplicationContext("file:" + filepath);
    }

    public static <T extends KustvaktConfiguration> T getConfiguration() {
        return (T) getBean("config");
    }

    public static <T extends KustvaktConfiguration> T getConfiguration(
            Class<T> clazz) {
        return getBean(clazz);
    }

    public static boolean hasContext() {
        return context != null;
    }

    protected static <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    protected static <T> T getBean(String name) {
        return (T) context.getBean(name);
    }

    public static AuditingIface getAuditingProvider() {
        return (AuditingIface) context.getBean("auditingProvider");
    }
}
