/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentId;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentNotFoundException;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.restadm.mappers.policy.PolicyDocumentMapper;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;

@Produces(MediaType.APPLICATION_JSON)
@Path(RESTAdminEndpoint.V1_PATH)
@PrototypeComponent
public class PolicyDocumentsRESTAdmin implements RESTAdminHandler
{
	private final ObjectMapper mapper = Constants.MAPPER;
	private final PolicyDocumentManagement policyDocumentManagement;

	@Autowired
	PolicyDocumentsRESTAdmin(PolicyDocumentManagement policyDocumentManagement)
	{
		this.policyDocumentManagement = policyDocumentManagement;
	}


	@Path("/policy-documents")
	@GET
	public String getPolicyDocuments() throws EngineException, JsonProcessingException
	{
		Collection<PolicyDocumentWithRevision> policyDocuments = policyDocumentManagement.getPolicyDocuments();
		List<RestPolicyDocument> restPolicyDocuments = policyDocuments.stream()
			.map(PolicyDocumentMapper::map)
			.collect(Collectors.toList());
		return mapper.writeValueAsString(restPolicyDocuments);
	}

	@Path("/policy-documents/{document-id}")
	@GET
	public String getPolicyDocument(@PathParam("document-id") String documentId) throws EngineException,
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
		return mapper.writeValueAsString(PolicyDocumentMapper.map(policyDocument));
	}

	@Path("/policy-documents")
	@POST
	public String addPolicyDocuments(String policyDocumentJson) throws EngineException, JsonProcessingException
	{
		RestPolicyDocumentRequest parsedDocument = JsonUtil.parse(policyDocumentJson,
			RestPolicyDocumentRequest.class);
		long id = policyDocumentManagement.addPolicyDocument(PolicyDocumentMapper.map(parsedDocument));
		return mapper.writeValueAsString(new RestPolicyDocumentId(id));
	}

	@Path("/policy-documents/{document-id}")
	@PUT
	public void updatePolicyDocument(@PathParam("document-id") String documentId,
	                                 @QueryParam("incrementRevision") boolean incrementRevision, String policyDocumentJson) throws EngineException
	{
		RestPolicyDocumentRequest parsedDocument = JsonUtil.parse(policyDocumentJson,
			RestPolicyDocumentRequest.class);
		if(incrementRevision)
			policyDocumentManagement.updatePolicyDocumentWithRevision(PolicyDocumentMapper.map(parseLong(documentId),
				parsedDocument));
		else
			policyDocumentManagement.updatePolicyDocument(PolicyDocumentMapper.map(parseLong(documentId), parsedDocument));
	}

	@Path("/policy-documents/{document-id}")
	@DELETE
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
}




