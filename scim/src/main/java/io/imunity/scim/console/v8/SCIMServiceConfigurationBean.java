/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.console.v8;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.imunity.scim.console.mapping.SchemaWithMappingBean;
import io.imunity.scim.schema.DefaultSchemaProvider;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;

public class SCIMServiceConfigurationBean
{
	private List<String> allowedCORSheaders;
	private List<String> allowedCORSorigins;
	private GroupWithIndentIndicator rootGroup;
	private GroupWithIndentIndicator restAdminGroup;
	private List<Group> membershipGroups;
	private List<SchemaWithMappingBean> schemas;
	private List<String> membershipAttributes;
	private List<Group> excludedMembershipGroups;

	SCIMServiceConfigurationBean(ConfigurationVaadinBeanMapper configurationVaadinBeanMapper)
	{
		allowedCORSheaders = new ArrayList<>();
		allowedCORSorigins = new ArrayList<>();
		membershipGroups = new ArrayList<>();
		excludedMembershipGroups = new ArrayList<>();
		membershipAttributes = new ArrayList<>(DefaultSchemaProvider.getBasicUserSchemaMembershipAttributes());
		schemas = new ArrayList<>();
		schemas.add(
				configurationVaadinBeanMapper.mapFromConfigurationSchema(DefaultSchemaProvider.getBasicGroupSchema()));
		schemas.add(
				configurationVaadinBeanMapper.mapFromConfigurationSchema(DefaultSchemaProvider.getBasicUserSchema()));
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

	public List<SchemaWithMappingBean> getSchemas()
	{
		return schemas;
	}

	public void setSchemas(List<SchemaWithMappingBean> schemas)
	{
		this.schemas = schemas;
	}

	public List<String> getMembershipAttributes()
	{
		return membershipAttributes;
	}

	public void setMembershipAttributes(List<String> membershipAttributes)
	{
		this.membershipAttributes = membershipAttributes;
	}

	public List<Group> getExcludedMembershipGroups()
	{
		return excludedMembershipGroups;
	}

	public void setExcludedMembershipGroups(List<Group> excludedMembershipGroups)
	{
		this.excludedMembershipGroups = excludedMembershipGroups;
	}

	public GroupWithIndentIndicator getRestAdminGroup()
	{
		return restAdminGroup;
	}

	public void setRestAdminGroup(GroupWithIndentIndicator adminGroup)
	{
		this.restAdminGroup = adminGroup;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(allowedCORSheaders, allowedCORSorigins, membershipGroups, excludedMembershipGroups,
				rootGroup, restAdminGroup, schemas, membershipAttributes);
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
				&& Objects.equals(excludedMembershipGroups, other.excludedMembershipGroups)
				&& Objects.equals(membershipAttributes, other.membershipAttributes)
				&& Objects.equals(rootGroup, other.rootGroup) 
				&& Objects.equals(restAdminGroup, other.restAdminGroup) 
				&& Objects.equals(schemas, other.schemas);
	}
}