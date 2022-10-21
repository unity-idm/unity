/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SCIMEndpointPropertiesConfigurationMapperTest
{

	@Test
	public void shouldOverwriteSchemasFromFile()
	{
		String properties = "unity.endpoint.scim.rootGroup=/\n" + "unity.endpoint.scim.restAdminGroup=/\n"
				+ "unity.endpoint.scim.membershipGroups.1=/\n"
				+ "unity.endpoint.scim.schemas.1={\\\"id\\\":\\\"custom\\\",\\\"type\\\":\\\"USER\\\","
				+ "\\\"name\\\":\\\"CustomFromConfig\\\",\\\"description\\\":\\\"\\\",\\\"enable\\\":true,"
				+ "\\\"attributesWithMapping\\\":[{\\\"attributeDefinition\\\":{\\\"name\\\":\\\"login\\\","
				+ "\\\"type\\\":\\\"STRING\\\",\\\"description\\\":\\\"\\\",\\\"subAttributesWithMapping\\\":[],"
				+ "\\\"multiValued\\\":false},\\\"attributeMapping\\\":{\\\"mappingType\\\":\\\"Simple\\\","
				+ "\\\"dataArray\\\":null,\\\"dataValue\\\":{\\\"type\\\":\\\"IDENTITY\\\",\\\"value\\\":\\\"userName\\\"},"
				+ "\\\"evaluatorId\\\":\\\"Simple\\\"}}]} \n"
				+ "unity.endpoint.scim.schemasFile.1=src/test/resources/UserCustom.json\n";

		SCIMEndpointConfiguration fromProperties = SCIMEndpointPropertiesConfigurationMapper.fromProperties(properties);

		assertThat(fromProperties.schemas.get(0).name, is("CustomFromFile"));
	}

	@Test
	public void shouldLoadSchemasFromFileAndFromConfig()
	{
		String properties = "unity.endpoint.scim.rootGroup=/\n" + "unity.endpoint.scim.restAdminGroup=/\n"
				+ "unity.endpoint.scim.membershipGroups.1=/\n"
				+ "unity.endpoint.scim.schemas.1={\\\"id\\\":\\\"customFromConfig\\\",\\\"type\\\":\\\"USER\\\","
				+ "\\\"name\\\":\\\"CustomFromConfig\\\",\\\"description\\\":\\\"\\\",\\\"enable\\\":true,"
				+ "\\\"attributesWithMapping\\\":[{\\\"attributeDefinition\\\":{\\\"name\\\":\\\"login\\\","
				+ "\\\"type\\\":\\\"STRING\\\",\\\"description\\\":\\\"\\\",\\\"subAttributesWithMapping\\\":[],"
				+ "\\\"multiValued\\\":false},\\\"attributeMapping\\\":{\\\"mappingType\\\":\\\"Simple\\\","
				+ "\\\"dataArray\\\":null,\\\"dataValue\\\":{\\\"type\\\":\\\"IDENTITY\\\",\\\"value\\\":\\\"userName\\\"},"
				+ "\\\"evaluatorId\\\":\\\"Simple\\\"}}]} \n"
				+ "unity.endpoint.scim.schemasFile.1=src/test/resources/UserCustom.json\n";
		SCIMEndpointConfiguration fromProperties = SCIMEndpointPropertiesConfigurationMapper.fromProperties(properties);
		assertThat(fromProperties.schemas.size(), is(2));
	}
}
