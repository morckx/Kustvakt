package de.ids_mannheim.korap.entity;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import de.ids_mannheim.korap.constants.GroupMemberStatus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import lombok.Getter;
import lombok.Setter;

/** Describes members of user groups.
 * 
 *  @author margaretha
 *  @see UserGroup
 *  @see Role
 */
@Setter
@Getter
@Entity
@Table(name = "user_group_member",
        indexes = { @Index(unique = true, columnList = "user_id, group_id") })
public class UserGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "deleted_by")
    private String deletedBy;
    
    @Enumerated(EnumType.STRING)
    private GroupMemberStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private UserGroup group;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "group_member_role",
            joinColumns = @JoinColumn(name = "group_member_id",
                    referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id",
                    referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = { "group_member_id", "role_id" }))
    private Set<Role> roles;


    public UserGroupMember createMember (String userId, String createdBy,
            UserGroup userGroup, Set<Role> roles) throws KustvaktException {
//        checkStringValue(userId, "userId");
//        checkStringValue(createdBy, "createdBy");
        
        UserGroupMember member = new UserGroupMember();
        member.setUserId(userId);
        member.setCreatedBy(createdBy);
        member.setGroup(userGroup);
        member.setStatus(GroupMemberStatus.PENDING);
        member.setRoles(roles);
        return member;
    }
    
    @Override
    public String toString () {
        return "id=" + id + ", group= " + group + ", userId= " + userId
                + ", createdBy= " + createdBy + ", deletedBy= " + deletedBy
                + ", roles=" + roles;
    }
}
