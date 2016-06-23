package de.ids_mannheim.korap.security.auth;

import de.ids_mannheim.korap.config.Attributes;
import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.config.KustvaktBaseDaoInterface;
import de.ids_mannheim.korap.config.KustvaktConfiguration;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.interfaces.db.EntityHandlerIface;
import de.ids_mannheim.korap.user.*;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author hanl
 * @date 04/02/2016
 */
public class KustvaktAuthenticationManagerTest extends BeanConfigTest {

    @After
    public void after () {
        try {
            User user = helper()
                    .getContext()
                    .getAuthenticationManager()
                    .getUser(
                            (String) KustvaktConfiguration.KUSTVAKT_USER
                                    .get(Attributes.USERNAME));
            helper().getContext().getAuthenticationManager()
                    .deleteAccount(user);
        }
        catch (KustvaktException e) {}
    }


    @Test
    public void testCreateUser () throws KustvaktException {
        User user = helper().getContext().getAuthenticationManager()
                .createUserAccount(KustvaktConfiguration.KUSTVAKT_USER, false);

        EntityHandlerIface dao = helper().getContext().getUserDBHandler();
        assertNotEquals(0, ((KustvaktBaseDaoInterface) dao).size());
        User check = dao.getAccount(user.getUsername());
        assertNotNull(check);
    }


    @Test
    public void testBatchStore () {
        int i = 6;

        AuthenticationManagerIface manager = helper().getContext()
                .getAuthenticationManager();
        for (int ix = 0; ix < i; ix++) {}

    }


    @Test
    public void testUserdetailsGet () throws KustvaktException {
        testCreateUser();
        AuthenticationManagerIface manager = helper().getContext()
                .getAuthenticationManager();

        User user = manager
                .getUser((String) KustvaktConfiguration.KUSTVAKT_USER
                        .get(Attributes.USERNAME));

        Userdata data = manager.getUserData(user, UserDetails.class);
        assertNotNull(data);
    }


    @Test
    public void testUsersettingsGet () throws KustvaktException {
        testCreateUser();
        AuthenticationManagerIface manager = helper().getContext()
                .getAuthenticationManager();

        User user = manager
                .getUser((String) KustvaktConfiguration.KUSTVAKT_USER
                        .get(Attributes.USERNAME));

        Userdata data = manager.getUserData(user, UserSettings.class);
        assertNotNull(data);
    }


    @Test(expected = KustvaktException.class)
    public void testUserDetailsGetNonExistent () throws KustvaktException {
        testCreateUser();
        AuthenticationManagerIface manager = helper().getContext()
                .getAuthenticationManager();

        User user = new KorAPUser(10, "random");
        manager.getUserData(user, UserDetails.class);
    }


    @Test(expected = KustvaktException.class)
    public void testUserSettingsGetNonExistent () throws KustvaktException {
        testCreateUser();
        AuthenticationManagerIface manager = helper().getContext()
                .getAuthenticationManager();

        User user = new KorAPUser(10, "random");
        manager.getUserData(user, UserSettings.class);
    }

    @Test
    @Ignore
    public void testUserUpdate() throws KustvaktException {
        testCreateUser();
        AuthenticationManagerIface manager = helper().getContext()
                .getAuthenticationManager();
        // todo:
    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
    }
}
