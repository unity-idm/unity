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
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.restadm.mappers.policy.PolicyDocumentMapper;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;

@Produces(MediaType.APPLICATION_JSON)
@Path(RESTAdminEndpoint.V1_PATH)
@PrototypeComponent
public class PolicyDocumentsRESTAdmin implements RESTAdminHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, PolicyDocumentsRESTAdmin.class);
	private ObjectMapper mapper = Constants.MAPPER;
	private PolicyDocumentManagement policyDocumentManagement;

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
		PolicyDocumentWithRevision policyDocument =
			policyDocumentManagement.getPolicyDocument(parseLong(documentId));
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
	                                 @QueryParam("revision") boolean revision, String policyDocumentJson) throws EngineException
	{
		RestPolicyDocumentRequest parsedDocument = JsonUtil.parse(policyDocumentJson,
			RestPolicyDocumentRequest.class);
		if(revision)
			policyDocumentManagement.updatePolicyDocumentWithRevision(PolicyDocumentMapper.map(parseLong(documentId),
				parsedDocument));
		else
			policyDocumentManagement.updatePolicyDocument(PolicyDocumentMapper.map(parseLong(documentId), parsedDocument));
	}

	@Path("/policy-documents/{document-id}")
	@DELETE
	public void deletePolicyDocument(@PathParam("document-id") String documentId) throws EngineException
	{
		policyDocumentManagement.removePolicyDocument(parseLong(documentId));
	}

	@Path("/policy-documents/{document-id}/content")
	@GET
	public Response getPolicyDocumentContent(@PathParam("document-id") String documentId, @Context HttpServletRequest request) throws EngineException
	{
		String language = request.getLocale().getLanguage();

		PolicyDocumentWithRevision policyDocument =
			policyDocumentManagement.getPolicyDocument(parseLong(documentId));

		String content = Optional.ofNullable(policyDocument.content.getValue(language))
			.orElse(policyDocument.content.getDefaultValue());

		if(policyDocument.contentType.equals(PolicyDocumentContentType.EMBEDDED))
			return Response.ok(content, MediaType.TEXT_HTML).build();
		else
			return Response.ok(content, MediaType.TEXT_PLAIN).build();
	}

}




