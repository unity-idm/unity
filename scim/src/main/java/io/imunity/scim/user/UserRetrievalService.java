/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import io.imunity.scim.MembershipGroupsUtils;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.user.UserAuthzService.SCIMUserAuthzServiceFactory;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.IdentityTaV;

class UserRetrievalService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, UserRetrievalService.class);

	public static final String DEFAULT_META_VERSION = "v1";

	private final UserAuthzService authzService;
	private final EntityManagement entityManagement;
	private final BulkGroupQueryService bulkService;
	private final AttributesManagement attrMan;

	private final SCIMEndpointDescription configuration;

	UserRetrievalService(UserAuthzService scimAuthzService, EntityManagement entityManagement,
			BulkGroupQueryService bulkService, AttributesManagement attrMan, SCIMEndpointDescription configuration)
	{
		this.entityManagement = entityManagement;
		this.configuration = configuration;
		this.bulkService = bulkService;
		this.authzService = scimAuthzService;
		this.attrMan = attrMan;
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
		Map<String, GroupMembership> groups = entityManagement.getGroups(new EntityParam(entity.getId()));
		authzService.checkReadUser(entity.getId().longValue(), groups.keySet());
		if (!groups.keySet().contains(configuration.rootGroup))
		{
			log.error("User " + entity.getId() + " is out of range for configured membership groups");
			throw new UserNotFoundException("Invalid user");
		}

		Map<String, GroupContents> effectiveMembershipGroups = getAllMembershipGroups();

		Set<String> userGroups = groups.keySet().stream().filter(userGroup -> effectiveMembershipGroups.keySet()
				.stream().anyMatch(mgroup -> Group.isChildOrSame(userGroup, mgroup))).collect(Collectors.toSet());

		Collection<AttributeExt> attributes = attrMan.getAttributes(new EntityParam(entity.getId()),
				configuration.rootGroup, null);

		return mapToUser(entity,
				effectiveMembershipGroups.entrySet().stream().filter(e -> userGroups.contains(e.getKey()))
						.map(e -> e.getValue().getGroup()).collect(Collectors.toSet()),
				attributes.stream().collect(Collectors.toMap(a -> a.getName(), a -> a)));
	}

	List<User> getUsers() throws EngineException
	{
		authzService.checkReadUsers();

		List<User> users = new ArrayList<>();
		GroupMembershipData bulkMembershipData = bulkService.getBulkMembershipData("/");
		Map<Long, EntityInGroupData> membershipInfo = bulkService.getMembershipInfo(bulkMembershipData);
		Map<String, GroupContents> groupAndSubgroups = getAllMembershipGroups();

		Map<Long, Map<String, AttributeExt>> groupUsersAttributes = bulkService
				.getGroupUsersAttributes(configuration.rootGroup, bulkMembershipData);

		for (EntityInGroupData entityInGroup : membershipInfo.values())
		{
			if (!entityInGroup.groups.contains(configuration.rootGroup))
				continue;

			Set<String> groups = new HashSet<>(entityInGroup.groups);
			groups.retainAll(groupAndSubgroups.keySet());

			users.add(mapToUser(entityInGroup.entity,
					groupAndSubgroups.entrySet().stream().filter(e -> groups.contains(e.getKey()))
							.map(e -> e.getValue().getGroup()).collect(Collectors.toSet()),
					groupUsersAttributes.getOrDefault(entityInGroup.entity.getId(), Collections.emptyMap())));
		}

		return users;
	}

	private Map<String, GroupContents> getAllMembershipGroups() throws EngineException
	{
		Map<String, GroupContents> allGroupsWithSubgroups = bulkService
				.getGroupAndSubgroups(bulkService.getBulkStructuralData("/"));

		List<String> effectiveMembershipGroups = MembershipGroupsUtils.getEffectiveMembershipGroups(
				configuration.membershipGroups, configuration.excludedMembershipGroups, allGroupsWithSubgroups.values()
						.stream().collect(Collectors.toMap(g -> g.getGroup().getPathEncoded(), g -> g.getGroup())));

		Map<String, GroupContents> ret = new HashMap<>();

		for (String g : effectiveMembershipGroups)
		{
			for (String ag : allGroupsWithSubgroups.keySet())
			{
				if (ag.equals(g))
					ret.put(ag, allGroupsWithSubgroups.get(ag));
			}
		}

		return ret;
	}

	private User mapToUser(Entity entity, Set<Group> groups, Map<String, AttributeExt> attributes)
	{
		return User.builder().withEntityId(entity.getId()).withGroups(groups).withIdentities(entity.getIdentities())
				.withAttributes(attributes.values().stream().collect(Collectors.toList())).build();
	}

	@Component
	static class SCIMUserRetrievalServiceFactory
	{
		private final EntityManagement entityManagement;
		private final BulkGroupQueryService bulkService;
		private final SCIMUserAuthzServiceFactory authzManFactory;
		private final AttributesManagement attrMan;

		@Autowired
		SCIMUserRetrievalServiceFactory(@Qualifier("insecure") EntityManagement entityManagement,
				@Qualifier("insecure") BulkGroupQueryService bulkService,
				@Qualifier("insecure") AttributesManagement attrMan, SCIMUserAuthzServiceFactory authzManFactory)

		{
			this.entityManagement = entityManagement;
			this.bulkService = bulkService;
			this.authzManFactory = authzManFactory;
			this.attrMan = attrMan;
		}

		UserRetrievalService getService(SCIMEndpointDescription configuration)
		{
			return new UserRetrievalService(authzManFactory.getService(configuration), entityManagement, bulkService,
					attrMan, configuration);
		}
	}
}
