/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.AttributeMapping;
import io.imunity.scim.config.SCIMEndpointConfiguration;
import io.imunity.scim.config.SchemaWithMapping;
import io.imunity.scim.config.UndefinedMapping;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;

@Component
class ConfigurationVaadinBeanMapper
{
	private final GroupsManagement groupsManagement;

	
	 ConfigurationVaadinBeanMapper(GroupsManagement groupsManagement)
	{
		this.groupsManagement = groupsManagement;
	}

	SCIMServiceConfigurationBean mapToBean(SCIMEndpointConfiguration orgConfig) 
	{
		
		Map<String, Group> allGroups;
		try
		{
			allGroups = groupsManagement.getAllGroups();
		} catch (EngineException e)
		{
			throw new InternalException("Can not get all groups", e);
		}
		
		SCIMServiceConfigurationBean bean = new SCIMServiceConfigurationBean(this);
		bean.getAllowedCORSheaders().addAll(orgConfig.allowedCorsHeaders);
		bean.getAllowedCORSorigins().addAll(orgConfig.allowedCorsOrigins);
		bean.getMembershipGroups().addAll(orgConfig.membershipGroups.stream()
				.map(g -> allGroups.getOrDefault(g, new Group(g)))
				.collect(Collectors.toList()));
		bean.setRootGroup(new GroupWithIndentIndicator(new Group(orgConfig.rootGroup), false));
		bean.setSchemas(
				orgConfig.schemas.stream().map(s -> mapFromConfigurationSchema(s)).collect(Collectors.toList()));
		bean.setMembershipAttributes(orgConfig.membershipAttributes);
		return bean;
	}

	SchemaWithMappingBean mapFromConfigurationSchema(SchemaWithMapping schema)
	{
		SchemaWithMappingBean schemaBean = new SchemaWithMappingBean();
		schemaBean.setId(schema.id);
		schemaBean.setType(schema.type);
		schemaBean.setName(schema.name);
		schemaBean.setDescription(schema.description);
		schemaBean.setEnable(schema.enable);
		schemaBean.setAttributes(schema.attributesWithMapping.stream()
				.map(a -> mapFromAttributeDefinitionWithMapping(a)).collect(Collectors.toList()));

		return schemaBean;
	}

	private AttributeDefinitionWithMappingBean mapFromAttributeDefinitionWithMapping(
			AttributeDefinitionWithMapping a)
	{
		AttributeDefinitionWithMappingBean bean = new AttributeDefinitionWithMappingBean();
		AttributeDefinitionBean attrBean = new AttributeDefinitionBean();
		attrBean.setType(a.attributeDefinition.type);
		attrBean.setDescription(a.attributeDefinition.description);
		attrBean.setMultiValued(a.attributeDefinition.multiValued);
		attrBean.setName(a.attributeDefinition.name);
		attrBean.setSubAttributesWithMapping(a.attributeDefinition.subAttributesWithMapping.stream()
				.map(sa -> mapFromAttributeDefinitionWithMapping(sa)).collect(Collectors.toList()));

		bean.setAttributeDefinition(attrBean);
		bean.setAttributeMapping(mapToConfigurationMappingBean(a.attributeMapping));
		return bean;
	}

	SCIMEndpointConfiguration mapToConfigurationBean(SCIMServiceConfigurationBean bean)
	{
		return SCIMEndpointConfiguration.builder().withAllowedCorsHeaders(bean.getAllowedCORSheaders())
				.withAllowedCorsOrigins(bean.getAllowedCORSorigins())
				.withRootGroup(bean.getRootGroup().group.getPathEncoded())
				.withMembershipGroups(
						bean.getMembershipGroups().stream().map(g -> g.getPathEncoded()).collect(Collectors.toList()))

				.withSchemas(
						bean.getSchemas().stream().map(s -> mapToConfigurationSchema(s)).collect(Collectors.toList()))
				.withMembershipAttributes(bean.getMembershipAttributes()).build();
	}

	private SchemaWithMapping mapToConfigurationSchema(SchemaWithMappingBean schemaBean)
	{
		return SchemaWithMapping.builder().withName(schemaBean.getName()).withId(schemaBean.getId())
				.withType(schemaBean.getType()).withDescription(schemaBean.getDescription())
				.withEnable(schemaBean.isEnable()).withAttributesWithMapping(schemaBean.getAttributes().stream()
						.map(a -> mapToAttributeDefinition(a)).collect(Collectors.toList()))
				.build();

	}

	private AttributeDefinitionWithMapping mapToAttributeDefinition(AttributeDefinitionWithMappingBean attrDefBean)
	{
		return AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder()
						.withName(attrDefBean.getAttributeDefinition().getName())
						.withDescription(attrDefBean.getAttributeDefinition().getDescription())
						.withMultiValued(attrDefBean.getAttributeDefinition().isMultiValued())
						.withType(attrDefBean.getAttributeDefinition().getType())
						.withSubAttributesWithMapping(attrDefBean.getAttributeDefinition().getSubAttributesWithMapping()
								.stream().map(sa -> mapToAttributeDefinition(sa)).collect(Collectors.toList()))
						.build())
				.withAttributeMapping(mapToConfigurationMapping(attrDefBean.getAttributeMapping(),
						attrDefBean.getAttributeDefinition()))
				.build();
	}

	private AttributeMapping mapToConfigurationMapping(AttributeMappingBean bean,
			AttributeDefinitionBean attributeDefinition)
	{
		if (bean == null)
			return new UndefinedMapping();
		return bean.toConfiguration(attributeDefinition);
	}

	private AttributeMappingBean mapToConfigurationMappingBean(AttributeMapping mapping)
	{
		AttributeMappingBean bean = new AttributeMappingBean();
		if (mapping == null)
			return bean;
		return mapping.toBean();
	}

}
