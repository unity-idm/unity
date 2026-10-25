/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.imunity.rest.admin.AdminJsonBody;
import io.imunity.rest.admin.AdminJsonProvider;
import io.imunity.rest.admin.RestEntityId;
import io.imunity.rest.admin.RestFormattedToken;
import io.imunity.rest.api.RestGroupMemberWithAttributes;
import io.imunity.rest.api.RestMultiGroupMembersWithAttributes;
import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestAttributeStatement;
import io.imunity.rest.api.types.basic.RestAttributeType;
import io.imunity.rest.api.types.basic.RestEntity;
import io.imunity.rest.api.types.basic.RestEntityWithAttributes;
import io.imunity.rest.api.types.basic.RestExternalizedAttribute;
import io.imunity.rest.api.types.basic.RestGroup;
import io.imunity.rest.api.types.basic.RestGroupContents;
import io.imunity.rest.api.types.basic.RestToken;
import io.imunity.rest.api.types.endpoint.RestEndpointConfiguration;
import io.imunity.rest.api.types.endpoint.RestResolvedEndpoint;
import io.imunity.rest.api.types.idp.statistic.RestGroupedIdpStatistic;
import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.registration.RestEnquiryResponseState;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.rest.api.types.registration.RestRegistrationRequestState;
import io.imunity.rest.api.types.registration.invite.RestEnquiryInvitationParam;
import io.imunity.rest.api.types.registration.invite.RestInvitationParam;
import io.imunity.rest.api.types.registration.invite.RestInvitationWithCode;
import io.imunity.rest.api.types.registration.invite.RestRegistrationInvitationParam;
import io.imunity.rest.api.types.translation.RestTranslationRule;
import io.imunity.rest.api.types.userimport.RestImportResult;
import io.imunity.rest.mappers.AttributeMapper;
import io.imunity.rest.mappers.AttributeStatementMapper;
import io.imunity.rest.mappers.AttributeTypeMapper;
import io.imunity.rest.mappers.EntityMapper;
import io.imunity.rest.mappers.GroupContentsMapper;
import io.imunity.rest.mappers.GroupMapper;
import io.imunity.rest.mappers.GroupMemberWithAttributesMapper;
import io.imunity.rest.mappers.GroupMembershipMapper;
import io.imunity.rest.mappers.TokenMapper;
import io.imunity.rest.mappers.endpoint.EndpointConfigurationMapper;
import io.imunity.rest.mappers.endpoint.ResolvedEndpointMapper;
import io.imunity.rest.mappers.idp.statistic.GroupedIdpStatisticMapper;
import io.imunity.rest.mappers.registration.EnquiryFormMapper;
import io.imunity.rest.mappers.registration.EnquiryResponseStateMapper;
import io.imunity.rest.mappers.registration.RegistrationFormMapper;
import io.imunity.rest.mappers.registration.RegistrationRequestStateMapper;
import io.imunity.rest.mappers.registration.invite.InvitationParamMapper;
import io.imunity.rest.mappers.registration.invite.InvitationWithCodeMapper;
import io.imunity.rest.mappers.translation.TranslationRuleMapper;
import io.imunity.rest.mappers.userimport.ImportResultMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.UserImportManagement;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.event.EventPublisherWithAuthz;
import pl.edu.icm.unity.engine.api.groupMember.GroupMembersService;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce.ImportResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.rest.exception.JSONParsingException;
import pl.edu.icm.unity.restadm.token.Token2JsonFormatter;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
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
	@Context
	private HttpHeaders httpHeaders;

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
	private EnquiryManagement enquiryManagement;
	
	
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
			GroupMembersService groupMembersService,
			EnquiryManagement enquiryManagement)
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
		this.enquiryManagement = enquiryManagement;
	}

	
	@Path("/resolve/{identityType}/{identityValue}")
	@GET
	@Operation(operationId = "getEntityObsolete")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestEntity getEntityObsolete(@PathParam("identityType") String identityType,
			@PathParam("identityValue") String identityValue) 
			throws EngineException, JsonProcessingException
	{
		log.debug("resolve query for " + identityType + ":" + identityValue);
		Entity entity = identitiesMan.getEntity(new EntityParam(new IdentityTaV(identityType, identityValue)));
		return EntityMapper.map(entity);
	}

	
	@Path("/entity/{entityId}")
	@GET
	@Operation(operationId = "getEntity")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestEntity getEntity(@PathParam("entityId") String entityId, @QueryParam("identityType") String idType)
			throws EngineException, JsonProcessingException
	{
		log.debug("getEntity query for " + entityId);
		Entity entity = identitiesMan.getEntity(getEP(entityId, idType));
		return EntityMapper.map(entity);
	}
	
	@Path("/entity/{entityId}")
	@DELETE
	@Operation(operationId = "removeEntity")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeEntity(@PathParam("entityId") String entityId, @QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.info("removeEntity of " + entityId);
		identitiesMan.removeEntity(getEP(entityId, idType));
	}

	@Path("/entity/{entityId}/removal-schedule")
	@PUT
	@Operation(operationId = "scheduleRemoval")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "scheduleOperation")
	@ApiResponse(responseCode = "204", description = "Success")
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
	
	@Path("/entity/{entityId}/admin-schedule")
	@DELETE
	@Operation(operationId = "clearScheduledOperation")
	@ApiResponse(responseCode = "204", description = "Success")
	public void clearScheduledOperation(@PathParam("entityId") String entityId, @QueryParam("identityType") String idType) 
			throws EngineException
	{
		log.info("clear scheduleEntityChange of " + entityId );
		
		EntityParam ep = getEP(entityId, idType);
		Entity entity = identitiesMan.getEntity(ep);
		if (entity.getState().equals(EntityState.onlyLoginPermitted))
		{
			identitiesMan.clearScheduledRemovalStatus(ep);
		}
		
		identitiesMan.scheduleEntityChange(ep, null, null);
	}
	
	@Path("/entity/{entityId}/status/{status}")
	@PUT
	@Operation(operationId = "changeEntityStatus")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "addEntity")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestEntityId addEntity(@PathParam("type") String type, @PathParam("value") String value,
			@QueryParam("credentialRequirement") String credReqIdId) 
			throws EngineException, JsonProcessingException
	{
		log.info("addEntity " + value + " type: " + type);
		Identity identity = identitiesMan.addEntity(dataParser.parseAsIdentity(type, value), 
				credReqIdId, EntityState.valid);
		return new RestEntityId(identity.getEntityId());
	}

	
	@Path("/entity/{entityId}/identity/{type}/{value}")
	@POST
	@Operation(operationId = "addIdentity")
	@ApiResponse(responseCode = "204", description = "Success")
	public void addIdentity(@PathParam("type") String type, @PathParam("value") String value, 
			@PathParam("entityId") String entityId, @QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.info("addIdentity of " + value + " type: " + type + " for entity: " + entityId);
		identitiesMan.addIdentity(dataParser.parseAsIdentity(type, value), getEP(entityId, idType));
	}

	@Path("/entity/identity/{type}/{value}")
	@DELETE
	@Operation(operationId = "removeIdentity")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeIdentity(@PathParam("type") String type, @PathParam("value") String value,
			@QueryParam("target") String target, @QueryParam("realm") String realm) 
			throws EngineException, JsonProcessingException
	{
		log.info("removeIdentity of " + value + " type: " + type + " target: " + target + " realm: " + realm);
		identitiesMan.removeIdentity(new IdentityTaV(type, value, target, realm));
	}
	
	@Path("/entity/{entityId}/groups")
	@GET
	@Operation(operationId = "getGroups")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public Set<String> getGroups(@PathParam("entityId") String entityId, @QueryParam("identityType") String idType)
			throws EngineException, JsonProcessingException
	{
		log.debug("getGroups query for " + entityId);
		Map<String, GroupMembership> groups = identitiesMan.getGroups(getEP(entityId, idType));
		return groups.keySet();
	}

	@Path("/entity/{entityId}/attributes")
	@GET
	@Operation(operationId = "getAttributes")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestExternalizedAttribute> getAttributes(@PathParam("entityId") String entityId,
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
		return attributes;
	}

	@Path("/entity/{entityId}/groups/direct/attributes")
	@GET
	@Operation(operationId = "getAttributesInAllGroups")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public Map<String, List<RestExternalizedAttribute>> getAttributesInAllGroups(@PathParam("entityId") String entityId,
			@QueryParam("identityType") String idType)
			throws EngineException, JsonProcessingException
	{
		Map<String, List<RestExternalizedAttribute>> attributesInGroups = attributesService.getAllDirectAttributes(
				getEP(entityId, idType));
		return attributesInGroups;
	}

	@Path("/group-members-attributes/{groupPath}")
	@GET
	@Operation(operationId = "getGroupMembersWithSelectedAttributes")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestGroupMemberWithAttributes> getGroupMembersWithSelectedAttributes(@PathParam("groupPath") String group,
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

		log.debug("Request completed: {}", stopwatch.toString());
		return groupMembers;
	}

	@Path("/multi-group-members-attributes")
	@GET
	@Operation(operationId = "getMultiGroupsMembersInGroupsWithSelectedAttributes")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestMultiGroupMembersWithAttributes getMultiGroupsMembersInGroupsWithSelectedAttributes(
			@QueryParam("attributes") List<String> attributes,
	                                                @QueryParam("groups") List<String> groups)
			throws JsonProcessingException
	{
		log.debug("getMultiGroupsMembersInGroupsWithSelectedAttributes query for " + groups);
		if (groups.isEmpty())
		{
			return RestMultiGroupMembersWithAttributes.builder().build();
		}

		List<String> allGroups = groups.stream()
				.map(grp ->
				{
					if (!grp.startsWith("/"))
						return "/" + grp;
					return grp;
				}).collect(Collectors.toList());
		Map<String, List<RestGroupMemberWithAttributes>> groupMembers = groupMembersService
				.getGroupsMembersInGroupsWithSelectedAttributes(allGroups, attributes)
				.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> entry.getValue().stream()
								.map(GroupMemberWithAttributesMapper::map)
								.collect(Collectors.toList())
				));
		return RestMultiGroupMembersWithAttributes.builder().withMembers(groupMembers).build();
	}

	@Path("/entity/{entityId}/record")
	@GET
	@Operation(operationId = "getEntityWithAttributes")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestEntityWithAttributes getEntityWithAttributes(@PathParam("entityId") String entityId,
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
		return new RestEntityWithAttributes(EntityMapper.map(entity), groups.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> GroupMembershipMapper.map(e.getValue()))),
				attributesInGroups);
	}
	
	@Path("/entity/{entityId}/groups/attributes")
	@GET
	@Operation(operationId = "getAttributesInGroups")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public Map<String, List<RestExternalizedAttribute>> getAttributesInGroups(@PathParam("entityId") String entityId,
			@QueryParam("groupsPatterns") List<String> groupsPatterns,
			@QueryParam("effective") Boolean effective,
			@QueryParam("identityType") String idType)
				throws EngineException, JsonProcessingException
	{
		if (effective == null)
			effective = true;
		Map<String, List<RestExternalizedAttribute>> attributesInGroups = attributesService.getAttributesInGroups(
			getEP(entityId, idType), effective, groupsPatterns);
		return attributesInGroups;
	}

	@Path("/entity/{entityId}/attribute/{attributeName}")
	@DELETE
	@Operation(operationId = "removeAttribute")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "setAttribute")
	@ApiResponse(responseCode = "204", description = "Success")
	public void setAttribute(@PathParam("entityId") String entityId,
			@RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
				schema = @Schema(implementation = RestAttribute.class))) InputStream attributeInput,
			@QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		String attribute = readBody(attributeInput);
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
	@Operation(operationId = "setAttributes")
	@ApiResponse(responseCode = "204", description = "Success")
	public void setAttributes(@PathParam("entityId") String entityId,
			@RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
				array = @ArraySchema(schema = @Schema(implementation = RestAttribute.class)))) InputStream attributesInput,
			@QueryParam("identityType") String idType) 
			throws EngineException, IOException
	{
		String attributes = readBody(attributesInput);
		log.info("Bulk setAttributes for " + entityId);
		
		JsonNode root = mapper.readTree(attributes);
		if (!root.isArray())
			throw new JSONParsingException("Can't parse the attributes input: root is not an array");
		ArrayNode rootA = (ArrayNode) root;
		List<RestAttribute> parsedParams = new ArrayList<>(rootA.size());
		for (JsonNode node: rootA)
		{
			try
			{
				parsedParams.add(mapper.readValue(mapper.writeValueAsString(node), 
						RestAttribute.class));
			} catch (IOException e)
			{
				throw new JSONParsingException("Can't parse the attribute input", e);
			}
		}
		EntityParam ep = getEP(entityId, idType);
		for (Attribute ap: parsedParams.stream().map(AttributeMapper::map).collect(Collectors.toList()))
			attributesService.setAttribute(ap, ep);
	}

	//TODO - those two endpoints are duplicating functionality. Should be unified into a single one.
	//remaining after old method of providing used credential
	@Path("/entity/{entityId}/credential-adm/{credential}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "setCredentialByAdmin")
	@ApiResponse(responseCode = "204", description = "Success")
	public void setCredentialByAdmin(@PathParam("entityId") String entityId, 
			@PathParam("credential") String credential, 
			@QueryParam("identityType") String idType,
			@RequestBody(required = true, description = "Raw payload interpreted by the configured plugin",
				content = @Content(mediaType = MediaType.APPLICATION_JSON,
						schema = @Schema(type = "string", format = "binary"))) String secrets)
			throws EngineException, JsonProcessingException
	{
		log.info("setCredentialByAdmin for " + entityId);
		entityCredMan.setEntityCredential(getEP(entityId, idType), credential, secrets);
	}
	
	@Path("/entity/{entityId}/credential/{credential}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "setCredentialByUser")
	@ApiResponse(responseCode = "204", description = "Success")
	public void setCredentialByUser(@PathParam("entityId") String entityId, 
			@PathParam("credential") String credential, 
			@QueryParam("identityType") String idType,
			@RequestBody(required = true, description = "Array whose first value contains the new credential secret",
				content = @Content(mediaType = MediaType.APPLICATION_JSON,
						array = @ArraySchema(minItems = 1, schema = @Schema(type = "string")))) String secretsArray)
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
	@Operation(operationId = "setCredentialStatus")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "set2ndFactorOptIn")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "get2ndFactorOptIn")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public boolean get2ndFactorOptIn(@PathParam("entityId") String entityId,
			@QueryParam("identityType") String idType) 
			throws EngineException, JsonProcessingException
	{
		log.debug("get2ndFactorOptIn for {}", entityId);
		boolean userMFAOptIn = entityCredMan.getUserMFAOptIn(getEP(entityId, idType));
		return userMFAOptIn;
	}	


	@Path("/group/{groupPath}/meta")
	@GET
	@Operation(operationId = "getGroupMeta")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestGroup getGroupMeta(@PathParam("groupPath") String group)
			throws EngineException, JsonProcessingException
	{
		log.debug("getGroupMeta query for {}", group);
		if (!group.startsWith("/"))
			group = "/" + group;
		GroupContents contents = groupsMan.getContents(group, GroupContents.METADATA);
		return GroupMapper.map(contents.getGroup());
	}
	
	@Path("/group/{groupPath}")
	@GET
	@Operation(operationId = "getGroupContents")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestGroupContents getGroupContents(@PathParam("groupPath") String group)
			throws EngineException, JsonProcessingException
	{
		log.debug("getGroupContents query for " + group);
		if (!group.startsWith("/"))
			group = "/" + group;
		GroupContents contents = groupsMan.getContents(group, GroupContents.GROUPS | GroupContents.MEMBERS);
		return GroupContentsMapper.map(contents);
	}
	
	@Path("/group/{groupPath}")
	@DELETE
	@Operation(operationId = "removeGroup")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "addInitializedGroup")
	@ApiResponse(responseCode = "204", description = "Success")
	public void addInitializedGroup(RestGroup parsedGroup) throws EngineException, JsonProcessingException
	{
		log.info("addInitializedGroup {}", parsedGroup);
		groupsMan.addGroup(GroupMapper.map(parsedGroup));
	}

	@Path("/group")
	@PUT
	@Operation(operationId = "updateGroup")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateGroup(RestGroup parsedGroup) throws EngineException, JsonProcessingException
	{
		log.info("updateGroup {}", parsedGroup);
		groupsMan.updateGroup(parsedGroup.path,GroupMapper.map(parsedGroup));
	}
	
	@Path("/group/{groupPath}")
	@POST
	@Operation(operationId = "addGroup")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "addGroups")
	@ApiResponse(responseCode = "204", description = "Success")
	public void addGroups(@RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
				array = @ArraySchema(schema = @Schema(implementation = RestGroup.class)))) InputStream groupsInput)
			throws EngineException, JsonProcessingException
	{
		String groups = readBody(groupsInput);
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
	@Operation(operationId = "getGroupStatements")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestAttributeStatement[] getGroupStatements(@PathParam("groupPath") String group)
			throws EngineException, JsonProcessingException
	{
		if (!group.startsWith("/"))
			group = "/" + group;
		log.debug("getGroupStatements query for " + group);
		GroupContents contents = groupsMan.getContents(group, GroupContents.METADATA);
		return Stream.of(contents.getGroup()
				.getAttributeStatements())
				.map(as -> Optional.ofNullable(as).map(AttributeStatementMapper::map).orElse(null))
				.collect(Collectors.toList()).toArray(new RestAttributeStatement[contents.getGroup()
													.getAttributeStatements().length]);
	}

	@Path("/group/{groupPath}/statements")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateGroupStatements")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateGroupStatements(@PathParam("groupPath") String group,
			@RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
				array = @ArraySchema(schema = @Schema(implementation = RestAttributeStatement.class)))) InputStream statementsJsonInput)
			throws EngineException, JsonProcessingException
	{
		String statementsJson = readBody(statementsJsonInput);
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
		contents.setAttributeStatements(statements.stream()
				.map(as -> Optional.ofNullable(as)
						.map(AttributeStatementMapper::map)
						.orElse(null))
				.collect(Collectors.toList())
				.toArray(new AttributeStatement[statements.size()]));
		groupsMan.updateGroup(group, contents, "set group statement", contents.getAttributeStatements().toString());
	}

	
	
	@Path("/group/{groupPath}/entity/{entityId}")
	@DELETE
	@Operation(operationId = "removeMember")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "addMember")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "getAttributeTypes")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestAttributeType> getAttributeTypes() throws EngineException, JsonProcessingException
	{
		Collection<AttributeType> attributeTypes = attributeTypeMan.getAttributeTypes();
		return attributeTypes.stream()
				.map(AttributeTypeMapper::map)
				.collect(Collectors.toList());
	}
	
	@Path("/attributeType")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addAttributeType")
	@ApiResponse(responseCode = "204", description = "Success")
	public void addAttributeType(RestAttributeType at) throws EngineException
	{
		log.debug("addAttributeType " + at);
		log.info("addAttributeType " + at.name);
		attributeTypeMan.addAttributeType(AttributeTypeMapper.map(at));
	}

	@Path("/attributeType")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateAttributeType")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateAttributeType(RestAttributeType at) throws EngineException
	{
		log.debug("updateAttributeType " + at);
		log.info("updateAttributeType " + at.name);
		attributeTypeMan.updateAttributeType(AttributeTypeMapper.map(at));
	}
	
	@Path("/attributeType/{toRemove}")
	@DELETE
	@Operation(operationId = "removeAttributeType")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "resendConfirmationForAttribute")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "userNotificationTrigger")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "resendConfirmationForIdentity")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "getEndpoints")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestResolvedEndpoint> getEndpoints() throws EngineException, JsonProcessingException
	{
		List<ResolvedEndpoint> endpoints = endpointManagement.getDeployedEndpoints();
		return endpoints.stream().map(ResolvedEndpointMapper::map).collect(Collectors.toList());
	}
	
	@Path("/endpoint/{id}")
	@DELETE
	@Operation(operationId = "undeployEndpoint")
	@ApiResponse(responseCode = "204", description = "Success")
	public void undeployEndpoint(@PathParam("id") String id) throws EngineException
	{
		endpointManagement.undeploy(id);
	}
	
	@Path("/endpoint/{id}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "deployEndpoint")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public ResolvedEndpoint deployEndpoint(@QueryParam("typeId") String typeId,
			@PathParam("id") String id, 
			@QueryParam("address") String address, 
			RestEndpointConfiguration configuration) throws EngineException, IOException
	{
		ResolvedEndpoint deployed = endpointManagement.deploy(typeId, id, address,
				EndpointConfigurationMapper.map(configuration));
		return deployed;
	}

	@Path("/endpoint/{id}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateEndpoint")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateEndpoint(@PathParam("id") String id, 
			RestEndpointConfiguration configuration) throws EngineException, IOException
	{
		endpointManagement.updateEndpoint(id, EndpointConfigurationMapper.map(configuration));
	}
	
	
	@Path("/registrationForms")
	@GET
	@Operation(operationId = "getRegistrationForms")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestRegistrationForm> getRegistrationForms() throws EngineException, JsonProcessingException
	{
		List<RegistrationForm> forms = registrationManagement.getForms();
		return forms.stream()
				.map(RegistrationFormMapper::map)
				.collect(Collectors.toList());
	}
	
	@Path("/registrationForm/{formId}")
	@DELETE
	@Operation(operationId = "removeRegistrationForm")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "addForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void addForm(@AdminJsonBody(AdminJsonBody.Failure.FORM) RestRegistrationForm form)
			throws EngineException, IOException
	{
		registrationManagement.addForm(RegistrationFormMapper.map(form));
	}
	
	@Path("/registrationForm")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateForm(@QueryParam("ignoreRequestsAndInvitations") Boolean ignoreRequestsAndInvitations,
			@QueryParam("ignoreInvitations") Boolean ignoreInvitations,
			@AdminJsonBody(AdminJsonBody.Failure.FORM) RestRegistrationForm form)
			throws EngineException, IOException
	{
		if (ignoreRequestsAndInvitations == null)
			ignoreRequestsAndInvitations = false;
		registrationManagement.updateForm(RegistrationFormMapper.map(form), ignoreRequestsAndInvitations);
	}
	
	@Path("/registrationRequests")
	@GET
	@Operation(operationId = "getRegistrationRequests")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestRegistrationRequestState> getRegistrationRequests() throws EngineException, JsonProcessingException
	{
		List<RegistrationRequestState> requests = registrationManagement.getRegistrationRequests();
		return requests.stream()
				.map(RegistrationRequestStateMapper::map)
				.collect(Collectors.toList());
	}
	
	@Path("/registrationRequest/{requestId}")
	@GET
	@Operation(operationId = "getRegistrationRequest")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestRegistrationRequestState getRegistrationRequest(@PathParam("requestId") String requestId)
			throws EngineException, JsonProcessingException
	{
		List<RegistrationRequestState> requests = registrationManagement.getRegistrationRequests();
		Optional<RegistrationRequestState> request = requests.stream().
				filter(r -> r.getRequestId().equals(requestId)).
				findAny();
		if (!request.isPresent())
			throw new WrongArgumentException("There is no request with id " + requestId);
		return RegistrationRequestStateMapper.map(request.get());
	}
	
	@Path("/enquiryForms")
	@GET
	@Operation(operationId = "getEnquiryForms")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestEnquiryForm> getEnquiryForms() throws EngineException, JsonProcessingException
	{
		List<EnquiryForm> forms = enquiryManagement.getEnquires();
		return forms.stream()
				.map(EnquiryFormMapper::map)
				.collect(Collectors.toList());
	}
	
	@Path("/enquiryForm/{formId}")
	@DELETE
	@Operation(operationId = "removeEnquiryForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeEnquiryForm(@PathParam("formId") String formId, 
			@QueryParam("dropRequests") Boolean dropRequests) throws EngineException
	{
		if (dropRequests == null)
			dropRequests = false;
		enquiryManagement.removeEnquiry(formId, dropRequests);
	}
	
	@Path("/enquiryForm")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addEnquiryForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void addEnquiryForm(@AdminJsonBody(AdminJsonBody.Failure.FORM) RestEnquiryForm form)
			throws EngineException, IOException
	{
		enquiryManagement.addEnquiry(EnquiryFormMapper.map(form));
	}
	
	@Path("/enquiryForm")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateEnquiryForm")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateEnquiryForm(@QueryParam("ignoreRequestsAndInvitations") Boolean ignoreRequestsAndInvitations,
			@QueryParam("ignoreInvitations") Boolean ignoreInvitations,
			@AdminJsonBody(AdminJsonBody.Failure.FORM) RestEnquiryForm form)
			throws EngineException, IOException
	{
		if (ignoreRequestsAndInvitations == null)
			ignoreRequestsAndInvitations = false;
		enquiryManagement.updateEnquiry(EnquiryFormMapper.map(form), ignoreRequestsAndInvitations);
	}
	
	@Path("/enquiryResponses")
	@GET
	@Operation(operationId = "getEnquiryResponses")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestEnquiryResponseState> getEnquiryResponses() throws EngineException, JsonProcessingException
	{
		List<EnquiryResponseState> requests = enquiryManagement.getEnquiryResponses();
		return requests.stream()
				.map(EnquiryResponseStateMapper::map)
				.collect(Collectors.toList());
	}
	
	@Path("/enquiryResponse/{responseId}")
	@GET
	@Operation(operationId = "getEnquiryResponse")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestEnquiryResponseState getEnquiryResponse(@PathParam("responseId") String requestId)
			throws EngineException, JsonProcessingException
	{
		List<EnquiryResponseState> requests = enquiryManagement.getEnquiryResponses();
		Optional<EnquiryResponseState> request = requests.stream().
				filter(r -> r.getRequestId().equals(requestId)).
				findAny();
		if (!request.isPresent())
			throw new WrongArgumentException("There is no enquiry response with id " + requestId);
		return EnquiryResponseStateMapper.map(request.get());
	}

	@Path("/invitations")
	@GET
	@Operation(operationId = "getInvitations")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestInvitationWithCode> getInvitations() throws EngineException, JsonProcessingException
	{
		List<InvitationWithCode> invitations = invitationMan.getInvitations();
		return invitations.stream().map(InvitationWithCodeMapper::map).collect(Collectors.toList());
	}

	@Path("/invitation/{code}")
	@GET
	@Operation(operationId = "getInvitation")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public RestInvitationWithCode getInvitation(@PathParam("code") String code)
			throws EngineException, JsonProcessingException
	{
		InvitationWithCode invitation = invitationMan.getInvitation(code);
		return InvitationWithCodeMapper.map(invitation);
	}
	
	@Path("/invitation/{code}")
	@DELETE
	@Operation(operationId = "removeInvitation")
	@ApiResponse(responseCode = "204", description = "Success")
	public void removeInvitation(@PathParam("code") String code) throws EngineException
	{
		invitationMan.removeInvitation(code);
	}

	@Path("/invitation/{code}/send")
	@POST
	@Operation(operationId = "sendInvitation")
	@ApiResponse(responseCode = "204", description = "Success")
	public void sendInvitation(@PathParam("code") String code) throws EngineException, IOException
	{
		invitationMan.sendInvitation(code);
	}

	@Path("/invitation")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(operationId = "addInvitation")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public String addInvitation(@RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
				schema = @Schema(implementation = RestInvitationParam.class))) InputStream jsonInvitationInput)
			throws EngineException, IOException
	{
		String jsonInvitation = readBody(jsonInvitationInput);
		RestInvitationParam invitationParam = getInvitationFromJson(jsonInvitation);
		return invitationMan.addInvitation(InvitationParamMapper.map(invitationParam));
	}
	
	@Path("/invitation/{code}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateInvitation")
	@ApiResponse(responseCode = "204", description = "Success")
	public void updateInvitation(@PathParam("code") String code,
			@RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
				schema = @Schema(implementation = RestInvitationParam.class))) InputStream jsonInvitationInput)
			throws EngineException, IOException
	{
		String jsonInvitation = readBody(jsonInvitationInput);
		RestInvitationParam invitationParam = getInvitationFromJson(jsonInvitation);
		invitationMan.updateInvitation(code, InvitationParamMapper.map(invitationParam));
	}	
	
	private RestInvitationParam getInvitationFromJson(String jsonInvitation)
			throws WrongArgumentException, JsonMappingException, JsonProcessingException
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
			return mapper.readValue(jsonInvitation, RestRegistrationInvitationParam.class);
		} else
		{
			return mapper.readValue(jsonInvitation, RestEnquiryInvitationParam.class);
		}
	}
	
	@Path("/bulkProcessing/instant")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Operation(operationId = "applyBulkProcessingRule")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public String applyBulkProcessingRule(@QueryParam("timeout") Long timeout, RestTranslationRule rule)
			throws EngineException
	{
		
		if (timeout == null)
			timeout = -1l;
		
		if (timeout < 0)
		{
			bulkProcessingManagement.applyRule(TranslationRuleMapper.map(rule));
			return "async";
		} else
		{
			try
			{
				bulkProcessingManagement.applyRuleSync(TranslationRuleMapper.map(rule), timeout);
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
	@Operation(operationId = "importUser")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestImportResult> importUser(@PathParam("identity") String identity,
			@QueryParam("type") String identityType,
			@QueryParam("importer") String importer) throws EngineException, IOException
	{
		UserImportSpec param = importer == null ? 
				UserImportSpec.withAllImporters(identity, identityType) : 
				new UserImportSpec(importer, identity, identityType);
		List<ImportResult> importUser = userImportManagement.importUser(
				Lists.newArrayList(param));
		return importUser.stream()
				.map(ImportResultMapper::map)
				.collect(Collectors.toList());
	}
	
	@Path("/triggerEvent/{eventName}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "triggerEvent")
	@ApiResponse(responseCode = "204", description = "Success")
	public void triggerEvent(@PathParam("eventName") String eventName,
			@RequestBody(required = true, description = "Raw payload interpreted by the configured plugin",
				content = @Content(mediaType = MediaType.APPLICATION_JSON,
						schema = @Schema(type = "string", format = "binary"))) String eventBody)
			throws EngineException, IOException
	{
		PersistableEvent event = new PersistableEvent(eventName, -1l, new Date(), eventBody);
		eventPublisher.fireEventWithAuthz(event);
	}	
	
	@Path("/token/{type}/{value}")
	@DELETE
	@Operation(operationId = "removeToken")
	@ApiResponse(responseCode = "204", description = "Success")
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
	@Operation(operationId = "getTokens")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestFormattedToken> getTokens(@QueryParam("type") String type, @QueryParam("owner") String entity,
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
		
		List<RestFormattedToken> formatted = new ArrayList<>();
		for (RestToken t : tokens.stream().map(TokenMapper::map).collect(Collectors.toList()))
			formatted.add(new RestFormattedToken(t.type, t.value, t.owner, t.created, t.expires,
					jsonFormatter.toJson(t).get("contents")));
		
		
		return formatted;
	}
	
	@Path("/idp-stats")
	@GET
	@Operation(operationId = "getIdpStatistics")
	@ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
	public List<RestGroupedIdpStatistic> getIdpStatistics(@QueryParam("since") long since,
			@QueryParam("groupBy") String groupBy,
			@QueryParam("skipZeroRecords") Boolean skipZeroRecords)
			throws EngineException, JsonProcessingException
	{
		LocalDateTime sinceDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(since), ZoneId.systemDefault());
		GroupBy groupByFallbackToTotal = groupBy != null ? GroupBy.valueOf(groupBy) : GroupBy.none;
		if (skipZeroRecords == null)
			skipZeroRecords = true;
		return idpStatisticManagement
				.getIdpStatisticsSinceGroupBy(sinceDate, groupByFallbackToTotal,
						IdpStatisticManagement.DEFAULT_SIG_IN_RECORD_LIMIT, skipZeroRecords)
				.stream()
				.map(GroupedIdpStatisticMapper::map)
				.collect(Collectors.toList());
	}

	private String readBody(InputStream input)
	{
		try
		{
			return AdminJsonProvider.readText(input, httpHeaders.getMediaType());
		} catch (IOException e)
		{
			throw new pl.edu.icm.unity.base.exceptions.InternalException("Can't read request body", e);
		}
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




