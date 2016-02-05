/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.json.AttributeTypeSerializer;
import pl.edu.icm.unity.rest.exception.JSONParsingException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeParamRepresentation;
import pl.edu.icm.unity.types.basic.AttributeRepresentation;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupContentsRepresentation;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.RESTInvitationParam;
import pl.edu.icm.unity.types.registration.invite.RESTInvitationWithCode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * RESTful API implementation.
 * 
 * @author K. Benedyczak
 */
@Produces(MediaType.APPLICATION_JSON)
@Path(RESTAdminEndpointFactory.V1_PATH)
public class RESTAdmin
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTAdmin.class);
	private IdentitiesManagement identitiesMan;
	private GroupsManagement groupsMan;
	private AttributesManagement attributesMan;
	private ObjectMapper mapper = Constants.MAPPER;
	private IdentityTypesRegistry identityTypesRegistry;
	private AttributeTypeSerializer attrTypeSerializer;
	private AttributeSyntaxFactoriesRegistry attributeSyntaxFactoriesRegistry;
	private ConfirmationManager confirmationManager;
	private EndpointManagement endpointManagement;
	private RegistrationsManagement registrationManagement;
	
	public RESTAdmin(IdentitiesManagement identitiesMan, GroupsManagement groupsMan,
			AttributesManagement attributesMan, IdentityTypesRegistry identityTypesRegistry,
			AttributeTypeSerializer attrTypeSerializer,
			AttributeSyntaxFactoriesRegistry attributeSyntaxFactoriesRegistry,
			ConfirmationManager confirmationManager, EndpointManagement endpointManagement,
			RegistrationsManagement registrationManagement)
	{
		super();
		this.identitiesMan = identitiesMan;
		this.groupsMan = groupsMan;
		this.attributesMan = attributesMan;
		this.identityTypesRegistry = identityTypesRegistry;
		this.attrTypeSerializer = attrTypeSerializer;
		this.attributeSyntaxFactoriesRegistry = attributeSyntaxFactoriesRegistry;
		this.confirmationManager = confirmationManager;
		this.endpointManagement = endpointManagement;
		this.registrationManagement = registrationManagement;
	}

	@Path("/resolve/{identityType}/{identityValue}")
	@GET
	public String getEntity(@PathParam("identityType") String identityType, 
			@PathParam("identityValue") String identityValue) 
			throws EngineException, JsonProcessingException
	{
		log.debug("resolve query for " + identityType + ":" + identityValue);
		Entity entity = identitiesMan.getEntity(new EntityParam(new IdentityTaV(identityType, identityValue)));
		return mapper.writeValueAsString(entity);
	}

	
	@Path("/entity/{entityId}")
	@GET
	public String getEntity(@PathParam("entityId") long entityId) throws EngineException, JsonProcessingException
	{
		log.debug("getEntity query for " + entityId);
		Entity entity = identitiesMan.getEntity(new EntityParam(entityId));
		return mapper.writeValueAsString(entity);
	}
	
	@Path("/entity/{entityId}")
	@DELETE
	public void removeEntity(@PathParam("entityId") long entityId) throws EngineException, JsonProcessingException
	{
		log.debug("removeEntity of " + entityId);
		identitiesMan.removeEntity(new EntityParam(entityId));
	}

	@Path("/entity/{entityId}/removal-schedule")
	@PUT
	public void scheduleRemoval(@PathParam("entityId") long entityId, @QueryParam("when") long when) 
			throws EngineException, JsonProcessingException
	{
		log.debug("scheduleRemovalByUser of " + entityId + " on " + when);
		Date time = new Date(when);
		identitiesMan.scheduleRemovalByUser(new EntityParam(entityId), time);
	}

	@Path("/entity/{entityId}/admin-schedule")
	@PUT
	public void scheduleOperation(@PathParam("entityId") long entityId, @QueryParam("when") long when,
			@QueryParam("operation") String operationStr) 
			throws EngineException
	{
		log.debug("scheduleEntityChange of " + entityId + " on " + when + " op " + operationStr);
		Date time = new Date(when);
		EntityScheduledOperation operation;
		try
		{
			operation = EntityScheduledOperation.valueOf(operationStr);
		} catch (Exception e)
		{
			throw new WrongArgumentException("Given operation '" + operationStr 
					+ "' is unknown, valid are: " + 
					Arrays.toString(EntityScheduledOperation.values()));
		}
		identitiesMan.scheduleEntityChange(new EntityParam(entityId), time, operation);
	}
	
	@Path("/entity/identity/{type}/{value}")
	@POST
	public String addEntity(@PathParam("type") String type, @PathParam("value") String value, 
			@QueryParam("credentialRequirement") String credReqIdId) 
			throws EngineException, JsonProcessingException
	{
		log.debug("addEntity " + value + " type: " + type);
		Identity identity = identitiesMan.addEntity(resolveIdentity(type, value), 
				credReqIdId, EntityState.valid, false);
		ObjectNode ret = mapper.createObjectNode();
		ret.put("entityId", identity.getEntityId());
		return mapper.writeValueAsString(ret);
	}

	
	@Path("/entity/{entityId}/identity/{type}/{value}")
	@POST
	public void addIdentity(@PathParam("type") String type, @PathParam("value") String value, 
			@PathParam("entityId") long entityId) 
			throws EngineException, JsonProcessingException
	{
		log.debug("addIdentity of " + value + " type: " + type + " for entity: " + entityId);
		identitiesMan.addIdentity(resolveIdentity(type, value), new EntityParam(entityId), false);
	}

	private IdentityParam resolveIdentity(String type, String value) throws EngineException
	{
		IdentityTypeDefinition idType = identityTypesRegistry.getByName(type);
		return idType.convertFromString(value, null, null);
	}
	
	@Path("/entity/identity/{type}/{value}")
	@DELETE
	public void removeIdentity(@PathParam("type") String type, @PathParam("value") String value,
			@QueryParam("target") String target, @QueryParam("realm") String realm) 
			throws EngineException, JsonProcessingException
	{
		log.debug("removeIdentity of " + value + " type: " + type + " target: " + target + " realm: " + realm);
		identitiesMan.removeIdentity(new IdentityTaV(type, value, target, realm));
	}
	
	@Path("/entity/{entityId}/groups")
	@GET
	public String getGroups(@PathParam("entityId") long entityId) throws EngineException, JsonProcessingException
	{
		log.debug("getGroups query for " + entityId);
		Map<String, GroupMembership> groups = identitiesMan.getGroups(new EntityParam(entityId));
		return mapper.writeValueAsString(groups.keySet());
	}

	@Path("/entity/{entityId}/attributes")
	@GET
	public String getAttributes(@PathParam("entityId") long entityId,
			@QueryParam("group") String group) throws EngineException, JsonProcessingException
	{
		if (group == null)
			group = "/";
		log.debug("getAttributes query for " + entityId + " in " + group);
		Collection<AttributeExt<?>> attributes = attributesMan.getAttributes(
				new EntityParam(entityId), group, null);
		
		List<AttributeRepresentation> wrapped = new ArrayList<AttributeRepresentation>(attributes.size());
		for (AttributeExt<?> a: attributes)
			wrapped.add(new AttributeRepresentation(a));
		
		return mapper.writeValueAsString(wrapped);
	}

	@Path("/entity/{entityId}/attribute/{attributeName}")
	@DELETE
	public void removeAttribute(@PathParam("entityId") long entityId, @PathParam("attributeName") String attribute,
			@QueryParam("group") String group) throws EngineException, JsonProcessingException
	{
		if (group == null)
			group = "/";
		log.debug("removeAttribute " + attribute + " of " + entityId + " in " + group);
		attributesMan.removeAttribute(new EntityParam(entityId), group, attribute);
	}
	
	@Path("/entity/{entityId}/attribute")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setAttribute(@PathParam("entityId") long entityId, String attribute) 
			throws EngineException, JsonProcessingException
	{
		log.debug("setAttribute for " + entityId);
		AttributeParamRepresentation attributeParam;
		try
		{
			attributeParam = mapper.readValue(attribute, 
					AttributeParamRepresentation.class);
		} catch (IOException e)
		{
			throw new JSONParsingException("Can't parse the attribute input", e);
		}
		setAttribute(attributeParam, entityId);
	}

	@Path("/entity/{entityId}/attributes")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setAttributes(@PathParam("entityId") long entityId, String attributes) 
			throws EngineException, IOException
	{
		log.debug("Bulk setAttributes for " + entityId);
		
		JsonNode root = mapper.readTree(attributes);
		if (!root.isArray())
			throw new JSONParsingException("Can't parse the attributes input: root is not an array");
		ArrayNode rootA = (ArrayNode) root;
		List<AttributeParamRepresentation> parsedParams = new ArrayList<>(rootA.size());
		for (JsonNode node: rootA)
		{
			try
			{
				parsedParams.add(mapper.readValue(mapper.writeValueAsString(node), 
						AttributeParamRepresentation.class));
			} catch (IOException e)
			{
				throw new JSONParsingException("Can't parse the attribute input", e);
			}
		}
		for (AttributeParamRepresentation ap: parsedParams)
			setAttribute(ap, entityId);
	}

	private void setAttribute(AttributeParamRepresentation attributeParam, long entityId) throws EngineException
	{
		log.debug("setAttribute: " + attributeParam.getName() + " in " + attributeParam.getGroupPath());
		Map<String, AttributeType> attributeTypesAsMap = attributesMan.getAttributeTypesAsMap();
		Attribute<?> apiAttribute = toAPIAttribute(attributeParam, attributeTypesAsMap);
		attributesMan.setAttribute(new EntityParam(entityId), apiAttribute, true);
	}
	
	private Attribute<?> toAPIAttribute(AttributeParamRepresentation attributeParam,
			Map<String, AttributeType> attributeTypesAsMap) throws IllegalAttributeTypeException
	{
		AttributeType aType = attributeTypesAsMap.get(attributeParam.getName());
		if (aType == null)
			throw new IllegalAttributeTypeException("Attribute type " + attributeParam.getName() + 
					" does not exist");
		return attributeParam.toAPIAttribute(aType.getValueType());
	}
	
	@Path("/entity/{entityId}/credential-adm/{credential}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setCredentialByAdmin(@PathParam("entityId") long entityId, @PathParam("credential") String credential,
			String secrets) 
			throws EngineException, JsonProcessingException
	{
		log.debug("setCredentialByAdmin for " + entityId);
		identitiesMan.setEntityCredential(new EntityParam(entityId), credential, secrets);
	}
	
	@Path("/entity/{entityId}/credential/{credential}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setCredentialByUser(@PathParam("entityId") long entityId, @PathParam("credential") String credential,
			String secretsArray) 
			throws EngineException, JsonProcessingException
	{
		log.debug("setCredentialByUser for " + entityId);
		JsonNode main;
		try
		{
			main = mapper.readTree(secretsArray);
		} catch (IOException e)
		{
			throw new JSONParsingException("Request body can not be parsed as JSON", e);
		}
		
		if (main instanceof ArrayNode)
		{
			ArrayNode mainA = (ArrayNode) main;
			if (mainA.size() < 1)
				throw new  JSONParsingException("Request body JSON array must have at least one element");
			String newSecrets = mainA.get(0).asText();
			String oldSecrets = mainA.size() > 1 ? mainA.get(1).asText() : null;
			identitiesMan.setEntityCredential(new EntityParam(entityId), credential, 
					newSecrets, oldSecrets);
		} else
		{
			throw new JSONParsingException("Request body must be a JSON array");
		}
	}

	
	@Path("/group/{groupPath}")
	@GET
	public String getGroupContents(@PathParam("groupPath") String group) throws EngineException, JsonProcessingException
	{
		log.debug("getGroupContents query for " + group);
		GroupContents contents = groupsMan.getContents(group, GroupContents.GROUPS | GroupContents.MEMBERS);
		return mapper.writeValueAsString(new GroupContentsRepresentation(contents));
	}
	
	@Path("/group/{groupPath}/entity/{entityId}")
	@DELETE
	public void removeMember(@PathParam("groupPath") String group, @PathParam("entityId") long entityId) 
			throws EngineException, JsonProcessingException
	{
		log.debug("removeMember " + entityId + " from " + group);
		groupsMan.removeMember(group, new EntityParam(entityId));
	}
	
	@Path("/group/{groupPath}/entity/{entityId}")
	@POST
	public void addMember(@PathParam("groupPath") String group, @PathParam("entityId") long entityId) 
			throws EngineException, JsonProcessingException
	{
		log.debug("addMember " + entityId + " to " + group);
		groupsMan.addMemberFromParent(group, new EntityParam(entityId));
	}
	
	@Path("/attributeTypes")
	@GET
	public String getAttributeTypes() throws EngineException, JsonProcessingException
	{
		Collection<AttributeType> attributeTypes = attributesMan.getAttributeTypes();
		ArrayNode root = mapper.createArrayNode();
		for (AttributeType at: attributeTypes)
			root.add(attrTypeSerializer.toJsonNodeFull(at));
		return mapper.writeValueAsString(root);
	}
	
	@Path("/attributeType")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void addAttributeType(String jsonRaw) throws EngineException
	{
		log.debug("addAttributeType " + jsonRaw);
		AttributeType at = attrTypeSerializer.fromJsonFull(jsonRaw.getBytes(StandardCharsets.UTF_8), 
				attributeSyntaxFactoriesRegistry);
		log.debug("addAttributeType " + at.getName());
		attributesMan.addAttributeType(at);
	}

	@Path("/attributeType")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateAttributeType(String jsonRaw) throws EngineException
	{
		log.debug("updateAttributeType " + jsonRaw);
		AttributeType at = attrTypeSerializer.fromJsonFull(jsonRaw.getBytes(StandardCharsets.UTF_8), 
				attributeSyntaxFactoriesRegistry);
		log.debug("updateAttributeType " + at.getName());
		attributesMan.updateAttributeType(at);
	}
	
	@Path("/attributeType/{toRemove}")
	@DELETE
	public void removeAttributeType(@PathParam("toRemove") String toRemove, 
			@QueryParam("withInstances") String withInstances) throws EngineException
	{
		log.debug("removeAttributeType " + toRemove);
		boolean instances = false;
		if (withInstances != null)
			instances = Boolean.parseBoolean(withInstances);
		attributesMan.removeAttributeType(toRemove, instances);
	}

	@Path("/confirmation-trigger/entity/{entityId}/attribute/{attributeName}")
	@POST
	public void resendConfirmationForAttribute(@PathParam("entityId") long entityId, 
			@PathParam("attributeName") String attribute,
			@QueryParam("group") String group) throws EngineException, JsonProcessingException
	{
		if (group == null)
			group = "/";
		log.debug("confirmation trigger for " + attribute + " of " + entityId + " in " + group);
		EntityParam entityParam = new EntityParam(entityId);
		Collection<AttributeExt<?>> attributes = attributesMan.getAttributes(entityParam, group, attribute);
		
		if (attributes.isEmpty())
			throw new WrongArgumentException("Attribute is undefined");
		
		confirmationManager.sendVerificationsQuiet(entityParam, attributes, true);
	}

	@Path("/confirmation-trigger/identity/{type}/{value}")
	@POST
	public void resendConfirmationForIdentity(@PathParam("type") String idType, 
			@PathParam("value") String value) throws EngineException, JsonProcessingException
	{
		log.debug("confirmation trigger for " + idType + ": " + value);
		EntityParam entityParam = new EntityParam(new IdentityTaV(idType, value));
		Entity entity = identitiesMan.getEntity(entityParam);
		for (Identity id: entity.getIdentities())
			if (id.getTypeId().equals(idType) && id.getValue().equals(value))
			{
				confirmationManager.sendVerification(entityParam, id, true);
				return;
			}

		throw new WrongArgumentException("Identity is unknown");
	}
	
	
	@Path("/endpoints")
	@GET
	public String getEndpoints() throws EngineException, JsonProcessingException
	{
		List<EndpointDescription> endpoints = endpointManagement.getEndpoints();
		return mapper.writeValueAsString(endpoints);
	}
	
	@Path("/endpoint/{id}")
	@DELETE
	public void undeployEndpoint(@PathParam("id") String id) throws EngineException
	{
		endpointManagement.undeploy(id);
	}
	
	@Path("/endpoint/{id}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public String deployEndpoint(@QueryParam("typeId") String typeId, 
			@PathParam("id") String id, 
			@QueryParam("address") String address, 
			String configurationJson) throws EngineException, IOException
	{
		EndpointConfiguration configuration = new EndpointConfiguration(JsonUtil.parse(configurationJson));
		EndpointDescription deployed = endpointManagement.deploy(typeId, id, address, configuration);
		return mapper.writeValueAsString(deployed);
	}

	@Path("/endpoint/{id}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateEndpoint(@PathParam("id") String id, 
			String configurationJson) throws EngineException, IOException
	{
		EndpointConfiguration configuration = new EndpointConfiguration(JsonUtil.parse(configurationJson));
		endpointManagement.updateEndpoint(id, configuration);
	}
	
	
	@Path("/registrationForms")
	@GET
	public String getRegistrationForms() throws EngineException, JsonProcessingException
	{
		List<RegistrationForm> forms = registrationManagement.getForms();
		return mapper.writeValueAsString(forms);
	}
	
	@Path("registrationForm/{formId}")
	@DELETE
	public void removeRegistrationForm(@PathParam("formId") String formId, 
			@QueryParam("dropRequests") Boolean dropRequests) throws EngineException
	{
		if (dropRequests == null)
			dropRequests = false;
		registrationManagement.removeForm(formId, dropRequests);
	}
	
	@Path("registrationForm")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void addForm(String json) throws EngineException, IOException
	{
		RegistrationForm form = new RegistrationForm(JsonUtil.parse(json));
		registrationManagement.addForm(form);
	}
	
	@Path("registrationForm")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateForm(@QueryParam("ignoreRequests") Boolean ignoreRequests,
			String json) throws EngineException, IOException
	{
		if (ignoreRequests == null)
			ignoreRequests = false;
		RegistrationForm form = new RegistrationForm(JsonUtil.parse(json));
		registrationManagement.updateForm(form, ignoreRequests);
	}
	
	
	@Path("/invitations")
	@GET
	public String getInvitations() throws EngineException, JsonProcessingException
	{
		List<InvitationWithCode> invitations = registrationManagement.getInvitations();
		List<RESTInvitationWithCode> restInvitations = invitations.stream()
				.map(InvitationWithCode::toRESTVariant)
				.collect(Collectors.toList());
		return mapper.writeValueAsString(restInvitations);
	}

	@Path("/invitation/{code}")
	@GET
	public String getInvitation(@PathParam("code") String code) throws EngineException, JsonProcessingException
	{
		InvitationWithCode invitation = registrationManagement.getInvitation(code);
		return mapper.writeValueAsString(invitation.toRESTVariant());
	}
	
	@Path("invitation/{code}")
	@DELETE
	public void removeInvitation(@PathParam("code") String code) throws EngineException
	{
		registrationManagement.removeInvitation(code);
	}

	@Path("invitation/{code}/send")
	@POST
	public void sendInvitation(@PathParam("code") String code) throws EngineException, IOException
	{
		registrationManagement.sendInvitation(code);
	}

	@Path("invitation")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String addInvitation(String jsonInvitation) throws EngineException, IOException
	{
		ObjectNode json = JsonUtil.parse(jsonInvitation);
		RESTInvitationParam restInvitationParam = new RESTInvitationParam(json);

		Map<Integer, PrefilledEntry<Attribute<?>>> apiPrefilledAttributes = 
				toAPIPrefilledAttributes(restInvitationParam.getAttributes());
		
		InvitationParam invitationParam = new InvitationParam(restInvitationParam, apiPrefilledAttributes);
		return registrationManagement.addInvitation(invitationParam);
	}
	
	private Map<Integer, PrefilledEntry<Attribute<?>>> toAPIPrefilledAttributes(
			Map<Integer, PrefilledEntry<AttributeParamRepresentation>> restAttributes) 
					throws EngineException
	{
		Map<String, AttributeType> attributeTypesAsMap = attributesMan.getAttributeTypesAsMap();
		Map<Integer, PrefilledEntry<Attribute<?>>> ret = new HashMap<>(restAttributes.size());
		
		for (Map.Entry<Integer, PrefilledEntry<AttributeParamRepresentation>> restAE: restAttributes.entrySet())
		{
			PrefilledEntry<AttributeParamRepresentation> value = restAE.getValue();
			Attribute<?> apiAttribute = toAPIAttribute(value.getEntry(), attributeTypesAsMap);
			ret.put(restAE.getKey(), new PrefilledEntry<Attribute<?>>(apiAttribute, value.getMode()));
		}
		
		return ret;
	}
}




