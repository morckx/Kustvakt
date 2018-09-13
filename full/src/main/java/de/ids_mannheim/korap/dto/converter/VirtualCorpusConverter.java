package de.ids_mannheim.korap.dto.converter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import de.ids_mannheim.korap.dto.VirtualCorpusDto;
import de.ids_mannheim.korap.entity.VirtualCorpus;
import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.utils.JsonUtils;

/**
 * VirtualCorpusConverter prepares data transfer objects (DTOs) from {@link VirtualCorpus}
 * entities. DTO structure defines controllers output, namely the structure of 
 * JSON objects in HTTP responses.
 * 
 * @author margaretha
 *
 */
@Component
public class VirtualCorpusConverter {

    public VirtualCorpusDto createVirtualCorpusDto (VirtualCorpus vc,
            String statistics) throws KustvaktException {

        VirtualCorpusDto dto = new VirtualCorpusDto();
        dto.setId(vc.getId());
        dto.setName(vc.getName());
        dto.setCreatedBy(vc.getCreatedBy());
        dto.setRequiredAccess(vc.getRequiredAccess().name());
        dto.setStatus(vc.getStatus());
        dto.setDescription(vc.getDescription());
        dto.setType(vc.getType().displayName());
        dto.setKoralQuery(vc.getKoralQuery());

        JsonNode node = JsonUtils.readTree(statistics);
        int numberOfDoc = node.at("/documents").asInt();
        dto.setNumberOfDoc(numberOfDoc);

        return dto;

    }
}
