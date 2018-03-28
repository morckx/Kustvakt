package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.constant.UserGroupStatus;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.service.UserGroupService;
import de.ids_mannheim.korap.user.TokenContext;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.FullResponseHandler;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.input.UserGroupJson;

/**
 * UserGroupController defines web APIs related to user groups,
 * such as creating a user group, listing groups of a user,
 * adding members to a group and subscribing (confirming an
 * invitation) to a group.
 * 
 * These APIs are only available to logged-in users.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("group")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
@ResourceFilters({ AuthenticationFilter.class, BlockingFilter.class,
        PiwikFilter.class })
public class UserGroupController {

    @Autowired
    private FullResponseHandler responseHandler;
    @Autowired
    private UserGroupService service;

    /** Returns all user-groups in which a user is an active or a pending member.
     *  Not suitable for system-admin, instead use {@link UserGroupController#
     *  getUserGroupBySystemAdmin(SecurityContext, String, UserGroupStatus)} 
     * 
     * @param securityContext
     * @return a list of user-groups
     * 
     */
    @GET
    @Path("list")
    public Response getUserGroup (@Context SecurityContext securityContext) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            List<UserGroupDto> dtos =
                    service.retrieveUserGroup(context.getUsername());
            String result = JsonUtils.toJSON(dtos);
            return Response.ok(result).build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }


    /** Lists user-groups for system-admin purposes. If username parameter 
     *  is not specified, list user-groups of all users. If status is not
     *  specified, list user-groups of all statuses.
     * 
     * @param securityContext
     * @param username username
     * @param status {@link UserGroupStatus}
     * @return a list of user-groups
     */
    @GET
    @Path("list/system-admin")
    public Response getUserGroupBySystemAdmin (
            @Context SecurityContext securityContext,
            @QueryParam("username") String username,
            @QueryParam("status") UserGroupStatus status) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            List<UserGroupDto> dtos = service.retrieveUserGroupByStatus(
                    username, context.getUsername(), status);
            String result = JsonUtils.toJSON(dtos);
            return Response.ok(result).build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /** Retrieves a specific user-group for system admins.
     * 
     * @param securityContext
     * @param groupId group id
     * @return a user-group
     */
    @GET
    @Path("{groupId}")
    public Response searchUserGroup (@Context SecurityContext securityContext,
            @PathParam("groupId") int groupId) {
        String result;
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            UserGroupDto dto =
                    service.searchById(context.getUsername(), groupId);
            result = JsonUtils.toJSON(dto);
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
        return Response.ok(result).build();
    }

    /** Creates a user group where the user in token context is the 
     * group owner, and assigns the listed group members with status 
     * GroupMemberStatus.PENDING. 
     * 
     * Invitations must be sent to these proposed members. If a member
     * accepts the invitation, update his GroupMemberStatus to 
     * GroupMemberStatus.ACTIVE by using 
     * {@link UserGroupController#subscribeToGroup(SecurityContext, String)}.
     * 
     * If he rejects the invitation, update his GroupMemberStatus 
     * to GroupMemberStatus.DELETED using 
     * {@link UserGroupController#unsubscribeFromGroup(SecurityContext, String)}.
     * 
     *  
     * 
     * @param securityContext
     * @param group UserGroupJson
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUserGroup (@Context SecurityContext securityContext,
            UserGroupJson group) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.createUserGroup(group, context.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /** Deletes a user-group specified by the group id. Only group owner 
     *  and system admins can delete groups. 
     * 
     * @param securityContext
     * @param groupId
     * @return HTTP 200, if successful.
     */
    @DELETE
    @Path("delete")
    public Response deleteUserGroup (@Context SecurityContext securityContext,
            @QueryParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.deleteGroup(groupId, context.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /** Deletes a user-group member. Group owner cannot be deleted.
     * 
     * @param securityContext
     * @param memberId a username of a group member
     * @param groupId a group id
     * @return if successful, HTTP response status OK
     */
    @DELETE
    @Path("member/delete")
    public Response deleteUserFromGroup (
            @Context SecurityContext securityContext,
            @QueryParam("memberId") String memberId,
            @QueryParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.deleteGroupMember(memberId, groupId, context.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /** Invites group members to join a user-group specified in the JSON object.
     * @param securityContext
     * @param group UserGroupJson containing groupId and usernames to be invited
     * as members 
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("member/invite")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response inviteGroupMembers (
            @Context SecurityContext securityContext, UserGroupJson group) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.inviteGroupMembers(group, context.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /** Adds roles of an active member of a user-group. Only user-group admins
     * and system admins are allowed.
     * 
     * @param securityContext
     * @param groupId a group id
     * @param memberUsername the username of a group member
     * @param roleIds list of role ids
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("member/role/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addMemberRoles (@Context SecurityContext securityContext,
            @FormParam("groupId") int groupId,
            @FormParam("memberUsername") String memberUsername,
            @FormParam("roleIds") List<Integer> roleIds) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.addMemberRoles(context.getUsername(), groupId,
                    memberUsername, roleIds);
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /** Deletes roles of a member of a user-group. Only user-group admins
     * and system admins are allowed.
     * 
     * @param securityContext
     * @param groupId a group id
     * @param memberUsername the username of a group member
     * @param roleIds list of role ids
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("member/role/delete")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteMemberRoles (@Context SecurityContext securityContext,
            @FormParam("groupId") int groupId,
            @FormParam("memberUsername") String memberUsername,
            @FormParam("roleIds") List<Integer> roleIds) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.deleteMemberRoles(context.getUsername(), groupId,
                    memberUsername, roleIds);
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /** Handles requests to accept membership invitation. Only invited users 
     * can subscribe to the corresponding user-group. 
     * 
     * @param securityContext
     * @param groupId a group id
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("subscribe")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response subscribeToGroup (@Context SecurityContext securityContext,
            @FormParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.acceptInvitation(groupId, context.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }

    /** Handles requests to reject membership invitation. A member can only 
     * unsubscribe him/herself from a group. 
     * 
     * Implemented identical to delete group member.
     * 
     * @param securityContext
     * @param groupId
     * @return if successful, HTTP response status OK
     */
    @POST
    @Path("unsubscribe")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response unsubscribeFromGroup (
            @Context SecurityContext securityContext,
            @FormParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            service.deleteGroupMember(context.getUsername(), groupId,
                    context.getUsername());
            return Response.ok().build();
        }
        catch (KustvaktException e) {
            throw responseHandler.throwit(e);
        }
    }
}
