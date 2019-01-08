/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.profile;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Map.Entry;

import org.junit.Test;

import com.nimbusds.jose.util.JSONObjectUtils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class ProfileFetcherTest
{

	@Test
	public void shouldResolveToJsonObjects() throws ParseException, IOException
	{

		JSONObject ob = JSONObjectUtils.parse(new String(Files
				.readAllBytes(Paths.get("src/test/resources/orcidOutput.json"))));
		JSONObject converted = ProfileFetcherUtils.convertToRawAttributes(ob);
		assertThat(converted.entrySet().size(), is(ob.entrySet().size()));

		JSONArray res = (JSONArray) getFromObject((JSONObject) getFromObject(
				(JSONObject) getFromObject((JSONObject) getFromObject(converted,
						"orcid-profile"), "orcid-bio"),
				"contact-details"), "email");

		assertThat(res.size(), is(2));
	}

	private Object getFromObject(JSONObject source, String key)
	{
		for (Entry<String, Object> entry : source.entrySet())
		{
			if (entry.getKey().equals(key))
			{
				return entry.getValue();
			}
		}
		return null;
	}

}
