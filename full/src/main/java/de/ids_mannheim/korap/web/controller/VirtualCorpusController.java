package de.ids_mannheim.korap.web.controller;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

import de.ids_mannheim.korap.constant.OAuth2Scope;
import de.ids_mannheim.korap.constant.VirtualCorpusAccessStatus;
import de.ids_mannheim.korap.constant.VirtualCorpusType;
import de.ids_mannheim.korap.dto.VirtualCorpusAccessDto;
import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.oauth2.service.OAuth2ScopeService;
import de.ids_mannheim.korap.security.context.TokenContext;
import de.ids_mannheim.korap.service.VirtualCorpusService;
import de.ids_mannheim.korap.web.KustvaktResponseHandler;
import de.ids_mannheim.korap.web.filter.APIVersionFilter;
import de.ids_mannheim.korap.web.filter.AuthenticationFilter;
import de.ids_mannheim.korap.web.filter.BlockingFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;
import de.ids_mannheim.korap.web.input.VirtualCorpusJson;

/**
 * VirtualCorpusController defines web APIs related to virtual corpus
 * (VC) such as creating, deleting and listing user virtual corpora.
 * 
 * This class also includes APIs related to virtual corpus access
 * (VCA) such as sharing and publishing VC. When a VC is published,
 * it is shared with all users, but not always listed like system
 * VC. It is listed for a user, once when he/she have searched for the
 * VC. A VC can be published by creating or editing the VC.
 * 
 * All the APIs in this class are available to logged-in users.
 * 
 * @author margaretha
 *
 */
@Controller
@Path("{version}/vc")
@ResourceFilters({ APIVersionFilter.class, AuthenticationFilter.class,
        BlockingFilter.class, PiwikFilter.class })
public class VirtualCorpusController {

    @Autowired
    private KustvaktResponseHandler kustvaktResponseHandler;
    @Autowired
    private VirtualCorpusService service;
    @Autowired
    private OAuth2ScopeService scopeService;

    /**
     * Creates a user virtual corpus, also for system admins
     * 
     * @see VirtualCorpusJson
     * 
     * @param securityContext
     * @param vc
     *            a JSON object describing the virtual corpus
     * @return HTTP Response OK if successful
     */
    @POST
    @Path("create")
    @Consumes("application/json")
    @Deprecated
    public Response createVC (@Context SecurityContext securityContext,
            VirtualCorpusJson vc) {
        try {
            // get user info
            TokenContext context =
                    (TokenContext) securityContext.getUserPrincipal();

            scopeService.verifyScope(context, OAuth2Scope.CREATE_VC);
            service.storeVC(vc, vc.getName(), context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /**
     * Edits a virtual corpus attributes including name, type and
     * corpus query. Only the virtual corpus owner and system admins
     * can edit a virtual corpus.
     * 
     * @see VirtualCorpusJson
     * 
     * @param securityContext
     * @param vc
     *            a JSON object describing the virtual corpus
     * @return HTTP Response OK if successful
     * @throws KustvaktException
     */
    @POST
    @Path("edit")
    @Consumes("application/json")
    @Deprecated
    public Response editVC (@Context SecurityContext securityContext,
            VirtualCorpusJson vc) throws KustvaktException {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();

        try {
            scopeService.verifyScope(context, OAuth2Scope.EDIT_VC);
            service.editVC(vc, context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    // EM: changing vc name is disabled
    @PUT
    @Path("/{vcCreator}/{vcName}")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response editVC (@Context SecurityContext securityContext,
            @PathParam("vcCreator") String vcCreator,
            @PathParam("vcName") String vcName,
            VirtualCorpusJson vc) throws KustvaktException {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();

        try {
            scopeService.verifyScope(context, OAuth2Scope.EDIT_VC);
            service.handlePutRequest(context.getUsername(),vcCreator, vcName, vc);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /**
     * Searches for a specific VC given the VC id.
     * 
     * @param securityContext
     * @param vcId
     *            a virtual corpus id
     * @return a list of virtual corpora
     */
    @GET
    @Path("{vcId}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Deprecated
    public VirtualCorpusDto retrieveVC (
            @Context SecurityContext securityContext,
            @PathParam("vcId") int vcId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.searchVCById(context.getUsername(), vcId);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Returns the virtual corpus with the given name and creator.
     * 
     * @param securityContext
     * @param createdBy
     *            vc creator
     * @param vcName
     *            vc name
     * @return the virtual corpus with the given name and creator.
     */
    @GET
    @Path("{createdBy}/{vcName}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public VirtualCorpusDto retrieveVCByName (
            @Context SecurityContext securityContext,
            @PathParam("createdBy") String createdBy,
            @PathParam("vcName") String vcName) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.retrieveVCByName(context.getUsername(), vcName,
                    createdBy);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists all virtual corpora available to the authenticated user.
     *
     * System-admins can list available vc for a specific user by
     * specifiying the username parameter.
     * 
     * Normal users cannot list virtual corpora
     * available for other users. Thus, username parameter is optional
     * and must be identical to the authenticated username.
     * 
     * 
     * 
     * @param securityContext
     * @param username
     *            a username (optional)
     * @return a list of virtual corpora
     */
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Deprecated
    public List<VirtualCorpusDto> listVCByUser (
            @Context SecurityContext securityContext,
            @QueryParam("username") String username) {
        return listAvailableVC(securityContext, username);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<VirtualCorpusDto> listAvailableVC (
            @Context SecurityContext securityContext,
            @QueryParam("username") String username) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.listAvailableVCForUser(context.getUsername(),
                    username);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    // EM: TODO: change path to @Path("{createdBy}"), currently
    // conflicted with /{vcId}
    /**
     * Lists all virtual corpora created by a user
     * 
     * @param securityContext
     * @return a list of virtual corpora created by the user
     *         in the security context.
     */
    @GET
    @Path("list/user")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<VirtualCorpusDto> listUserVC (
            @Context SecurityContext securityContext) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_INFO);
            return service.listOwnerVC(context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists virtual corpora by creator and type. This is a controller
     * for system admin requiring valid system admin authentication.
     * 
     * If type is not specified, retrieves virtual corpora of all
     * types. If createdBy is not specified, retrieves virtual corpora
     * of all users.
     * 
     * @param securityContext
     * @param createdBy
     *            username of virtual corpus creator
     * @param type
     *            {@link VirtualCorpusType}
     * @return a list of virtual corpora
     */
    @GET
    @Path("list/system-admin")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<VirtualCorpusDto> listVCByStatus (
            @Context SecurityContext securityContext,
            @QueryParam("createdBy") String createdBy,
            @QueryParam("type") VirtualCorpusType type) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.ADMIN);
            return service.listVCByType(context.getUsername(), createdBy, type);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Only the VC owner and system admins can delete VC. VCA admins
     * can delete VC-accesses e.g. of project VC, but not the VC
     * themselves.
     * 
     * @param securityContext
     * @param vcId
     *            the id of the virtual corpus
     * @return HTTP status 200, if successful
     */
    @DELETE
    @Path("delete/{vcId}")
    @Deprecated
    public Response deleteVC (@Context SecurityContext securityContext,
            @PathParam("vcId") int vcId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.DELETE_VC);
            service.deleteVC(context.getUsername(), vcId);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /**
     * Only the VC owner and system admins can delete VC. VCA admins
     * can delete VC-accesses e.g. of project VC, but not the VC
     * themselves.
     * 
     * @param securityContext
     * @param createdBy
     *            vc creator
     * @param vcName
     *            vc name
     * @return HTTP status 200, if successful
     */
    @DELETE
    @Path("{createdBy}/{vcName}")
    public Response deleteVCByName (@Context SecurityContext securityContext,
            @PathParam("createdBy") String createdBy,
            @PathParam("vcName") String vcName) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.DELETE_VC);
            service.deleteVCByName(context.getUsername(), vcName, createdBy);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    // EM: TODO: replace vcId with vcCreator and vcUsername
    /**
     * VC can only be shared with a group, not individuals.
     * Only VCA admins are allowed to share VC and the VC must have
     * been created by themselves.
     * 
     * <br /><br />
     * Not allowed via third-party apps.
     * 
     * @param securityContext
     * @param vcId
     *            a virtual corpus id
     * @param groupId
     *            a user group id
     * @return HTTP status 200, if successful
     */
    @POST
    @Path("access/share")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response shareVC (@Context SecurityContext securityContext,
            @FormParam("vcId") int vcId, @FormParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.SHARE_VC);
            service.shareVC(context.getUsername(), vcId, groupId);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /**
     * Only VCA Admins and system admins are allowed to delete a
     * VC-access.
     * 
     * <br /><br />
     * Not allowed via third-party apps.
     * 
     * @param securityContext
     * @param accessId
     * @return
     */
    @DELETE
    @Path("access/delete/{accessId}")
    @Deprecated
    public Response deleteVCAccess (@Context SecurityContext securityContext,
            @PathParam("accessId") int accessId) {
        return deleteVCAccessById(securityContext, accessId);
    }

    @DELETE
    @Path("access/{accessId}")
    public Response deleteVCAccessById (
            @Context SecurityContext securityContext,
            @PathParam("accessId") int accessId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.DELETE_VC_ACCESS);
            service.deleteVCAccess(accessId, context.getUsername());
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
        return Response.ok().build();
    }

    /**
     * Lists active VC accesses to the specified VC.
     * Only available to VCA and system admins.
     * For system admins, lists all VCA of the VC.
     * 
     * <br /><br />
     * Not allowed via third-party apps.
     * 
     * @see VirtualCorpusAccessStatus
     * 
     * @param securityContext
     * @param vcId
     *            virtual corpus id
     * @return a list of access to the specified virtual corpus
     */
    @GET
    @Path("access/list")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Deprecated
    public List<VirtualCorpusAccessDto> listVCAccess (
            @Context SecurityContext securityContext,
            @QueryParam("vcId") int vcId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_ACCESS_INFO);
            return service.listVCAccessByVC(context.getUsername(), vcId);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists active VC-accesses available to a given VC or user-group.
     * 
     * Only available to VCA and system admins.
     * For system admins, list all VCA for the group.
     * 
     * @param securityContext
     * @param vcCreator
     *            the username of a VC creator
     * @param vcName
     *            the name of a VC
     * @param groupId
     *            a group id number
     * @return
     */
    @GET
    @Path("access")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<VirtualCorpusAccessDto> listVCAccesses (
            @Context SecurityContext securityContext,
            @QueryParam("vcCreator") String vcCreator,
            @QueryParam("vcName") String vcName,
            @QueryParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_ACCESS_INFO);
            if (groupId > 0) {
                return service.listVCAccessByGroup(context.getUsername(),
                        groupId);
            }
            return service.listVCAccessByVC(context.getUsername(), vcCreator,
                    vcName);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }

    /**
     * Lists active VC-accesses available for a user-group.
     * Only available to VCA and system admins.
     * For system admins, list all VCA for the group.
     * 
     * <br /><br />
     * Not allowed via third-party apps.
     * 
     * @param securityContext
     * @param groupId
     *            a group id
     * @return a list of VC-access
     */
    @GET
    @Path("access/list/byGroup")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Deprecated
    public List<VirtualCorpusAccessDto> listVCAccessByGroup (
            @Context SecurityContext securityContext,
            @QueryParam("groupId") int groupId) {
        TokenContext context =
                (TokenContext) securityContext.getUserPrincipal();
        try {
            scopeService.verifyScope(context, OAuth2Scope.VC_ACCESS_INFO);
            return service.listVCAccessByGroup(context.getUsername(), groupId);
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }
    }
}
