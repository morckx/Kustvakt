package de.ids_mannheim.korap.web.controller;

import java.util.Locale;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sun.jersey.spi.container.ResourceFilters;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import de.ids_mannheim.korap.utils.KoralCollectionQueryBuilder;
import de.ids_mannheim.korap.web.CoreResponseHandler;
import de.ids_mannheim.korap.web.SearchKrill;
import de.ids_mannheim.korap.web.filter.PiwikFilter;

/**
 * Web services related to statistics
 * 
 * @author hanl
 * @author margaretha
 *
 * @date 08/11/2017
 * 
 */
@Controller
@Path("statistics/")
@ResourceFilters({ PiwikFilter.class })
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class StatisticController {


    private static Logger jlog =
            LoggerFactory.getLogger(StatisticController.class);
    @Autowired
    private CoreResponseHandler kustvaktResponseHandler;
    @Autowired
    private SearchKrill searchKrill;


    /**
     * Returns statistics of the virtual corpus defined by the given
     * collectionQuery parameter.
     * 
     * @param context
     *            SecurityContext
     * @param locale
     *            Locale
     * @param collectionQuery
     *            a collection query specifying a virtual corpus
     * @return statistics of the virtual corpus defined by the given
     *         collectionQuery parameter.
     */
    @GET
    public Response getStatistics (@Context SecurityContext context,
            @Context Locale locale,
            @QueryParam("collectionQuery") String collectionQuery) {

        if (collectionQuery == null || collectionQuery.isEmpty()) {
            throw kustvaktResponseHandler
                    .throwit(new KustvaktException(StatusCodes.MISSING_ARGUMENT,
                            "Parameter collectionQuery is missing.",
                            "collectionQuery"));
        }


        KoralCollectionQueryBuilder builder = new KoralCollectionQueryBuilder();
        builder.with(collectionQuery);
        String json = null;
        try {
            json = builder.toJSON();
        }
        catch (KustvaktException e) {
            throw kustvaktResponseHandler.throwit(e);
        }

        String stats = searchKrill.getStatistics(json);
        if (stats.contains("-1"))
            throw kustvaktResponseHandler.throwit(StatusCodes.NO_RESULT_FOUND);
        jlog.debug("Stats: " + stats);
        return Response.ok(stats).build();
    }
}
