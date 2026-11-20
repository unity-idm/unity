/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static java.lang.Long.parseLong;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.imunity.rest.admin.AdminJsonBody;
import io.imunity.rest.admin.AdminJsonProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentId;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.mappers.policy.PolicyDocumentMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentNotFoundException;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@Produces(MediaType.APPLICATION_JSON)
@Path(RESTAdminEndpoint.V1_PATH)
@PrototypeComponent
public class PolicyDocumentsRESTAdmin implements RESTAdminHandler
{
	private final PolicyDocumentManagement policyDocumentManagement;

	@Autowired
	PolicyDocumentsRESTAdmin(PolicyDocumentManagement policyDocumentManagement)
	{
		this.policyDocumentManagement = policyDocumentManagement;
	}


	@Path("/policy-documents")
	@GET
	@Operation(operationId = "getPolicyDocuments", summary = "List policy documents",
			description = "Returns all policy documents visible to the administrator, with their current revisions.")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(mediaType = MediaType.APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = RestPolicyDocument.class))))
	public List<RestPolicyDocument> getPolicyDocuments() throws EngineException, JsonProcessingException
	{
		Collection<PolicyDocumentWithRevision> policyDocuments = policyDocumentManagement.getPolicyDocuments();
		List<RestPolicyDocument> restPolicyDocuments = policyDocuments.stream()
			.map(PolicyDocumentMapper::map)
			.collect(Collectors.toList());
		return restPolicyDocuments;
	}

	@Path("/policy-documents/{document-id}")
	@GET
	@Operation(operationId = "getPolicyDocument", summary = "Read a policy document",
			description = "Reads a document by its numeric ID. An unknown document returns 404.")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestPolicyDocument.class)))
	public RestPolicyDocument getPolicyDocument(@PathParam("document-id") String documentId) throws EngineException,
		JsonProcessingException
	{
		PolicyDocumentWithRevision policyDocument;
		try
		{
			policyDocument = policyDocumentManagement.getPolicyDocument(parseLong(documentId));
		}
		catch (PolicyDocumentNotFoundException e)
		{
			throw new NotFoundException(e);
		}
		return PolicyDocumentMapper.map(policyDocument);
	}

	@Path("/policy-documents")
	@POST
	@Operation(operationId = "addPolicyDocuments", summary = "Create a policy document",
			description = "Creates a policy document and returns its ID with status 200. The initial revision is 1.")
	@ApiResponse(responseCode = "200", description = "Success",
			content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestPolicyDocumentId.class)))
	public RestPolicyDocumentId addPolicyDocuments(
			@AdminJsonBody(AdminJsonBody.Failure.POLICY) RestPolicyDocumentRequest parsedDocument) throws EngineException, JsonProcessingException
	{
		validate(parsedDocument);
		long id = policyDocumentManagement.addPolicyDocument(PolicyDocumentMapper.map(parsedDocument));
		return new RestPolicyDocumentId(id);
	}

	@Path("/policy-documents/{document-id}")
	@PUT
	@Operation(operationId = "updatePolicyDocument", summary = "Update a policy document",
			description = "Requires incrementRevision before the body is parsed. True increments the revision; false preserves it.")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updatePolicyDocument(@PathParam("document-id") String documentId,
	                                 @io.swagger.v3.oas.annotations.Parameter(required = true)
	                                 @QueryParam("incrementRevision") Boolean incrementRevision,
	                                 @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = RestPolicyDocumentRequest.class)))
	                                 InputStream policyDocumentJson, @Context HttpHeaders headers) throws EngineException, IOException
	{
		if(incrementRevision == null)
			throw new BadRequestException("Query param incrementRevision is mandatory");
		RestPolicyDocumentRequest parsedDocument = parse(AdminJsonProvider.readText(policyDocumentJson, headers.getMediaType()));
		try
		{
			if(incrementRevision)
				policyDocumentManagement.updatePolicyDocumentWithRevision(PolicyDocumentMapper.map(parseLong(documentId),
					parsedDocument));
			else
				policyDocumentManagement.updatePolicyDocument(PolicyDocumentMapper.map(parseLong(documentId), parsedDocument));
		}
		catch (PolicyDocumentNotFoundException e)
		{
			throw new NotFoundException(e);
		}
	}

	@Path("/policy-documents/{document-id}")
	@DELETE
	@Operation(operationId = "deletePolicyDocument", summary = "Remove a policy document",
			description = "Deletes a document by numeric ID. An unknown document returns 404.")
	@ApiResponse(responseCode = "204", description = "Success")
	public void deletePolicyDocument(@PathParam("document-id") String documentId) throws EngineException
	{
		try
		{
			policyDocumentManagement.removePolicyDocument(parseLong(documentId));
		}
		catch (PolicyDocumentNotFoundException e)
		{
			throw new NotFoundException(e);
		}
	}

	public static RestPolicyDocumentRequest parse(String contents)
	{
		RestPolicyDocumentRequest request;
		try
		{
			request = Constants.MAPPER.readValue(contents, RestPolicyDocumentRequest.class);
		} catch (Exception e)
		{
			throw new BadRequestException("Can't perform JSON deserialization", e);
		}
		validate(request);
		return request;
	}

	private static void validate(RestPolicyDocumentRequest request)
	{
		if(request.name == null)
			throw new BadRequestException("Name is required");
		if(request.displayedName == null || request.displayedName.isEmpty())
			throw new BadRequestException("DisplayedName is required");
		if(request.mandatory == null)
			throw new BadRequestException("Mandatory is required");
		if(request.contentType == null)
			throw new BadRequestException("ContentType is required");
		if(request.content == null || request.content.isEmpty())
			throw new BadRequestException("Content is required");
	}
}




