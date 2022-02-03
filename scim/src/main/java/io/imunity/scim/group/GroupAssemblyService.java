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

import javax.ws.rs.core.UriBuilder;

import org.springframework.stereotype.Component;

import io.imunity.scim.common.BasicSCIMResource;
import io.imunity.scim.common.ListResponse;
import io.imunity.scim.common.Meta;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.user.UserRestController;

class GroupAssemblyService
{
	private static final String DEFAULT_META_VERSION = "v1";
	private SCIMEndpointDescription configuration;

	GroupAssemblyService(SCIMEndpointDescription configuration)
	{
		this.configuration = configuration;
	}

	BasicSCIMResource mapToGroupResource(GroupData group)
	{
		return mapToSingleGroupResource(group);
	}

	ListResponse<SCIMGroupResource> mapToGroupsResource(List<GroupData> groups)
	{
		List<SCIMGroupResource> groupsResource = groups.stream().map(u -> mapToSingleGroupResource(u))
				.collect(Collectors.toList());
		return ListResponse.<SCIMGroupResource>builder().withResources(groupsResource)
				.withTotalResults(groupsResource.size()).build();
	}

	SCIMGroupResource mapToSingleGroupResource(GroupData group)
	{
		return SCIMGroupResource.builder().withDisplayName(group.displayName).withId(group.id)
				.withMeta(Meta.builder().withResourceType(Meta.ResourceType.Group).withVersion(DEFAULT_META_VERSION)
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
		return UriBuilder.fromUri(configuration.baseLocation).path(GroupRestController.SINGLE_GROUP_LOCATION)
				.path(URLEncoder.encode(group, StandardCharsets.UTF_8)).build();
	}

	private URI getUserLocation(String user)
	{

		return UriBuilder.fromUri(configuration.baseLocation)
				.path(UserRestController.SINGLE_USER_LOCATION + "/" + user).build();
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
