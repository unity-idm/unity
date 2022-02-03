/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.imunity.scim.config.SCIMEndpointConfiguration;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;

public class SCIMServiceConfigurationBean
{
	private List<String> allowedCORSheaders;
	private List<String> allowedCORSorigins;
	private GroupWithIndentIndicator rootGroup;
	private List<Group> membershipGroups;

	SCIMServiceConfigurationBean()
	{
		allowedCORSheaders = new ArrayList<>();
		allowedCORSorigins = new ArrayList<>();
		membershipGroups = new ArrayList<>();
	}

	public List<String> getAllowedCORSheaders()
	{
		return allowedCORSheaders;
	}

	public void setAllowedCORSheaders(List<String> allowedCORSheaders)
	{
		this.allowedCORSheaders = allowedCORSheaders;
	}

	public void setAllowedCORSorigins(List<String> allowedCORSorigins)
	{
		this.allowedCORSorigins = allowedCORSorigins;
	}

	public List<String> getAllowedCORSorigins()
	{
		return allowedCORSorigins;
	}

	public GroupWithIndentIndicator getRootGroup()
	{
		return rootGroup;
	}

	public void setRootGroup(GroupWithIndentIndicator rootGroup)
	{
		this.rootGroup = rootGroup;
	}

	public List<Group> getMembershipGroups()
	{
		return membershipGroups;
	}

	public void setMembershipGroups(List<Group> membershipGroups)
	{
		this.membershipGroups = membershipGroups;
	}

	void setConfig(SCIMEndpointConfiguration orgConfig)
	{
		this.allowedCORSheaders.addAll(orgConfig.allowedCorsHeaders);
		this.allowedCORSorigins.addAll(orgConfig.allowedCorsOrigins);
		this.membershipGroups
				.addAll(orgConfig.membershipGroups.stream().map(g -> new Group(g)).collect(Collectors.toList()));
		this.rootGroup = new GroupWithIndentIndicator(new Group(orgConfig.rootGroup), false);
	}

	SCIMEndpointConfiguration getConfig()
	{
		return SCIMEndpointConfiguration.builder().withAllowedCorsHeaders(allowedCORSheaders)
				.withAllowedCorsOrigins(allowedCORSorigins).withRootGroup(rootGroup.group.getPathEncoded())
				.withMembershipGroups(
						membershipGroups.stream().map(g -> g.getPathEncoded()).collect(Collectors.toList()))
				.build();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(allowedCORSheaders, allowedCORSorigins, membershipGroups, rootGroup);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SCIMServiceConfigurationBean other = (SCIMServiceConfigurationBean) obj;
		return Objects.equals(allowedCORSheaders, other.allowedCORSheaders)
				&& Objects.equals(allowedCORSorigins, other.allowedCORSorigins)
				&& Objects.equals(membershipGroups, other.membershipGroups)
				&& Objects.equals(rootGroup, other.rootGroup);
	}

}