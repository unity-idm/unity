/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.user.UserAuthzService.SCIMUserAuthzServiceFactory;
import io.imunity.scim.user.UserGroup.GroupType;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.IdentityTaV;

class UserRetrievalService
{
	public static final String DEFAULT_META_VERSION = "v1";
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, UserRetrievalService.class);

	private final MessageSource msg;
	private final UserAuthzService authzMan;
	private final EntityManagement entityManagement;
	private final BulkGroupQueryService bulkService;

	private final SCIMEndpointDescription configuration;

	UserRetrievalService(MessageSource msg, UserAuthzService scimAuthzService,
			EntityManagement entityManagement, BulkGroupQueryService bulkService, SCIMEndpointDescription configuration)
	{
		this.msg = msg;
		this.entityManagement = entityManagement;
		this.configuration = configuration;
		this.bulkService = bulkService;
		this.authzMan = scimAuthzService;
	}

	User getLoggedUser() throws EngineException
	{
		Entity entity = entityManagement
				.getEntity(new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId()));
		return getUser(entity);
	}

	User getUser(PersistentId userId) throws EngineException
	{
		Entity entity = entityManagement.getEntity(new EntityParam(new IdentityTaV(PersistentIdentity.ID, userId.id)));
		return getUser(entity);
	}

	private User getUser(Entity entity) throws EngineException
	{
		authzMan.checkReadUser(entity.getId().longValue());

		Map<String, GroupMembership> groups = entityManagement.getGroups(new EntityParam(entity.getId()));
		Set<String> rgroups = groups.keySet().stream().filter(userGroup -> configuration.membershipGroups.stream()
				.anyMatch(mgroup -> Group.isChildOrSame(userGroup, mgroup))).collect(Collectors.toSet());
		Map<String, GroupContents> groupAndSubgroups = getAllMembershipGroups();

		if (rgroups.isEmpty())
		{
			log.error("User " + entity.getId() + " is out of range for configured membership groups");
			throw new UserNotFoundException("Invalid user");
		}

		return mapToUser(entity, groupAndSubgroups.entrySet().stream().filter(e -> rgroups.contains(e.getKey()))
				.map(e -> e.getValue().getGroup()).collect(Collectors.toSet()));

	}

	List<User> getUsers() throws EngineException
	{
		authzMan.checkReadUsers();

		List<User> users = new ArrayList<>();
		GroupMembershipData bulkMembershipData = bulkService.getBulkMembershipData("/");
		Map<Long, EntityInGroupData> membershipInfo = bulkService.getMembershipInfo(bulkMembershipData);
		Map<String, GroupContents> groupAndSubgroups = getAllMembershipGroups();

		for (EntityInGroupData entityInGroup : membershipInfo.values())
		{
			Set<String> groups = new HashSet<>(entityInGroup.groups);
			groups.retainAll(groupAndSubgroups.keySet());
			if (groups.isEmpty())
				continue;

			users.add(mapToUser(entityInGroup.entity,
					groupAndSubgroups.entrySet().stream().filter(e -> groups.contains(e.getKey()))
							.map(e -> e.getValue().getGroup()).collect(Collectors.toSet())));
		}

		return users;
	}

	private Map<String, GroupContents> getAllMembershipGroups() throws EngineException
	{
		Map<String, GroupContents> groupAndSubgroups = bulkService
				.getGroupAndSubgroups(bulkService.getBulkStructuralData("/"));
		Map<String, GroupContents> ret = new HashMap<>();

		for (String g : configuration.membershipGroups)
		{
			for (String ag : groupAndSubgroups.keySet())
			{
				if (Group.isChildOrSame(ag, g))
					ret.put(ag, groupAndSubgroups.get(ag));
			}
		}

		return ret;
	}

	private User mapToUser(Entity entity, Set<Group> groups)
	{
		return User.builder().withEntityId(entity.getId()).withGroups(groups.stream()
				.map(g -> UserGroup.builder().withDisplayName(g.getDisplayedNameShort(msg).getValue(msg))
						.withType(GroupType.direct).withValue(g.getPathEncoded()).build())
				.collect(Collectors.toSet()))
				.withIdentities(entity.getIdentities().stream()
						.map(i -> UserIdentity.builder().withCreationTs(i.getCreationTs().toInstant())
								.withUpdateTs(i.getUpdateTs().toInstant()).withValue(i.getComparableValue())
								.withTypeId(i.getTypeId()).build())
						.collect(Collectors.toList()))
				.build();
	}

	@Component
	static class SCIMUserRetrievalServiceFactory
	{
		private final MessageSource msg;
		private final EntityManagement entityManagement;
		private final BulkGroupQueryService bulkService;
		private final SCIMUserAuthzServiceFactory authzManFactory;

		@Autowired
		SCIMUserRetrievalServiceFactory(MessageSource msg, @Qualifier("insecure") EntityManagement entityManagement,
				@Qualifier("insecure") BulkGroupQueryService bulkService, SCIMUserAuthzServiceFactory authzManFactory)

		{
			this.entityManagement = entityManagement;
			this.bulkService = bulkService;
			this.authzManFactory = authzManFactory;
			this.msg = msg;
		}

		UserRetrievalService getService(SCIMEndpointDescription configuration)
		{
			return new UserRetrievalService(msg, authzManFactory.getService(configuration), entityManagement,
					bulkService, configuration);
		}
	}
}
