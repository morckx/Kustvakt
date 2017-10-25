package de.ids_mannheim.korap.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.dao.ResourceDao;
import de.ids_mannheim.korap.dto.ResourceDto;
import de.ids_mannheim.korap.dto.converter.ResourceConverter;
import de.ids_mannheim.korap.entity.Resource;

@Service
public class ResourceService {

    private static Logger jlog = LoggerFactory.getLogger(ResourceService.class);

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ResourceConverter resourceConverter;

    public List<ResourceDto> getResourceDtos () {
        List<Resource> resources = resourceDao.getAllResources();
        List<ResourceDto> resourceDtos =
                resourceConverter.convertToResourcesDto(resources);
        jlog.debug("/info " + resourceDtos.toString());
        return resourceDtos;
    }

}
