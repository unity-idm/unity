/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.stream.Collectors;

import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.AttributeMapping;
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
		bean.setRootGroup( new GroupWithIndentIndicator(new Group(orgConfig.rootGroup), false));
		bean.setSchemas(orgConfig.schemas.stream().map(s -> mapFromConfigurationSchema(s)).collect(Collectors.toList()));
		return bean;
	}

	static SchemaWithMappingBean mapFromConfigurationSchema(SchemaWithMapping schema)
	{
		SchemaWithMappingBean schemaBean = new SchemaWithMappingBean();
		schemaBean.setId(schema.id);
		schemaBean.setName(schema.name);
		schemaBean.setDescription(schema.description);
		schemaBean.setEnable(schema.enable);
		schemaBean.setAttributes(schema.attributesWithMapping.stream().map(a -> mapFromAttributeDefinitionWithMapping(a))
				.collect(Collectors.toList()));

		return schemaBean;
	}

	private static AttributeDefinitionWithMappingBean mapFromAttributeDefinitionWithMapping(AttributeDefinitionWithMapping a)
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

		// TODO
		bean.setAttributeMapping(new AttributeMappingBean());

		return bean;
	}

	static SCIMEndpointConfiguration mapToConfigurationBean(SCIMServiceConfigurationBean bean)
	{
		return SCIMEndpointConfiguration.builder().withAllowedCorsHeaders(bean.getAllowedCORSheaders())
				.withAllowedCorsOrigins(bean.getAllowedCORSorigins()).withRootGroup(bean.getRootGroup().group.getPathEncoded())
				.withMembershipGroups(
						bean.getMembershipGroups().stream().map(g -> g.getPathEncoded()).collect(Collectors.toList()))

				.withSchemas(bean.getSchemas().stream().map(s -> mapToConfigurationSchema(s)).collect(Collectors.toList())).build();
	}

	private static SchemaWithMapping mapToConfigurationSchema(SchemaWithMappingBean s)
	{
		return SchemaWithMapping.builder().withName(s.getName()).withId(s.getId()).withDescription(s.getDescription())
				.withEnable(s.isEnable())
				.withAttributesWithMapping(
						s.getAttributes().stream().map(a -> mapToAttributeDefinition(a)).collect(Collectors.toList()))
				.build();

	}

	private static AttributeDefinitionWithMapping mapToAttributeDefinition(AttributeDefinitionWithMappingBean a)
	{
		return AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(
						AttributeDefinition.builder().withName(a.getAttributeDefinition().getName())
								.withDescription(a.getAttributeDefinition().getDescription())
								.withMultiValued(a.getAttributeDefinition().isMultiValued())
								.withType(a.getAttributeDefinition().getType())
								.withSubAttributesWithMapping(a.getAttributeDefinition().getSubAttributesWithMapping()
										.stream().map(sa -> mapToAttributeDefinition(sa)).collect(Collectors.toList()))
								.build()
				// TODO
				).withAttributeMapping(AttributeMapping.builder().build()).build();
	}

}
