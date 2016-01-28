/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;

public class UnityTypesFactoryTest
{
	@Test
	public void shouldConvertToString() throws JsonParseException, JsonMappingException, IOException
	{
		// given
		AuthenticationOptionDescription authn =  new AuthenticationOptionDescription("primary", "secondary");

		// when
		String jsonString = UnityTypesFactory.toJsonString(authn);

		// then
		ObjectNode json = Constants.MAPPER.readValue(jsonString, ObjectNode.class);
		assertThat(json.get("primaryAuthenticator").asText(), equalTo("primary"));
		assertThat(json.get("mandatory2ndAuthenticator").asText(), equalTo("secondary"));
	}

	@Test
	public void shouldConvertToGivenType()
	{
		// given
		String jsonString = "{\"primaryAuthenticator\":\"one\",\"mandatory2ndAuthenticator\":\"two\"}";

		// when
		 AuthenticationOptionDescription authn = UnityTypesFactory.parse(jsonString, AuthenticationOptionDescription.class);

		// then
		assertThat(authn, equalTo(new AuthenticationOptionDescription("one", "two")));
	}
}
