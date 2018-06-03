/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;

public class JsonUtilTest
{
	@Test
	public void shouldConvertToString()
			throws JsonParseException, JsonMappingException, IOException
	{
		// given
		AuthenticationFlowDefinition authn = new AuthenticationFlowDefinition("flow",
				Policy.REQUIRE, new HashSet<String>(Arrays.asList("primary")),
				new ArrayList<String>(Arrays.asList("secondary")));

		// when
		String jsonString = JsonUtil.toJsonString(authn);

		// then
		ObjectNode json = Constants.MAPPER.readValue(jsonString, ObjectNode.class);
		assertThat(json.get("firstFactorAuthenticators").get(0).asText(),
				equalTo("primary"));
		assertThat(json.get("secondFactorAuthenticators").get(0).asText(),
				equalTo("secondary"));
		System.out.println(json);

	}

	@Test
	public void shouldConvertToGivenType()
	{
		// given
		String jsonString = "{\"name\":\"flow\",\"firstFactorAuthenticators\":[\"primary\"],\"secondFactorAuthenticators\":[\"secondary\"],\"policy\":\"REQUIRE\"}";

		// when
		AuthenticationFlowDefinition authn = JsonUtil.parse(jsonString,
				AuthenticationFlowDefinition.class);

		// then
		assertThat(authn, equalTo(new AuthenticationFlowDefinition("flow", Policy.REQUIRE,
				new HashSet<String>(Arrays.asList("primary")),
				new ArrayList<String>(Arrays.asList("secondary")))));
	}

	@Test
	public void shouldConvertToList()
	{
		// given
		String jsonString = "["
				+ "{\"name\":\"flow1\",\"firstFactorAuthenticators\":[\"primary\"],\"secondFactorAuthenticators\":[\"secondary\"],\"policy\":\"REQUIRE\"}"
				+ ","
				+ "{\"name\":\"flow2\",\"firstFactorAuthenticators\":[\"primary\"],\"secondFactorAuthenticators\":[\"secondary\"],\"policy\":\"REQUIRE\"}"

				+ "]";

		// when
		List<AuthenticationFlowDefinition> authn = JsonUtil.parseToList(jsonString,
				AuthenticationFlowDefinition.class);

		// then
		assertThat(authn.size(), equalTo(2));
		assertThat(authn, hasItems(
				new AuthenticationFlowDefinition("flow1", Policy.REQUIRE,
						new HashSet<String>(Arrays.asList("primary")),
						new ArrayList<String>(Arrays.asList("secondary"))),
				new AuthenticationFlowDefinition("flow2", Policy.REQUIRE,
						new HashSet<String>(Arrays.asList("primary")),
						new ArrayList<String>(
								Arrays.asList("secondary")))));
	}
}
