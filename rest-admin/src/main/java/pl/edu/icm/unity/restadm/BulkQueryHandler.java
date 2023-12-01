/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.imunity.rest.api.types.basic.RestEntityGroupAttributes;
import io.imunity.rest.api.types.basic.RestGroupMember;
import io.imunity.rest.api.types.basic.RestMultiGroupMembers;
import io.imunity.rest.mappers.AttributeExtMapper;
import io.imunity.rest.mappers.EntityMapper;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityGroupAttributes;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupsWithMembers;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;


@Produces(MediaType.APPLICATION_JSON)
@Path(RESTAdminEndpoint.V1_PATH)
@PrototypeComponent
public class BulkQueryHandler implements RESTAdminHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTAdmin.class);
	private final ObjectMapper mapper = Constants.MAPPER;
	private final BulkGroupQueryService bulkQueryService;

	@Autowired
	BulkQueryHandler(BulkGroupQueryService bulkQueryService)
	{
		this.bulkQueryService = bulkQueryService;
	}

	@Path("/group-members/{groupPath}")
	@GET
	public String getGroupMembersResolved(@PathParam("groupPath") String group) 
			throws EngineException, JsonProcessingException
	{
		log.debug("getGroupMembersResolved query for " + group);
		if (!group.startsWith("/"))
			group = "/" + group;
		GroupMembershipData bulkMembershipData = bulkQueryService.getBulkMembershipData(group);
		Map<Long, Map<String, AttributeExt>> userAttributes = 
				bulkQueryService.getGroupUsersAttributes(group, bulkMembershipData);
		Map<Long, Entity> entitiesData = bulkQueryService.getGroupEntitiesNoContextWithoutTargeted(bulkMembershipData);
		List<RestGroupMember> ret = new ArrayList<>(userAttributes.size());
		for (Long memberId: userAttributes.keySet())
		{
			Collection<AttributeExt> attributes = userAttributes.get(memberId).values(); 
			Entity entity = entitiesData.get(memberId);
			ret.add(createGroupMember(group, entity, attributes));
		}
		return mapper.writeValueAsString(ret);
	}

	private RestGroupMember createGroupMember(String group, Entity entity, Collection<AttributeExt> attributes)
	{
		return RestGroupMember.builder()
		.withGroup(group)
		.withAttributes(Optional.ofNullable(attributes)
				.map(l -> l.stream()
						.map(attr -> Optional.ofNullable(attr)
								.map(AttributeExtMapper::map)
								.orElse(null))
						.collect(Collectors.toList()))
				.orElse(null))
		.withEntity(Optional.ofNullable(entity)
				.map(EntityMapper::map)
				.orElse(null))
		.build();
	}
	
	@Path("/group-members-multi/{rootGroupPath}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public String getMultiGroupMembersResolved(@PathParam("rootGroupPath") String rootGroup, String groupsFilter) 
			throws EngineException, JsonProcessingException
	{
		log.debug("getGroupMembersResolved query for contents under " + rootGroup);
		if (!rootGroup.startsWith("/"))
			rootGroup = "/" + rootGroup;
		Set<String> requestedGroups;
		try
		{
			requestedGroups = Constants.MAPPER.readValue(groupsFilter, 
					new TypeReference<Set<String>>() {});
		} catch (IOException e)
		{
			throw new WrongArgumentException("Can not parse request body as a list of groups", e);
		}
		
		GroupsWithMembers members = bulkQueryService.getMembersWithAttributeForAllGroups(rootGroup, requestedGroups);
		
		Map<String, List<RestEntityGroupAttributes>> attributesByGroup = new HashMap<>();
		
		for (Entry<String, List<EntityGroupAttributes>> groupData: members.membersByGroup.entrySet())
		{
			List<RestEntityGroupAttributes> perGroupAttributes = groupData.getValue().stream()
					.map(src -> createEntityGroupAttributes(src))
					.collect(Collectors.toList());
			attributesByGroup.put(groupData.getKey(), perGroupAttributes);
		}
		
		return mapper.writeValueAsString(createMultiGroupMembers(members.entities.values(), attributesByGroup));
	}
	
	private RestMultiGroupMembers createMultiGroupMembers(Collection<Entity> entities,
			Map<String, List<RestEntityGroupAttributes>> members)
	{

		return RestMultiGroupMembers.builder()
				.withEntities(Optional.ofNullable(entities)
						.map(l -> l.stream()
								.map(en -> Optional.ofNullable(en)
										.map(EntityMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.withMembers(members)
				.build();
	}

	public static RestEntityGroupAttributes createEntityGroupAttributes(EntityGroupAttributes entityGroupAttributes)
	{
		return RestEntityGroupAttributes.builder()
				.withEntityId(entityGroupAttributes.entityId)
				.withAttributes(Optional.ofNullable(entityGroupAttributes.attribtues.values())
						.map(l -> l.stream()
								.map(attr -> Optional.ofNullable(attr)
										.map(AttributeExtMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))

				.build();
	}
}
