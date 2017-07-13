package de.ids_mannheim.korap.web.service.full;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.dao.ResourceDao;
import de.ids_mannheim.korap.entity.Resource;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.web.filter.AuthFilter;
import de.ids_mannheim.korap.web.filter.DemoUserFilter;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/**
 * Provides information about free resources.
 * 
 * @author margaretha
 *
 */
@Service
@Path("resource/")
@ResourceFilters({ AuthFilter.class, DemoUserFilter.class, PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ResourceService {

    private static Logger jlog = LoggerFactory.getLogger(ResourceService.class);
    
    @Autowired
    private ResourceDao resourceDao; 
    
    @GET
    @Path("info")
    public Response getAllResourceInfo () {
        List<Resource> resources = resourceDao.getAllResources();
        String result = JsonUtils.toJSON(resources);
//        System.out.println(result);
        return Response.ok(result).build();
    }


    @POST
    @Path("layers")
    public Response getResourceLayers (
            @QueryParam("resourceId") List<String> resourceIds) {
        // TODO Auto-generated method stub
        return Response.status(200).build();
    }
}
