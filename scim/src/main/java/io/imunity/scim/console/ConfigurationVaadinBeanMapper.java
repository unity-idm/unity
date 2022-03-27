/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.stream.Collectors;

import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.AttributeMapping;
import io.imunity.scim.config.NotDefinedMapping;
import io.imunity.scim.config.SCIMEndpointConfiguration;
import io.imunity.scim.config.SchemaWithMapping;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;

class ConfigurationVaadinBeanMapper
{
	static SCIMServiceConfigurationBean mapToBean(SCIMEndpointConfiguration orgConfig)
	{
		SCIMServiceConfigurationBean bean = new SCIMServiceConfigurationBean();
		bean.getAllowedCORSheaders().addAll(orgConfig.allowedCorsHeaders);
		bean.getAllowedCORSorigins().addAll(orgConfig.allowedCorsOrigins);
		bean.getMembershipGroups()
				.addAll(orgConfig.membershipGroups.stream().map(g -> new Group(g)).collect(Collectors.toList()));
		bean.setRootGroup(new GroupWithIndentIndicator(new Group(orgConfig.rootGroup), false));
		bean.setSchemas(
				orgConfig.schemas.stream().map(s -> mapFromConfigurationSchema(s)).collect(Collectors.toList()));
		bean.setMembershipAttributes(orgConfig.membershipAttributes);
		return bean;
	}

	static SchemaWithMappingBean mapFromConfigurationSchema(SchemaWithMapping schema)
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

	private static AttributeDefinitionWithMappingBean mapFromAttributeDefinitionWithMapping(
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

	static SCIMEndpointConfiguration mapToConfigurationBean(SCIMServiceConfigurationBean bean)
	{
		return SCIMEndpointConfiguration.builder().withAllowedCorsHeaders(bean.getAllowedCORSheaders())
				.withAllowedCorsOrigins(bean.getAllowedCORSorigins())
				.withRootGroup(bean.getRootGroup().group.getPathEncoded())
				.withMembershipGroups(
						bean.getMembershipGroups().stream().map(g -> g.getPathEncoded()).collect(Collectors.toList()))

				.withSchemas(
						bean.getSchemas().stream().map(s -> mapToConfigurationSchema(s)).collect(Collectors.toList()))
				.withMembershipAttributes(bean.getMembershipAttributes())
				.build();
	}

	private static SchemaWithMapping mapToConfigurationSchema(SchemaWithMappingBean schemaBean)
	{
		return SchemaWithMapping.builder().withName(schemaBean.getName()).withId(schemaBean.getId())
				.withType(schemaBean.getType()).withDescription(schemaBean.getDescription())
				.withEnable(schemaBean.isEnable()).withAttributesWithMapping(schemaBean.getAttributes().stream()
						.map(a -> mapToAttributeDefinition(a)).collect(Collectors.toList()))
				.build();

	}

	private static AttributeDefinitionWithMapping mapToAttributeDefinition(
			AttributeDefinitionWithMappingBean attrDefBean)
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
				.withAttributeMapping(
						mapToConfigurationMapping(attrDefBean.getAttributeMapping(), attrDefBean.getAttributeDefinition()))
				.build();
	}

	private static AttributeMapping mapToConfigurationMapping(AttributeMappingBean bean, AttributeDefinitionBean attributeDefinition)
	{
		if (bean == null)
			return new NotDefinedMapping();
		return bean.toConfiguration(attributeDefinition);
	}

	private static AttributeMappingBean mapToConfigurationMappingBean(AttributeMapping mapping)
	{
		AttributeMappingBean bean = new AttributeMappingBean();
		if (mapping == null )
			return bean;
		return mapping.toBean();
	}

	
}
