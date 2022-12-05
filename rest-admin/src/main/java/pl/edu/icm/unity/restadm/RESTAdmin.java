/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.*;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.event.EventPublisherWithAuthz;
import pl.edu.icm.unity.engine.api.groupMember.GroupMembersService;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce.ImportResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.json.Token2JsonFormatter;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import io.imunity.rest.api.RestGroupMemberWithAttributes;
import io.imunity.rest.api.RestMultiGroupMembersWithAttributes;
import io.imunity.rest.api.types.basic.RestEntityWithAttributes;
import io.imunity.rest.api.types.basic.RestExternalizedAttribute;
import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestAttributeStatement;
import io.imunity.rest.api.types.basic.RestGroup;
import pl.edu.icm.unity.rest.exception.JSONParsingException;
import pl.edu.icm.unity.restadm.mappers.AttributeMapper;
import pl.edu.icm.unity.restadm.mappers.AttributeStatementMapper;
import pl.edu.icm.unity.restadm.mappers.EntityMapper;
import pl.edu.icm.unity.restadm.mappers.GroupContentsMapper;
import pl.edu.icm.unity.restadm.mappers.GroupMapper;
import pl.edu.icm.unity.restadm.mappers.GroupMemberWithAttributesMapper;
import pl.edu.icm.unity.restadm.mappers.GroupMembershipMapper;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.types.translation.TranslationRule;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RESTful API implementation.
 * 
 * @author K. Benedyczak
 */
@Produces(MediaType.APPLICATION_JSON)
@Path(RESTAdminEndpoint.V1_PATH)
@PrototypeComponent
public class RESTAdmin implements RESTAdminHandler
{
	private static final int UUID_LENGTH = 36;
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTAdmin.class);
	private EntityManagement identitiesMan;
	private GroupsManagement groupsMan;
	private AttributesManagementRESTService attributesService;
	private ObjectMapper mapper = Constants.MAPPER;
	private EmailConfirmationManager confirmationManager;
	private EndpointManagement endpointManagement;
	private RegistrationsManagement registrationManagement;
	private BulkProcessingManagement bulkProcessingManagement;
	private UserImportManagement userImportManagement;
	private EntityCredentialManagement entityCredMan;
	private AttributeTypeManagement attributeTypeMan;
	private InvitationManagement invitationMan;
	private EventPublisherWithAuthz eventPublisher;
	private SecuredTokensManagement securedTokenMan;
	private Token2JsonFormatter jsonFormatter;
	private UserNotificationTriggerer userNotificationTriggerer;
	private ExternalDataParser dataParser;
	private IdpStatisticManagement idpStatisticManagement;
	private GroupMembersService groupMembersService;

	@Autowired
	public RESTAdmin(EntityManagement identitiesMan,
			GroupsManagement groupsMan,
			AttributesManagementRESTService attributesService,
			EmailConfirmationManager confirmationManager,
			EndpointManagement endpointManagement,
			RegistrationsManagement registrationManagement,
			BulkProcessingManagement bulkProcessingManagement,
			UserImportManagement userImportManagement,
			EntityCredentialManagement entityCredMan,
			AttributeTypeManagement attributeTypeMan,
			InvitationManagement invitationMan,
			EventPublisherWithAuthz eventPublisher,
			SecuredTokensManagement securedTokenMan,
			Token2JsonFormatter jsonFormatter,
			UserNotificationTriggerer userNotificationTriggerer,
			ExternalDataParser dataParser,
			IdpStatisticManagement idpStatisticManagement,
			GroupMembersService groupMembersService)
	{
		this.identitiesMan = identitiesMan;
		this.groupsMan = groupsMan;
		this.attributesService = attributesService;
		this.confirmationManager = confirmationManager;
		this.endpointManagement = endpointManagement;
		this.registrationManagement = registrationManagement;
		this.bulkProcessingManagement = bulkProcessingManagement;
		this.userImportManagement = userImportManagement;
		this.entityCredMan = entityCredMan;
		this.attributeTypeMan = attributeTypeMan;
		this.invitationMan = invitationMan;
		this.eventPublisher = eventPublisher;
		this.securedTokenMan = securedTokenMan;
		this.jsonFormatter = jsonFormatter;
		this.userNotificationTriggerer = userNotificationTriggerer;
		this.dataParser = dataParser;
		this.idpStatisticManagement = idpStatisticManagement;
		this.groupMembersService = groupMembersService;
	}

	
	@Path("/resolve/{identityType}/{identityValue}")
	@GET
	public String getEntityObsolete(@PathParam("identityType") String identityType, 
			@PathParam("identityValue") String identityValue) 
			throws EngineException, JsonProcessingException
	{
		log.debug("resolve query for " + identityType + ":" + identityValue);
		Entity entity = identitiesMan.getEntity(new EntityParam(new IdentityTaV(identityType, identityValue)));
		return mapper.writeValueAsString(EntityMapper.map(entity));
	}

	
	@Path("/entity/{entityId}")
	@GET
	public String getEntity(@PathParam("entityId") String entityId, @QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.debug("getEntity query for " + entityId);
		Entity entity = identitiesMan.getEntity(getEP(entityId, idType));
		return mapper.writeValueAsString(EntityMapper.map(entity));
	}
	
	@Path("/entity/{entityId}")
	@DELETE
	public void removeEntity(@PathParam("entityId") String entityId, @QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.info("removeEntity of " + entityId);
		identitiesMan.removeEntity(getEP(entityId, idType));
	}

	@Path("/entity/{entityId}/removal-schedule")
	@PUT
	public void scheduleRemoval(@PathParam("entityId") String entityId, @QueryParam("when") long when, 
			@QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.info("scheduleRemovalByUser of " + entityId + " on " + when);
		Date time = new Date(when);
		identitiesMan.scheduleRemovalByUser(getEP(entityId, idType), time);
	}

	@Path("/entity/{entityId}/admin-schedule")
	@PUT
	public void scheduleOperation(@PathParam("entityId") String entityId, @QueryParam("when") long when,
			@QueryParam("operation") String operationStr, @QueryParam("identityType") String idType) 
			throws EngineException
	{
		log.info("scheduleEntityChange of " + entityId + " on " + when + " op " + operationStr);
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
		identitiesMan.scheduleEntityChange(getEP(entityId, idType), time, operation);
	}
	
	@Path("/entity/{entityId}/status/{status}")
	@PUT
	public void changeEntityStatus(@PathParam("entityId") String entityId, @PathParam("status") String status, 
			@QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.info("changeEntityStatus of " + entityId + " to " + status);
		EntityState newState;
		try
		{
			newState = EntityState.valueOf(status);
		} catch (Exception e)
		{
			throw new WrongArgumentException("Given status '" + status + "' is unknown, valid are: "
					+ Arrays.toString(EntityState.values()));
		}
		identitiesMan.setEntityStatus(getEP(entityId, idType), newState);
	}
	
	@Path("/entity/identity/{type}/{value}")
	@POST
	public String addEntity(@PathParam("type") String type, @PathParam("value") String value, 
			@QueryParam("credentialRequirement") String credReqIdId) 
			throws EngineException, JsonProcessingException
	{
		log.info("addEntity " + value + " type: " + type);
		Identity identity = identitiesMan.addEntity(dataParser.parseAsIdentity(type, value), 
				credReqIdId, EntityState.valid);
		ObjectNode ret = mapper.createObjectNode();
		ret.put("entityId", identity.getEntityId());
		return mapper.writeValueAsString(ret);
	}

	
	@Path("/entity/{entityId}/identity/{type}/{value}")
	@POST
	public void addIdentity(@PathParam("type") String type, @PathParam("value") String value, 
			@PathParam("entityId") String entityId, @QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.info("addIdentity of " + value + " type: " + type + " for entity: " + entityId);
		identitiesMan.addIdentity(dataParser.parseAsIdentity(type, value), getEP(entityId, idType));
	}

	@Path("/entity/identity/{type}/{value}")
	@DELETE
	public void removeIdentity(@PathParam("type") String type, @PathParam("value") String value,
			@QueryParam("target") String target, @QueryParam("realm") String realm) 
			throws EngineException, JsonProcessingException
	{
		log.info("removeIdentity of " + value + " type: " + type + " target: " + target + " realm: " + realm);
		identitiesMan.removeIdentity(new IdentityTaV(type, value, target, realm));
	}
	
	@Path("/entity/{entityId}/groups")
	@GET
	public String getGroups(@PathParam("entityId") String entityId, @QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.debug("getGroups query for " + entityId);
		Map<String, GroupMembership> groups = identitiesMan.getGroups(getEP(entityId, idType));
		return mapper.writeValueAsString(groups.keySet());
	}

	@Path("/entity/{entityId}/attributes")
	@GET
	public String getAttributes(@PathParam("entityId") String entityId,
			@QueryParam("group") String group,
			@QueryParam("effective") Boolean effective,
			@QueryParam("identityType") String idType,
			@QueryParam("includeSimpleValues") Boolean includeSimpleValues)
			throws EngineException, JsonProcessingException
	{
		if (group == null)
			group = "/";
		if (effective == null)
			effective = true;
		includeSimpleValues = includeSimpleValues == null ? false : includeSimpleValues;
		
		List<RestExternalizedAttribute> attributes = attributesService.getAttributes(
				getEP(entityId, idType), group, effective, idType, includeSimpleValues);
		return mapper.writeValueAsString(attributes);
	}

	@Path("/entity/{entityId}/groups/direct/attributes")
	@GET
	public String getAttributesInAllGroups(@PathParam("entityId") String entityId,
			@QueryParam("identityType") String idType)
			throws EngineException, JsonProcessingException
	{
		Map<String, List<RestExternalizedAttribute>> attributesInGroups = attributesService.getAllDirectAttributes(
				getEP(entityId, idType));
		return mapper.writeValueAsString(attributesInGroups);
	}

	@Path("/group-members-attributes/{groupPath}")
	@GET
	public String getGroupMembersWithSelectedAttributes(@PathParam("groupPath") String group,
			@QueryParam("attributes") List<String> attributes)
			throws JsonProcessingException
	{
		Stopwatch stopwatch = Stopwatch.createStarted();
		log.debug("getGroupMembersWithSelectedAttributes query for " + group);
		if (!group.startsWith("/"))
			group = "/" + group;
		List<RestGroupMemberWithAttributes> groupMembers = groupMembersService
				.getGroupMembersWithSelectedAttributes(group, attributes)
				.stream()
				.map(GroupMemberWithAttributesMapper::map)
				.collect(Collectors.toList());
		String s = mapper.writeValueAsString(groupMembers);
		log.debug("Request completed: {}", stopwatch.toString());
		return s;
	}

	@Path("/multi-group-members-attributes")
	@GET
	public String getMultiGroupsMembersInGroupsWithSelectedAttributes(@QueryParam("attributes") List<String> attributes,
	                                                @QueryParam("groups") List<String> groups)
			throws JsonProcessingException
	{
		log.debug("getMultiGroupsMembersInGroupsWithSelectedAttributes query for " + groups);
		if(groups.isEmpty()){
			return mapper.writeValueAsString(Map.of());
		}

		List<String> allGroups = groups.stream()
				.map(grp ->
				{
					if (!grp.startsWith("/"))
						return "/" + grp;
					return grp;
				}).collect(Collectors.toList());
		Map<String, List<RestGroupMemberWithAttributes>> groupMembers = groupMembersService.getGroupsMembersInGroupsWithSelectedAttributes(allGroups, attributes)
				.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> entry.getValue().stream()
								.map(GroupMemberWithAttributesMapper::map)
								.collect(Collectors.toList())
				));
		return mapper.writeValueAsString(RestMultiGroupMembersWithAttributes.builder().withMembers(groupMembers).build());
	}

	@Path("/entity/{entityId}/record")
	@GET
	public String getEntityWithAttributes(@PathParam("entityId") String entityId,
			@QueryParam("groupsPatterns") List<String> groupsPatterns,
			@QueryParam("effective") Boolean effective, @QueryParam("identityType") String idType)
			throws EngineException, JsonProcessingException
	{
		if (effective == null)
			effective = true;
		if (groupsPatterns == null || groupsPatterns.isEmpty())
			groupsPatterns = Arrays.asList("/**");

		EntityParam entityParam = getEP(entityId, idType);
		Entity entity = identitiesMan.getEntity(entityParam);
		Map<String, List<RestExternalizedAttribute>> attributesInGroups = attributesService
				.getAttributesInGroups(getEP(entityId, idType), effective, groupsPatterns);
		Map<String, GroupMembership> groups = identitiesMan.getGroups(getEP(entityId, idType));
		return mapper.writeValueAsString(new RestEntityWithAttributes(EntityMapper.map(entity), groups.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> GroupMembershipMapper.map(e.getValue()))),
				attributesInGroups));
	}
	
	@Path("/entity/{entityId}/groups/attributes")
	@GET
	public String getAttributesInGroups(@PathParam("entityId") String entityId,
			@QueryParam("groupsPatterns") List<String> groupsPatterns,
			@QueryParam("effective") Boolean effective,
			@QueryParam("identityType") String idType)
				throws EngineException, JsonProcessingException
	{
		if (effective == null)
			effective = true;
		Map<String, List<RestExternalizedAttribute>> attributesInGroups = attributesService.getAttributesInGroups(
			getEP(entityId, idType), effective, groupsPatterns);
		return mapper.writeValueAsString(attributesInGroups);
	}

	@Path("/entity/{entityId}/attribute/{attributeName}")
	@DELETE
	public void removeAttribute(@PathParam("entityId") String entityId, 
			@PathParam("attributeName") String attribute,
			@QueryParam("group") String group, 
			@QueryParam("identityType") String idType) 
					throws EngineException, JsonProcessingException
	{
		if (group == null)
			group = "/";
		attributesService.removeAttribute(getEP(entityId, idType), group, attribute);
	}
	
	@Path("/entity/{entityId}/attribute")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setAttribute(@PathParam("entityId") String entityId, String attribute, 
			@QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.info("setAttribute for " + entityId);
		RestAttribute attributeParam;
		try
		{
			attributeParam = mapper.readValue(attribute, RestAttribute.class);
		} catch (IOException e)
		{
			throw new JSONParsingException("Can't parse the attribute input", e);
		}
		attributesService.setAttribute(AttributeMapper.map(attributeParam), getEP(entityId, idType));
	}

	@Path("/entity/{entityId}/attributes")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setAttributes(@PathParam("entityId") String entityId, String attributes, 
			@QueryParam("identityType") String idType) 
			throws EngineException, IOException
	{
		log.info("Bulk setAttributes for " + entityId);
		
		JsonNode root = mapper.readTree(attributes);
		if (!root.isArray())
			throw new JSONParsingException("Can't parse the attributes input: root is not an array");
		ArrayNode rootA = (ArrayNode) root;
		List<Attribute> parsedParams = new ArrayList<>(rootA.size());
		for (JsonNode node: rootA)
		{
			try
			{
				parsedParams.add(mapper.readValue(mapper.writeValueAsString(node), 
						Attribute.class));
			} catch (IOException e)
			{
				throw new JSONParsingException("Can't parse the attribute input", e);
			}
		}
		EntityParam ep = getEP(entityId, idType);
		for (Attribute ap: parsedParams)
			attributesService.setAttribute(ap, ep);
	}

	//TODO - those two endpoints are duplicating functionality. Should be unified into a single one.
	//remaining after old method of providing used credential
	@Path("/entity/{entityId}/credential-adm/{credential}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setCredentialByAdmin(@PathParam("entityId") String entityId, 
			@PathParam("credential") String credential, 
			@QueryParam("identityType") String idType,
			String secrets) 
			throws EngineException, JsonProcessingException
	{
		log.info("setCredentialByAdmin for " + entityId);
		entityCredMan.setEntityCredential(getEP(entityId, idType), credential, secrets);
	}
	
	@Path("/entity/{entityId}/credential/{credential}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setCredentialByUser(@PathParam("entityId") String entityId, 
			@PathParam("credential") String credential, 
			@QueryParam("identityType") String idType,
			String secretsArray) 
			throws EngineException, JsonProcessingException
	{
		log.info("setCredentialByUser for " + entityId);
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
			entityCredMan.setEntityCredential(getEP(entityId, idType), credential, newSecrets);
		} else
		{
			throw new JSONParsingException("Request body must be a JSON array");
		}
	}

	@Path("/entity/{entityId}/credential/{credential}/status/{status}")
	@PUT
	public void setCredentialStatus(@PathParam("entityId") String entityId, 
			@PathParam("credential") String credential, 
			@QueryParam("identityType") String idType,
			@PathParam("status") String status) 
			throws EngineException, JsonProcessingException
	{
		log.info("setCredential {} status for {} to {}", credential, entityId, status);
		LocalCredentialState desiredCredentialState = LocalCredentialState.valueOf(status);
		entityCredMan.setEntityCredentialStatus(getEP(entityId, idType), credential, desiredCredentialState);
	}

	@Path("/entity/{entityId}/2f-optin/{status}")
	@PUT
	public void set2ndFactorOptIn(@PathParam("entityId") String entityId, 
			@PathParam("status") boolean status, 
			@QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.info("set2ndFactorOptIn for {} to {}", entityId, status);
		entityCredMan.setUserMFAOptIn(getEP(entityId, idType), status);
	}	

	@Path("/entity/{entityId}/2f-optin")
	@GET
	public String get2ndFactorOptIn(@PathParam("entityId") String entityId, 
			@QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.debug("get2ndFactorOptIn for {}", entityId);
		boolean userMFAOptIn = entityCredMan.getUserMFAOptIn(getEP(entityId, idType));
		return String.valueOf(userMFAOptIn);
	}	


	@Path("/group/{groupPath}/meta")
	@GET
	public String getGroupMeta(@PathParam("groupPath") String group) 
			throws EngineException, JsonProcessingException
	{
		log.debug("getGroupMeta query for {}", group);
		if (!group.startsWith("/"))
			group = "/" + group;
		GroupContents contents = groupsMan.getContents(group, GroupContents.METADATA);
		return mapper.writeValueAsString(GroupMapper.map(contents.getGroup()));
	}
	
	@Path("/group/{groupPath}")
	@GET
	public String getGroupContents(@PathParam("groupPath") String group) 
			throws EngineException, JsonProcessingException
	{
		log.debug("getGroupContents query for " + group);
		if (!group.startsWith("/"))
			group = "/" + group;
		GroupContents contents = groupsMan.getContents(group, GroupContents.GROUPS | GroupContents.MEMBERS);
		return mapper.writeValueAsString(GroupContentsMapper.map(contents));
	}
	
	@Path("/group/{groupPath}")
	@DELETE
	public void removeGroup(@PathParam("groupPath") String group, 
			@QueryParam("recursive") Boolean recursive) throws EngineException, JsonProcessingException
	{
		if (recursive == null)
			recursive = false;
		if (!group.startsWith("/"))
			group = "/" + group;
		log.info("removeGroup " + group + (recursive ? " [recursive]" : ""));
		groupsMan.removeGroup(group, recursive);
	}

	@Path("/group")
	@POST
	public void addInitializedGroup(String groupJson) throws EngineException, JsonProcessingException
	{
		log.info("addInitializedGroup {}", groupJson);
		RestGroup parsedGroup = JsonUtil.parse(groupJson, RestGroup.class);
		groupsMan.addGroup(GroupMapper.map(parsedGroup));
	}

	@Path("/group")
	@PUT
	public void updateGroup(String groupJson) throws EngineException, JsonProcessingException
	{
		log.info("updateGroup {}", groupJson);
		RestGroup parsedGroup = JsonUtil.parse(groupJson, RestGroup.class);
		groupsMan.updateGroup(parsedGroup.path,GroupMapper.map(parsedGroup));
	}
	
	@Path("/group/{groupPath}")
	@POST
	public void addGroup(@PathParam("groupPath") String group,
			@QueryParam("withParents") Boolean withParents) throws EngineException, JsonProcessingException
	{
		log.info("addGroup " + group);
		Group toAdd = new Group(group);
		if (withParents == null)
			withParents = false;
		groupsMan.addGroup(toAdd, withParents);
	}
	
	@Path("/groups")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void addGroups(String groups) throws EngineException, JsonProcessingException
	{
		Set<RestGroup> groupsToAdd;
		try
		{
			groupsToAdd = Constants.MAPPER.readValue(groups, 
					new TypeReference<Set<RestGroup>>() {});
		} catch (IOException e)
		{
			throw new WrongArgumentException("Can not parse request body as a list of groups", e);
		}
		
		groupsMan.addGroups(groupsToAdd.stream().map(GroupMapper::map).collect(Collectors.toSet()));	
	}

	@Path("/group/{groupPath}/statements")
	@GET
	public String getGroupStatements(@PathParam("groupPath") String group) 
			throws EngineException, JsonProcessingException
	{
		if (!group.startsWith("/"))
			group = "/" + group;
		log.debug("getGroupStatements query for " + group);
		GroupContents contents = groupsMan.getContents(group, GroupContents.METADATA);
		return mapper.writeValueAsString(Stream.of(contents.getGroup()
				.getAttributeStatements())
				.map(AttributeStatementMapper::map)
				.collect(Collectors.toList()).toArray(new RestAttributeStatement[contents.getGroup()
				                                             				.getAttributeStatements().length]));
	}

	@Path("/group/{groupPath}/statements")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateGroupStatements(@PathParam("groupPath") String group,
			String statementsJson) throws EngineException, JsonProcessingException
	{
		if (!group.startsWith("/"))
			group = "/" + group;
		log.info("updateGroup statements " + group);
		
		List<RestAttributeStatement> statements;
		try
		{
			statements = Constants.MAPPER.readValue(statementsJson, 
					new TypeReference<List<RestAttributeStatement>>() {});
		} catch (IOException e)
		{
			throw new WrongArgumentException("Can not parse input as list of attribute statements", e);
		}
		
		Group contents = groupsMan.getContents(group, GroupContents.METADATA).getGroup();
		contents.setAttributeStatements(statements.stream().map(s -> AttributeStatementMapper.map(s)).collect(Collectors.toList()).toArray(new AttributeStatement[statements.size()]));
		groupsMan.updateGroup(group, contents, "set group statement", contents.getAttributeStatements().toString());
	}

	
	
	@Path("/group/{groupPath}/entity/{entityId}")
	@DELETE
	public void removeMember(@PathParam("groupPath") String group, 
			@PathParam("entityId") String entityId, 
			@QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		if (!group.startsWith("/"))
			group = "/" + group;
		log.info("removeMember " + entityId + " from " + group);
		groupsMan.removeMember(group, getEP(entityId, idType));
	}
	
	@Path("/group/{groupPath}/entity/{entityId}")
	@POST
	public void addMember(@PathParam("groupPath") String group, 
			@PathParam("entityId") String entityId, 
			@QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		if (!group.startsWith("/"))
			group = "/" + group;
		log.info("addMember " + entityId + " to " + group);
		
		EntityParam entityParam = getEP(entityId, idType);
		
		Set<String> existingGroups = identitiesMan.getGroups(entityParam).keySet();
		Deque<String> notMember = Group.getMissingGroups(group, existingGroups);
		while (!notMember.isEmpty())
		{
			String notMemberGroup = notMember.pollLast();
			groupsMan.addMemberFromParent(notMemberGroup, entityParam);
		}
	}

	
	
	@Path("/attributeTypes")
	@GET
	public String getAttributeTypes() throws EngineException, JsonProcessingException
	{
		Collection<AttributeType> attributeTypes = attributeTypeMan.getAttributeTypes();
		return mapper.writeValueAsString(attributeTypes);
	}
	
	@Path("/attributeType")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void addAttributeType(String jsonRaw) throws EngineException
	{
		log.debug("addAttributeType " + jsonRaw);
		AttributeType at = JsonUtil.parse(jsonRaw, AttributeType.class);
		log.info("addAttributeType " + at.getName());
		attributeTypeMan.addAttributeType(at);
	}

	@Path("/attributeType")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateAttributeType(String jsonRaw) throws EngineException
	{
		log.debug("updateAttributeType " + jsonRaw);
		AttributeType at = JsonUtil.parse(jsonRaw, AttributeType.class);
		log.info("updateAttributeType " + at.getName());
		attributeTypeMan.updateAttributeType(at);
	}
	
	@Path("/attributeType/{toRemove}")
	@DELETE
	public void removeAttributeType(@PathParam("toRemove") String toRemove, 
			@QueryParam("withInstances") String withInstances) throws EngineException
	{
		log.info("removeAttributeType " + toRemove);
		boolean instances = false;
		if (withInstances != null)
			instances = Boolean.parseBoolean(withInstances);
		attributeTypeMan.removeAttributeType(toRemove, instances);
	}

	@Path("/confirmation-trigger/entity/{entityId}/attribute/{attributeName}")
	@POST
	public void resendConfirmationForAttribute(@PathParam("entityId") String entityId, 
			@PathParam("attributeName") String attribute,
			@QueryParam("group") String group, 
			@QueryParam("identityType") String idType) throws EngineException, JsonProcessingException
	{
		if (group == null)
			group = "/";
		log.info("confirmation trigger for " + attribute + " of " + entityId + " in " + group);
		EntityParam entityParam = getEP(entityId, idType);
		Collection<AttributeExt> attributes = attributesService.getAttributes(entityParam, group, attribute);
		
		if (attributes.isEmpty())
			throw new WrongArgumentException("Attribute is undefined");
		
		confirmationManager.sendVerificationsQuietNoTx(entityParam, attributes, true);
	}
	
	@Path("/userNotification-trigger/entity/{identityValue}/template/{templateId}")
	@POST
	public void userNotificationTrigger(
			@PathParam("identityValue") String identityValue, 
			@PathParam("templateId") String templateId, 
			@QueryParam("identityType") String identityType, 
			@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.info("Triggering UserNotification \'{}\' for identity {} type {}", 
				templateId,  identityValue,  identityType);
		EntityParam entityParam = getEP(identityValue, identityType);
		Entity entity = identitiesMan.getEntity(entityParam);
		
		Map<String, String> customTemplateParams = Maps.newHashMap();
		uriInfo.getQueryParameters().forEach((key, value) -> 
		{
			if (key.startsWith(MessageTemplateDefinition.CUSTOM_VAR_PREFIX))
			{
				String flatValue = value.stream().collect(Collectors.joining());
				customTemplateParams.put(key, flatValue);
			}
		});
		userNotificationTriggerer.sendNotification(entity, templateId, customTemplateParams);
	}

	@Path("/confirmation-trigger/identity/{type}/{value}")
	@POST
	public void resendConfirmationForIdentity(@PathParam("type") String idType, 
			@PathParam("value") String value) throws EngineException, JsonProcessingException
	{
		log.info("confirmation trigger for " + idType + ": " + value);
		EntityParam entityParam = new EntityParam(new IdentityTaV(idType, value));
		Entity entity = identitiesMan.getEntity(entityParam);
		for (Identity id: entity.getIdentities())
			if (id.getTypeId().equals(idType) && id.getValue().equals(value))
			{
				confirmationManager.sendVerificationNoTx(entityParam, id, true);
				return;
			}

		throw new WrongArgumentException("Identity is unknown");
	}
	
	
	@Path("/endpoints")
	@GET
	public String getEndpoints() throws EngineException, JsonProcessingException
	{
		List<ResolvedEndpoint> endpoints = endpointManagement.getDeployedEndpoints();
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
		ResolvedEndpoint deployed = endpointManagement.deploy(typeId, id, address, configuration);
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
	
	@Path("/registrationForm/{formId}")
	@DELETE
	public void removeRegistrationForm(@PathParam("formId") String formId, 
			@QueryParam("dropRequests") Boolean dropRequests) throws EngineException
	{
		if (dropRequests == null)
			dropRequests = false;
		registrationManagement.removeForm(formId, dropRequests);
	}
	
	@Path("/registrationForm")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void addForm(String json) throws EngineException, IOException
	{
		RegistrationForm form = new RegistrationForm(JsonUtil.parse(json));
		registrationManagement.addForm(form);
	}
	
	@Path("/registrationForm")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateForm(@QueryParam("ignoreRequestsAndInvitations") Boolean ignoreRequestsAndInvitations,
			@QueryParam("ignoreInvitations") Boolean ignoreInvitations, String json)
			throws EngineException, IOException
	{
		if (ignoreRequestsAndInvitations == null)
			ignoreRequestsAndInvitations = false;
		RegistrationForm form = new RegistrationForm(JsonUtil.parse(json));
		registrationManagement.updateForm(form, ignoreRequestsAndInvitations);
	}
	
	@Path("/registrationRequests")
	@GET
	public String getRegistrationRequests() throws EngineException, JsonProcessingException
	{
		List<RegistrationRequestState> requests = registrationManagement.getRegistrationRequests();
		return mapper.writeValueAsString(requests);
	}
	
	@Path("/registrationRequest/{requestId}")
	@GET
	public String getRegistrationRequest(@PathParam("requestId") String requestId) 
			throws EngineException, JsonProcessingException
	{
		List<RegistrationRequestState> requests = registrationManagement.getRegistrationRequests();
		Optional<RegistrationRequestState> request = requests.stream().
				filter(r -> r.getRequestId().equals(requestId)).
				findAny();
		if (!request.isPresent())
			throw new WrongArgumentException("There is no request with id " + requestId);
		return mapper.writeValueAsString(request.get());
	}
	
	@Path("/invitations")
	@GET
	public String getInvitations() throws EngineException, JsonProcessingException
	{
		List<InvitationWithCode> invitations = invitationMan.getInvitations();
		return mapper.writeValueAsString(invitations);
	}

	@Path("/invitation/{code}")
	@GET
	public String getInvitation(@PathParam("code") String code) throws EngineException, JsonProcessingException
	{
		InvitationWithCode invitation = invitationMan.getInvitation(code);
		return mapper.writeValueAsString(invitation);
	}
	
	@Path("/invitation/{code}")
	@DELETE
	public void removeInvitation(@PathParam("code") String code) throws EngineException
	{
		invitationMan.removeInvitation(code);
	}

	@Path("/invitation/{code}/send")
	@POST
	public void sendInvitation(@PathParam("code") String code) throws EngineException, IOException
	{
		invitationMan.sendInvitation(code);
	}

	@Path("/invitation")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String addInvitation(String jsonInvitation) throws EngineException, IOException
	{	
		InvitationParam invitationParam = getInvitationFromJson(jsonInvitation);
		return invitationMan.addInvitation(invitationParam);
	}
	
	@Path("/invitation/{code}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void updateInvitation(@PathParam("code") String code, String jsonInvitation) throws EngineException, IOException
	{
		InvitationParam invitationParam = getInvitationFromJson(jsonInvitation);
		invitationMan.updateInvitation(code, invitationParam);
	}	
	
	private InvitationParam getInvitationFromJson(String jsonInvitation) throws WrongArgumentException
	{
		ObjectNode invNode = JsonUtil.parse(jsonInvitation);
		JsonNode itype = invNode.get("type");
		InvitationType type = null;
		if (itype == null)
		{
			type = InvitationType.REGISTRATION;
			log.debug("Use default invitation type = " + InvitationType.REGISTRATION.toString());
		} else
		{
			type = InvitationType.valueOf(invNode.get("type").asText());
		}

		if (type.equals(InvitationType.REGISTRATION))
		{
			return new RegistrationInvitationParam(invNode);
		} else
		{
			return new EnquiryInvitationParam(invNode);
		}
	}
	
	@Path("/bulkProcessing/instant")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String applyBulkProcessingRule(@QueryParam("timeout") Long timeout, String jsonProcessingRule) 
			throws EngineException
	{
		TranslationRule rule = JsonUtil.parse(jsonProcessingRule, TranslationRule.class); 
		
		if (timeout == null)
			timeout = -1l;
		
		if (timeout < 0)
		{
			bulkProcessingManagement.applyRule(rule);
			return "async";
		} else
		{
			try
			{
				bulkProcessingManagement.applyRuleSync(rule, timeout);
				return "sync";
			} catch (TimeoutException e)
			{
				return "timeout";
			}
		}
	}
	
	@Path("/import/user/{identity}")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public String importUser(@PathParam("identity") String identity,
			@QueryParam("type") String identityType,
			@QueryParam("importer") String importer) throws EngineException, IOException
	{
		UserImportSpec param = importer == null ? 
				UserImportSpec.withAllImporters(identity, identityType) : 
				new UserImportSpec(importer, identity, identityType);
		List<ImportResult> importUser = userImportManagement.importUser(
				Lists.newArrayList(param));
		return mapper.writeValueAsString(importUser);
	}
	
	@Path("/triggerEvent/{eventName}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void triggerEvent(@PathParam("eventName") String eventName, String eventBody) 
			throws EngineException, IOException
	{
		PersistableEvent event = new PersistableEvent(eventName, -1l, new Date(), eventBody);
		eventPublisher.fireEventWithAuthz(event);
	}	
	
	@Path("/token/{type}/{value}")
	@DELETE
	public void removeToken(@PathParam("type") String type, 
			@PathParam("value") String value) throws EngineException, JsonProcessingException
	{
		log.info("remove token " + type + ":" + value);
		try{
			securedTokenMan.removeToken(type, value);
		} catch (EngineException e) {
			log.error("Cannot remove token", e);
			throw new EngineException("Cannot remove token - invalid token");
		}
	}
	
	@Path("/tokens")
	@GET
	public String getTokens(@QueryParam("type") String type, @QueryParam("owner") String entity,
			@QueryParam("ownerType") String entityType)
			throws EngineException, JsonProcessingException
	{	
		Collection<Token> tokens;
		try
		{
			if (entity != null)
				tokens = securedTokenMan.getOwnedTokens(type,
						getEP(entity, entityType));
			else
				tokens = securedTokenMan.getAllTokens(type);
		} catch (EngineException e)
		{
			log.error("Cannot get tokens", e);
			throw new EngineException("Cannot get tokens - invalid type or owner");
		}
		
		ArrayNode jsonArray = mapper.createArrayNode();
		for (Token t : tokens)
			jsonArray.add(jsonFormatter.toJson(t));
		return mapper.writeValueAsString(jsonArray);
	}
	
	@Path("/idp-stats")
	@GET
	public String getIdpStatistics(@QueryParam("since") long since, @QueryParam("groupBy") String groupBy, 
			@QueryParam("skipZeroRecords") Boolean skipZeroRecords)
			throws EngineException, JsonProcessingException
	{
		LocalDateTime sinceDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(since), ZoneId.systemDefault());
		GroupBy groupByFallbackToTotal = groupBy != null ? GroupBy.valueOf(groupBy) : GroupBy.none;
		if (skipZeroRecords == null)
			skipZeroRecords = true;
		return mapper.writeValueAsString(idpStatisticManagement.getIdpStatisticsSinceGroupBy(sinceDate,
				groupByFallbackToTotal, IdpStatisticManagement.DEFAULT_SIG_IN_RECORD_LIMIT, skipZeroRecords));
	}

	/**
	 * Creates {@link EntityParam} from given entity address and optional
	 * type, which can be null. If type is null then entityId is checked to
	 * have the size of persistentId type and if matching then persistentId
	 * type is used. Otherwise it is assumed to be internal entityId - a
	 * long number. If type is not null then it is used as is.
	 * 
	 * @param identity
	 * @param idType
	 * @return
	 * @throws WrongArgumentException
	 */
	private EntityParam getEP(String identity, String idType) throws WrongArgumentException
	{
		if (idType == null)
		{
			if (identity.length() == UUID_LENGTH) 
				return new EntityParam(new IdentityTaV(PersistentIdentity.ID, identity));
			
			try
			{
				return new EntityParam(Long.valueOf(identity));
			} catch (NumberFormatException e)
			{
				throw new WrongArgumentException("When addressing identity either it must be "
						+ "a persistent ID or internal entityId (integer number) or"
						+ "identity type must be provided. "
						+ "The provided identifier is neither entityId nor persistentId "
						+ "and type was not given: " + identity, e);
			}
		} else
		{
			return new EntityParam(new IdentityTaV(idType, identity));
		}
	}
}




