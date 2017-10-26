package de.ids_mannheim.korap.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroupMember;
import de.ids_mannheim.korap.entity.UserGroupMember_;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.ParameterChecker;

@Transactional
@Repository
public class UserGroupMemberDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void addMember (UserGroupMember member) throws KustvaktException {
        ParameterChecker.checkObjectValue(member, "userGroupMember");
        entityManager.persist(member);
    }

    public void addMembers (List<UserGroupMember> members)
            throws KustvaktException {
        ParameterChecker.checkObjectValue(members, "List<UserGroupMember>");

        for (UserGroupMember member : members) {
            addMember(member);
        }
    }

    public void approveMember (String userId, int groupId)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        UserGroupMember member = retrieveMemberById(userId, groupId);
        member.setStatus(GroupMemberStatus.ACTIVE);
        entityManager.persist(member);
    }

    public void deleteMember (String userId, int groupId, boolean isSoftDelete)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        UserGroupMember m = retrieveMemberById(userId, groupId);
        if (isSoftDelete) {
            m.setStatus(GroupMemberStatus.DELETED);
            entityManager.persist(m);
        }
        else {
            entityManager.remove(m);
        }
    }

    public UserGroupMember retrieveMemberById (String userId, int groupId)
            throws KustvaktException {
        ParameterChecker.checkStringValue(userId, "userId");
        ParameterChecker.checkIntegerValue(groupId, "groupId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> query =
                criteriaBuilder.createQuery(UserGroupMember.class);

        Root<UserGroupMember> root = query.from(UserGroupMember.class);

        Predicate predicate = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroupMember_.group),
                        groupId),
                criteriaBuilder.equal(root.get(UserGroupMember_.userId),
                        userId));

        query.select(root);
        query.where(predicate);
        Query q = entityManager.createQuery(query);
        return (UserGroupMember) q.getSingleResult();
    }

    public List<UserGroupMember> retrieveMemberByRole (int groupId,
            int roleId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> query =
                criteriaBuilder.createQuery(UserGroupMember.class);

        Root<UserGroupMember> root = query.from(UserGroupMember.class);
        Join<UserGroupMember, Role> memberRole = root.join("roles");

        Predicate predicate = criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UserGroupMember_.group),
                        groupId),
                criteriaBuilder.equal(memberRole.get("role_id"), roleId));

        query.select(root);
        query.where(predicate);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }

    public List<UserGroupMember> retrieveMemberByGroupId (int groupId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserGroupMember> query =
                criteriaBuilder.createQuery(UserGroupMember.class);

        Root<UserGroupMember> root = query.from(UserGroupMember.class);

        Predicate predicate = criteriaBuilder.and(criteriaBuilder
                .equal(root.get(UserGroupMember_.group), groupId));

        query.select(root);
        query.where(predicate);
        Query q = entityManager.createQuery(query);
        return q.getResultList();
    }
}
