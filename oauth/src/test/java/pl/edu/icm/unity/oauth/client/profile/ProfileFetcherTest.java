/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.util.JSONObjectUtils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class ProfileFetcherTest
{
	
	@Test
	public void testHandleNullValues() throws Exception {
		String s = "{\"test-field\": \"null\"}";
		JSONParser p = new JSONParser(JSONParser.MODE_RFC4627);
		JSONObject o = (JSONObject)p.parse(s);
		Map<String,List<String>> m = ProfileFetcherUtils.convertToAttributes(o);
		assertThat(m.size()).isEqualTo(0);
	}
	

	@Test
	public void shouldResolveToJsonObjects() throws ParseException, IOException
	{
		JSONObject ob = new JSONObject(JSONObjectUtils.parse(new String(Files
				.readAllBytes(Paths.get("src/test/resources/orcidOutput.json")))));
		JSONObject converted = ProfileFetcherUtils.convertToRawAttributes(ob);
		assertThat(converted.entrySet().size()).isEqualTo(ob.entrySet().size());

		JSONArray res = (JSONArray) getFromObject(
					(JSONObject) getFromObject(
						(JSONObject) getFromObject(
							(JSONObject) getFromObject(converted, "orcid-profile"), 
							"orcid-bio"),
						"contact-details"), 
					"email");

		assertThat(res.size()).isEqualTo(2);
	}

	private Object getFromObject(JSONObject source, String key)
	{
		if (source == null)
			throw new IllegalArgumentException(key + " source is null");
		return source.get(key);
	}

}
