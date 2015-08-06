import de.ids_mannheim.korap.config.BeanConfiguration;
import de.ids_mannheim.korap.config.PluginHandler;
import de.ids_mannheim.korap.interfaces.AuditingIface;
import de.ids_mannheim.korap.interfaces.defaults.DefaultAuditing;
import org.junit.Test;

/**
 * @author hanl
 * @date 27/07/2015
 */
public class ClassLoaderTest {

    @Test
    public void testBeanConfigurationLoaderThrowsNoException() {
        BeanConfiguration.loadClasspathContext("classpath-config.xml");
        assert BeanConfiguration.getBeans() != null;
    }

    @Test
    public void testDefaultCreationThrowsNoException() {
        PluginHandler pl = new PluginHandler();
        Object o = pl.getDefault(BeanConfiguration.KUSTVAKT_AUDITING);
        assert o != null;
    }

    @Test(expected = RuntimeException.class)
    public void testDefaultCreationThrowsException() {
        BeanConfiguration.loadClasspathContext();
        BeanConfiguration.getBeans().getAuthenticationManager();
    }

    @Test
    public void testDefaultInterfaceMatchThrowsNoException() {
        BeanConfiguration.loadClasspathContext();
        AuditingIface iface = BeanConfiguration.getBeans()
                .getAuditingProvider();
        assert iface != null;
        assert iface instanceof DefaultAuditing;
    }

}
