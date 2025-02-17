package de.ids_mannheim.korap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import de.ids_mannheim.korap.constant.PredefinedRole;
import de.ids_mannheim.korap.constant.PrivilegeType;
import de.ids_mannheim.korap.entity.Privilege;
import de.ids_mannheim.korap.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-config.xml")
@DisplayName("Role Privilege Dao Test")
class RolePrivilegeDaoTest {

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private PrivilegeDao privilegeDao;

    @Test
    @DisplayName("Retrieve Predefined Role")
    void retrievePredefinedRole() {
        Role r = roleDao.retrieveRoleById(PredefinedRole.USER_GROUP_ADMIN.getId());
        assertEquals(1, r.getId());
    }

    @Test
    @DisplayName("Create Delete Role")
    void createDeleteRole() {
        String roleName = "vc editor";
        List<PrivilegeType> privileges = new ArrayList<PrivilegeType>();
        privileges.add(PrivilegeType.READ);
        privileges.add(PrivilegeType.WRITE);
        roleDao.createRole(roleName, privileges);
        Role r = roleDao.retrieveRoleByName(roleName);
        assertEquals(roleName, r.getName());
        assertEquals(2, r.getPrivileges().size());
        roleDao.deleteRole(r.getId());
    }

    @Test
    @DisplayName("Update Role")
    void updateRole() {
        Role role = roleDao.retrieveRoleByName("USER_GROUP_MEMBER");
        roleDao.editRoleName(role.getId(), "USER_GROUP_MEMBER role");
        role = roleDao.retrieveRoleById(role.getId());
        assertEquals(role.getName(), "USER_GROUP_MEMBER role");
        roleDao.editRoleName(role.getId(), "USER_GROUP_MEMBER");
        role = roleDao.retrieveRoleById(role.getId());
        assertEquals(role.getName(), "USER_GROUP_MEMBER");
    }

    @Test
    @DisplayName("Add Delete Privilege Of Existing Role")
    void addDeletePrivilegeOfExistingRole() {
        Role role = roleDao.retrieveRoleByName("USER_GROUP_MEMBER");
        List<Privilege> privileges = role.getPrivileges();
        assertEquals(1, role.getPrivileges().size());
        assertEquals(privileges.get(0).getName(), PrivilegeType.DELETE);
        // add privilege
        List<PrivilegeType> privilegeTypes = new ArrayList<PrivilegeType>();
        privilegeTypes.add(PrivilegeType.READ);
        privilegeDao.addPrivilegesToRole(role, privilegeTypes);
        role = roleDao.retrieveRoleByName("USER_GROUP_MEMBER");
        assertEquals(2, role.getPrivileges().size());
        // delete privilege
        privilegeDao.deletePrivilegeFromRole(role.getId(), PrivilegeType.READ);
        role = roleDao.retrieveRoleByName("USER_GROUP_MEMBER");
        assertEquals(1, role.getPrivileges().size());
        assertEquals(privileges.get(0).getName(), PrivilegeType.DELETE);
    }
}
