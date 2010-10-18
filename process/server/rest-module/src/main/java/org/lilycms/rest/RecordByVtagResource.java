package org.lilycms.rest;

import org.lilycms.repository.api.*;
import org.lilycms.util.repo.VersionTag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import java.util.Collections;
import java.util.List;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("record/{id}/vtag/{vtag}")
public class RecordByVtagResource extends RepositoryEnabled {

    @GET
    @Produces("application/json")
    public Record get(@PathParam("id") String id, @PathParam("vtag") String vtag, @Context UriInfo uriInfo) {
        QName vtagName = new QName(VersionTag.NAMESPACE, vtag);
        List<QName> fieldQNames = ResourceClassUtil.parseFieldList(uriInfo);

        RecordId recordId = repository.getIdGenerator().fromString(id);
        Record record;
        try {
            // First read record with its vtags
            try {
                record = repository.read(recordId, Collections.singletonList(vtagName));
            } catch (FieldTypeNotFoundException e) {
                // We assume this is because the vtag field type could not be found
                // (no other field types should be loaded, so this is the only one this exception should
                //  be thrown for)
                throw new ResourceException(e, NOT_FOUND.getStatusCode());
            }

            if (!record.hasField(vtagName)) {
                throw new ResourceException("Record does not have a vtag " + vtagName, NOT_FOUND.getStatusCode());
            }

            long version = (Long)record.getField(vtagName);

            record = repository.read(recordId, version, fieldQNames);
        } catch (RecordNotFoundException e) {
            throw new ResourceException(e, NOT_FOUND.getStatusCode());
        } catch (RepositoryException e) {
            throw new ResourceException("Error loading record.", e, INTERNAL_SERVER_ERROR.getStatusCode());
        }
        return record;

    }

}
