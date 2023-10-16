/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.UriBuilder;

import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.scim.common.ListResponse;
import io.imunity.scim.common.Meta;
import io.imunity.scim.common.ResourceType;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.user.UserRestController;

class GroupAssemblyService
{
	private SCIMEndpointDescription configuration;

	GroupAssemblyService(SCIMEndpointDescription configuration)
	{
		this.configuration = configuration;
	}

	SCIMGroupResource mapToGroupResource(GroupData group)
	{
		assertGroupSchemasAreActive();
		return mapToSingleGroupResource(group);
	}

	ListResponse<SCIMGroupResource> mapToGroupsResource(List<GroupData> groups)
	{
		assertGroupSchemasAreActive();
		List<SCIMGroupResource> groupsResource = groups.stream().map(u -> mapToSingleGroupResource(u))
				.collect(Collectors.toList());
		return ListResponse.<SCIMGroupResource>builder().withResources(groupsResource)
				.withTotalResults(groupsResource.size()).build();
	}

	//FIXME support for group creation and modification time
	private SCIMGroupResource mapToSingleGroupResource(GroupData group)
	{
		return SCIMGroupResource.builder().withDisplayName(group.displayName).withId(group.id)
				.withMeta(Meta.builder().withResourceType(ResourceType.GROUP.getName())
						// .withCreated(reated)
						// .withLastModified(lastModified)
						.withLocation(getGroupLocation(group.id)).build())
				.withMembers(group.members.stream().map(m -> mapToMember(m)).collect(Collectors.toList())).build();
	}

	private SCIMGroupMemberResource mapToMember(GroupMember member)
	{
		return SCIMGroupMemberResource.builder().withValue(member.value)
				.withRef(member.type.equals(GroupMember.MemberType.Group) ? getGroupLocation(member.value)
						: getUserLocation(member.value))
				.withType(member.type.toString()).withDisplay(member.displayName).build();
	}

	private URI getGroupLocation(String group)
	{
		return UriBuilder.fromUri(configuration.baseLocation).path(GroupRestController.GROUP_LOCATION)
				.path(URLEncoder.encode(group, StandardCharsets.UTF_8)).build();
	}

	private URI getUserLocation(String user)
	{

		return UriBuilder.fromUri(configuration.baseLocation)
				.path(UserRestController.USER_LOCATION + "/" + user).build();
	}

	private void assertGroupSchemasAreActive()
	{
		if (configuration.schemas.stream()
				.filter(s -> (s.type.equals(SchemaType.GROUP) || s.type.equals(SchemaType.GROUP_CORE)) && s.enable)
				.count() == 0)
		{
			throw new ConfigurationException("No group schemas are active");
		}
	}

	@Component
	static class SCIMGroupResourceAssemblyServiceFactory
	{
		GroupAssemblyService getService(SCIMEndpointDescription configuration)
		{
			return new GroupAssemblyService(configuration);
		}
	}
}
