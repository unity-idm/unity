/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import static io.imunity.scim.config.SCIMEndpointProperties.PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;

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

		assertThat(fromProperties.schemas.get(0).name).isEqualTo("CustomFromFile");
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
		assertThat(fromProperties.schemas.size()).isEqualTo(2);
	}

	@Test
	public void shouldSerialize() throws JsonProcessingException
	{
		Properties sourceCfg = ConfigurationGenerator
				.generateCompleteWithNonDefaults(SCIMEndpointProperties.PREFIX, SCIMEndpointProperties.META)
				.update("allowedCorsOrigins.1", "abc")
				.update("allowedCorsOrigins.2", "cde")
				.update("allowedCorsHeaders.1", "zzz")
				.update("allowedCorsHeaders.2", "sss")
				.get();

		SCIMEndpointConfiguration fromProperties = SCIMEndpointPropertiesConfigurationMapper
				.fromProperties(ConfigurationComparator.getAsString(sourceCfg));

		String converted = SCIMEndpointPropertiesConfigurationMapper.toProperties(fromProperties);

		Properties result = ConfigurationComparator.fromString(converted, PREFIX)
				.get();
		assertThat(result.get(PREFIX + "allowedCorsOrigins.1")).isEqualTo("abc");
		assertThat(result.get(PREFIX + "allowedCorsOrigins.2")).isEqualTo("cde");
		assertThat(result.get(PREFIX + "allowedCorsHeaders.1")).isEqualTo("zzz");
		assertThat(result.get(PREFIX + "allowedCorsHeaders.2")).isEqualTo("sss");
	}
}
