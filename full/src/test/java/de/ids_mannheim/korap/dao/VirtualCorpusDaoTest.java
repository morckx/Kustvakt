package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Iterator;
import java.util.List;
import javax.persistence.PersistenceException;

import org.hibernate.exception.GenericJDBCException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import de.ids_mannheim.korap.config.SpringJerseyTest;
import de.ids_mannheim.korap.constant.QueryType;
import de.ids_mannheim.korap.constant.ResourceType;
import de.ids_mannheim.korap.entity.QueryDO;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.user.User;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Virtual Corpus Dao Test")
class VirtualCorpusDaoTest extends SpringJerseyTest {

    @Autowired
    private QueryDao dao;

    @Test
    @DisplayName("Test List VC By Type")
    void testListVCByType() throws KustvaktException {
        List<QueryDO> vcList = dao.retrieveQueryByType(ResourceType.PUBLISHED, null, QueryType.VIRTUAL_CORPUS);
        assertEquals(1, vcList.size());
        QueryDO vc = vcList.get(0);
        assertEquals(4, vc.getId());
        assertEquals(vc.getName(), "published-vc");
        assertEquals(vc.getCreatedBy(), "marlin");
    }

    @Test
    @DisplayName("Test System VC")
    void testSystemVC() throws KustvaktException {
        // insert vc
        int id = dao.createQuery("system-vc", ResourceType.SYSTEM, QueryType.VIRTUAL_CORPUS, User.CorpusAccess.FREE, "corpusSigle=GOE", "definition", "description", "experimental", false, "test class", null, null);
        // select vc
        List<QueryDO> vcList = dao.retrieveQueryByType(ResourceType.SYSTEM, null, QueryType.VIRTUAL_CORPUS);
        assertEquals(2, vcList.size());
        QueryDO vc = dao.retrieveQueryById(id);
        // delete vc
        dao.deleteQuery(vc);
        // check if vc has been deleted
        KustvaktException exception = assertThrows(KustvaktException.class, () -> {
            dao.retrieveQueryById(id);
        });
        assertEquals(StatusCodes.NO_RESOURCE_FOUND, exception.getStatusCode().intValue());
    }

    @Test
    @DisplayName("Test Non Unique VC")
    void testNonUniqueVC() throws KustvaktException {
        PersistenceException exception = assertThrows(PersistenceException.class, () -> {
            dao.createQuery("system-vc", ResourceType.SYSTEM, QueryType.VIRTUAL_CORPUS, User.CorpusAccess.FREE, "corpusSigle=GOE", "definition", "description", "experimental", false, "system", null, null);
        });
        assertEquals(exception.getMessage(), "org.hibernate.exception.GenericJDBCException: could not execute statement");
    }

    @Test
    @DisplayName("Retrieve System VC")
    void retrieveSystemVC() throws KustvaktException {
        List<QueryDO> vc = dao.retrieveQueryByType(ResourceType.SYSTEM, null, QueryType.VIRTUAL_CORPUS);
        assertEquals(1, vc.size());
    }

    /**
     * retrieve private and group VC
     *
     * @throws KustvaktException
     */
    @Test
    @DisplayName("Retrieve VC By User Dory")
    void retrieveVCByUserDory() throws KustvaktException {
        List<QueryDO> virtualCorpora = dao.retrieveQueryByUser("dory", QueryType.VIRTUAL_CORPUS);
        // System.out.println(virtualCorpora);
        assertEquals(4, virtualCorpora.size());
        // ordered by id
        Iterator<QueryDO> i = virtualCorpora.iterator();
        assertEquals(i.next().getName(), "dory-vc");
        assertEquals(i.next().getName(), "group-vc");
        assertEquals(i.next().getName(), "system-vc");
        assertEquals(i.next().getName(), "published-vc");
    }

    /**
     * retrieves group VC and
     * excludes hidden published VC (user has never used it)
     *
     * @throws KustvaktException
     */
    @Test
    @DisplayName("Retrieve VC By User Nemo")
    void retrieveVCByUserNemo() throws KustvaktException {
        List<QueryDO> virtualCorpora = dao.retrieveQueryByUser("nemo", QueryType.VIRTUAL_CORPUS);
        assertEquals(3, virtualCorpora.size());
        Iterator<QueryDO> i = virtualCorpora.iterator();
        assertEquals(i.next().getName(), "group-vc");
        assertEquals(i.next().getName(), "system-vc");
        assertEquals(i.next().getName(), "nemo-vc");
    }

    /**
     * retrieves published VC by the owner and
     * excludes group vc when a user is a pending member
     *
     * @throws KustvaktException
     */
    @Test
    @DisplayName("Retrieve VC By User Marlin")
    void retrieveVCByUserMarlin() throws KustvaktException {
        List<QueryDO> virtualCorpora = dao.retrieveQueryByUser("marlin", QueryType.VIRTUAL_CORPUS);
        assertEquals(3, virtualCorpora.size());
        Iterator<QueryDO> i = virtualCorpora.iterator();
        assertEquals(i.next().getName(), "system-vc");
        assertEquals(i.next().getName(), "published-vc");
        assertEquals(i.next().getName(), "marlin-vc");
    }

    /**
     * retrieves published VC from an auto-generated hidden group and
     * excludes group vc when a user is a deleted member
     *
     * @throws KustvaktException
     */
    @Test
    @DisplayName("Retrieve VC By User Pearl")
    void retrieveVCByUserPearl() throws KustvaktException {
        List<QueryDO> virtualCorpora = dao.retrieveQueryByUser("pearl", QueryType.VIRTUAL_CORPUS);
        assertEquals(2, virtualCorpora.size());
        Iterator<QueryDO> i = virtualCorpora.iterator();
        assertEquals(i.next().getName(), "system-vc");
        assertEquals(i.next().getName(), "published-vc");
    }
}
