/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.scim.common.ListResponse;
import io.imunity.scim.common.Meta;
import io.imunity.scim.common.ResourceType;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;
import io.imunity.scim.user.UserAuthzService.SCIMUserAuthzServiceFactory;
import io.imunity.scim.user.mapping.evaluation.UserSchemaEvaluator;
import io.imunity.scim.user.mapping.evaluation.UserSchemaEvaluator.UserSchemaEvaluatorFactory;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.mvel.CachingMVELGroupProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.types.basic.Identity;

class UserAssemblyService
{
	private final SCIMEndpointDescription configuration;
	private final UserSchemaEvaluator userSchemaEvaluator;
	private final GroupsManagement groupsManagement;
	private final UserAuthzService authzService;

	UserAssemblyService(SCIMEndpointDescription configuration, UserSchemaEvaluator userSchemaEvaluator,
			GroupsManagement groupsManagement, UserAuthzService authzService)
	{
		this.configuration = configuration;
		this.userSchemaEvaluator = userSchemaEvaluator;
		this.groupsManagement = groupsManagement;
		this.authzService = authzService;
	}

	SCIMUserResource mapToUserResource(User user) throws EngineException
	{
		Predicate<AttributeDefinitionWithMapping> filter = authzService.getFilter();
		List<SchemaWithMapping> schemas = getFilteredSchemas(filter);
		return mapToSingleUserResource(user, schemas,
				new CachingMVELGroupProvider(groupsManagement.getAllGroups()));
	}

	ListResponse<SCIMUserResource> mapToListUsersResource(List<User> users) throws EngineException
	{
		List<SCIMUserResource> usersResource = new ArrayList<>();
		Predicate<AttributeDefinitionWithMapping> filter = authzService.getFilter();
		List<SchemaWithMapping> schemas = getFilteredSchemas(filter);
		CachingMVELGroupProvider groupProvider = new CachingMVELGroupProvider(groupsManagement.getAllGroups());
		for (User u : users)
		{
			usersResource.add(mapToSingleUserResource(u, schemas, groupProvider));
		}

		return ListResponse.<SCIMUserResource>builder().withResources(usersResource)
				.withTotalResults(usersResource.size()).build();
	}

	private SCIMUserResource mapToSingleUserResource(User user, 
			List<SchemaWithMapping> schemas, CachingMVELGroupProvider cachingMVELGroupProvider)
			throws EngineException
	{
		Identity persistenceIdentity = user.identities.stream().filter(i -> i.getTypeId().equals(PersistentIdentity.ID))
				.findFirst().get();
		Instant lastModified = user.identities.stream().map(i -> i.getUpdateTs().toInstant())
				.sorted(Comparator.reverseOrder()).findFirst().get();
		URI location = UriBuilder.fromUri(configuration.baseLocation)
				.path(UserRestController.USER_LOCATION + "/" + persistenceIdentity.getValue()).build();
		return SCIMUserResource.builder().withId(persistenceIdentity.getValue()).withSchemas(schemas.stream().map(s -> s.id).collect(Collectors.toSet()))
				.withMeta(Meta.builder().withResourceType(ResourceType.USER.getName())
						.withCreated(persistenceIdentity.getCreationTs().toInstant()).withLastModified(lastModified)
						.withLocation(location).build())
				.withAttributes(
						userSchemaEvaluator.evalUserSchema(user, schemas, cachingMVELGroupProvider))
				.build();
	}
	
	private List<SchemaWithMapping> getFilteredSchemas(Predicate<AttributeDefinitionWithMapping> filter)
	{
		List<SchemaWithMapping> schemas = configuration.schemas.stream()
				.filter(s -> (s.type.equals(SchemaType.USER) || s.type.equals(SchemaType.USER_CORE)) && s.enable)
				.map(s -> SchemaFilteringSupport.getSchemaWithFilteredAttributes(s, filter))
				.collect(Collectors.toList());
		if (schemas.isEmpty())
			throw new ConfigurationException("No user schemas are active");

		return schemas;
	}

	@Component
	static class SCIMUserAssemblyServiceFactory
	{
		private final UserSchemaEvaluatorFactory userSchemaEvaluator;
		private final GroupsManagement groupsManagement;
		private final SCIMUserAuthzServiceFactory authzManFactory;

		public SCIMUserAssemblyServiceFactory(UserSchemaEvaluatorFactory userSchemaEvaluator,
				@Qualifier("insecure") GroupsManagement groupsManagement, SCIMUserAuthzServiceFactory authzManFactory)
		{
			this.userSchemaEvaluator = userSchemaEvaluator;
			this.groupsManagement = groupsManagement;
			this.authzManFactory = authzManFactory;
		}

		UserAssemblyService getService(SCIMEndpointDescription configuration)
		{
			return new UserAssemblyService(configuration, userSchemaEvaluator.getService(configuration),
					groupsManagement, authzManFactory.getService(configuration));
		}
	}
}
