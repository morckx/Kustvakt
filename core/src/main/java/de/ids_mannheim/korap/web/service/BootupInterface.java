package de.ids_mannheim.korap.web.service;

import de.ids_mannheim.korap.exceptions.KustvaktException;

/**
 * @author hanl
 * @date 12/01/2016
 */
@Deprecated
public interface BootupInterface {

    void load () throws KustvaktException;


    Class<? extends BootupInterface>[] getDependencies ();

}
