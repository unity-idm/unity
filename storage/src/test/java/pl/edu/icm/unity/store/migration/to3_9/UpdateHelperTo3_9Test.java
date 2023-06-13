/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_9;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.JsonUtil;

public class UpdateHelperTo3_9Test
{
	@Test
	public void shouldSkipMigrationOfRegFormWithEmptySpec()
	{
		// given
		String regForm = "{"
				+ "    \"ExternalSignupSpec\" : {"
				+ "           \"specs\" : [ ]"
				+ "  }"
				+ "}";
		
		// when
		Optional<ObjectNode> result = UpdateHelperTo3_9.migrateExternalSignupSpec(JsonUtil.parse(regForm));
		
		// then
		Assertions.assertThat(result).isEmpty();
	}
	
	@Test
	public void shouldSkipMigrationOfRegFormWithProperFormat()
	{
		// given
		String regForm = "{"
				+ "    \"ExternalSignupSpec\" : {"
				+ "         \"specs\" : [ {"
				+ "            \"authenticatorKey\" : \"oauth\","
				+ "            \"optionKey\" : \"dropbox\""
				+ "          }, {"
				+ "            \"authenticatorKey\" : \"oauth\","
				+ "            \"optionKey\" : \"linkedin\""
				+ "          } ]"
				+ "  }"
				+ "}";
		
		// when
		Optional<ObjectNode> result = UpdateHelperTo3_9.migrateExternalSignupSpec(JsonUtil.parse(regForm));
		
		// then
		Assertions.assertThat(result).isEmpty();
	}
	
	@Test
	public void shouldMigrateSpecs()
	{
		// given
		String regForm = "{"
				+ "    \"ExternalSignupSpec\" : {"
				+ "         \"specs\" : [  \"oauth2.google\", \"oauth1.microsoft\" ]"
				+ "  }"
				+ "}";
		
		// when
		Optional<ObjectNode> result = UpdateHelperTo3_9.migrateExternalSignupSpec(JsonUtil.parse(regForm));
		
		// then
		Assertions.assertThat(result).isNotEmpty();
		JsonNode specs = result.get().get("ExternalSignupSpec").withArray("specs");
		Assertions.assertThat(specs).hasSize(2);
		Map<String, String> authKeyToOptionKey = new HashMap<>();
		specs.forEach(spec -> authKeyToOptionKey.put(spec.get("authenticatorKey").asText(), spec.get("optionKey").asText()));
		Assertions.assertThat(authKeyToOptionKey).isEqualTo(Map.of("oauth2", "google", "oauth1", "microsoft"));
	}
	
	@Test
	public void shouldMigrateProvidedSpec()
	{
		// given
		String regForm = "{"
				+ "    \"ExternalSignupSpec\" : {"
				+ "         \"specs\" : [  \"oauth2.google\", \"oauth1.microsoft\" ]"
				+ "  }"
				+ "}";
		
		// when
		ObjectNode inputSpec = JsonUtil.parse(regForm);
		Optional<ObjectNode> resultSpec = UpdateHelperTo3_9.migrateExternalSignupSpec(inputSpec);
		
		// then
		Assertions.assertThat(resultSpec).isNotEmpty();
		Assertions.assertThat(inputSpec).isEqualTo(resultSpec.get());
	}
}
