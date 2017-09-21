package de.ids_mannheim.korap.security;

import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import de.ids_mannheim.korap.config.BeanConfigTest;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.security.ac.PolicyBuilder;

/**
 * @author hanl
 * @date 20/11/2015
 */
public class PolicyBuilderTest extends BeanConfigTest {

    @Test
    public void testBuild () {
        PolicyBuilder b = new PolicyBuilder(helper().getUser());

    }


    @Override
    public void initMethod () throws KustvaktException {
        helper().setupAccount();
    }


    @Test
    public void testCreateForMultipleResources () {
        //todo:
    }
}
