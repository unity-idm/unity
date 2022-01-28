/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.exceptions.ResourceNotFoundException;
import io.imunity.scim.handlers.SCIMUserHandler;
import io.imunity.scim.messages.ListResponse;
import io.imunity.scim.types.Meta;
import io.imunity.scim.types.PersistentId;
import io.imunity.scim.types.UserResource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;

public class SCIMUserService
{
	public static final String DEFAULT_META_VERSION = "v1";
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SCIMUserService.class);

	private final AuthorizationManagement authzMan;
	private final EntityManagement entityManagement;
	private final BulkGroupQueryService bulkService;

	private final SCIMEndpointDescription configuration;

	public SCIMUserService(AuthorizationManagement authzMan, EntityManagement entityManagement,
			BulkGroupQueryService bulkService, SCIMEndpointDescription configuration)
	{
		this.entityManagement = entityManagement;
		this.configuration = configuration;
		this.bulkService = bulkService;
		this.authzMan = authzMan;
	}

	public UserResource getLoggedUser() throws EngineException
	{
		Entity entity = entityManagement
				.getEntity(new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId()));
		return getUser(entity);
	}

	public UserResource getUser(PersistentId userId) throws EngineException
	{
		Entity entity = entityManagement.getEntity(new EntityParam(new IdentityTaV(PersistentIdentity.ID, userId.id)));
		return getUser(entity);
	}

	public UserResource getUser(Entity entity) throws EngineException
	{
		long callerId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		authzMan.checkReadCapability(entity.getId().longValue() == callerId, configuration.rootGroup);
		Map<String, GroupMembership> groups = entityManagement.getGroups(new EntityParam(entity.getId()));
		if (!groups.keySet().stream().anyMatch(userGroup -> configuration.membershipGroups.stream()
				.anyMatch(mgroup -> Group.isChildOrSame(userGroup, mgroup))))
		{
			log.error("User " + entity.getId() + " is out of range for configured membership groups");
			throw new ResourceNotFoundException("Invalid user");
		}

		return mapToUserResource(entity);
	}

	public ListResponse<UserResource> getUsers() throws EngineException
	{
		authzMan.checkReadCapability(false, configuration.rootGroup);

		List<UserResource> users = new ArrayList<>();
		Map<Long, EntityInGroupData> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData("/"));

		for (EntityInGroupData entityInGroup : membershipInfo.values())
		{
			if (entityInGroup.groups.stream().anyMatch(userGroup -> configuration.membershipGroups.stream()
					.anyMatch(mgroup -> Group.isChildOrSame(userGroup, mgroup))))
			{
				users.add(mapToUserResource(entityInGroup.entity));
			}
		}

		return ListResponse.<UserResource>builder().withResources(users).withTotalResults(users.size()).build();
	}

	private UserResource mapToUserResource(Entity entity)
	{
		Identity persistence = entity.getIdentities().stream().filter(i -> i.getTypeId().equals(PersistentIdentity.ID))
				.findFirst().get();
		Date lastModified = entity.getIdentities().stream().map(i -> i.getUpdateTs()).sorted(Comparator.reverseOrder())
				.findFirst().get();

		URI location = UriBuilder.fromUri(configuration.baseLocation)
				.path(SCIMUserHandler.SINGLE_USER_LOCATION + "/" + persistence.getComparableValue()).build();

		return UserResource.builder().withId(persistence.getComparableValue())
				.withMeta(Meta.builder().withResourceType(Meta.ResourceType.User).withVersion(DEFAULT_META_VERSION)
						.withCreated(persistence.getCreationTs().toInstant()).withLastModified(lastModified.toInstant())
						.withLocation(location).build())
				.withUserName(getUserNameFallbackToNone(entity.getIdentities())).build();
	}

	private String getUserNameFallbackToNone(List<Identity> identities)
	{
		Optional<Identity> userNameId = identities.stream().filter(i -> i.getTypeId().equals(UsernameIdentity.ID))
				.findFirst();
		if (userNameId.isPresent())
			return userNameId.get().getComparableValue();
		Optional<Identity> emailId = identities.stream().filter(i -> i.getTypeId().equals(EmailIdentity.ID))
				.findFirst();
		if (emailId.isPresent())
			return emailId.get().getComparableValue();

		return "none";
	}

	@Component
	public static class SCIMUserServiceFactory
	{
		private final EntityManagement entityManagement;
		private final BulkGroupQueryService bulkService;
		private final AuthorizationManagement authzMan;

		@Autowired
		public SCIMUserServiceFactory(@Qualifier("insecure") EntityManagement entityManagement,
				@Qualifier("insecure") BulkGroupQueryService bulkService, AuthorizationManagement authzMan)
		{
			this.entityManagement = entityManagement;
			this.bulkService = bulkService;
			this.authzMan = authzMan;
		}

		public SCIMUserService getService(SCIMEndpointDescription configuration)
		{
			return new SCIMUserService(authzMan, entityManagement, bulkService, configuration);
		}
	}
}
