/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.profile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * Utils for profile fetchers
 * 
 * @author P.Piernik
 *
 */
public class ProfileFetcherUtils
{
	
	public static Map<String, List<String>> convertToFlatAttributes(JSONObject profile,
			boolean deepResolve)
	{
		Map<String, List<String>> ret = new HashMap<>();
		convertToFlatAttributes("", profile, ret, deepResolve);
		return ret;
	}

	public static Map<String, List<String>> convertToFlatAttributes(String prefix,
			JSONObject profile, Map<String, List<String>> ret, boolean deepResolve)
	{
		for (Entry<String, Object> entry : profile.entrySet())
		{
			if (entry.getValue() == null)
				continue;
			Object value = JSONValue.parse(entry.getValue().toString());
			if (value instanceof JSONObject)
			{
				if (!deepResolve)
				{
					JSONObject json = (JSONObject) value;
					for (Entry<String, Object> jsonEntry : json.entrySet())
					{
						ret.put(entry.getKey() + "." + jsonEntry.getKey(),
								Arrays.asList(jsonEntry.getValue()
										.toString()));
					}
				} else
				{
					convertToFlatAttributes(prefix + entry.getKey() + ".",
							(JSONObject) value, ret, deepResolve);
				}

			} else if (value instanceof JSONArray)
			{
				convertToFlatAttributes(prefix + entry.getKey(), (JSONArray) value,
						ret, deepResolve);
			} else
			{
				ret.put(prefix + entry.getKey(), Arrays.asList(value.toString()));
			}

		}
		return ret;
	}

	public static Map<String, List<String>> convertToFlatAttributes(String prefix,
			JSONArray element, Map<String, List<String>> ret, boolean deepResolve)
	{
		if (!deepResolve)
		{

			ret.put(prefix, element.stream().map(o -> o.toString())
					.collect(Collectors.toList()));
			return ret;

		}
		String deepPrefix = prefix + ".";

		for (int i = 0; i < element.size(); i++)
		{
			Object value = element.get(i);
			if (value == null)
				continue;

			if (value instanceof JSONObject)
			{
				convertToFlatAttributes(deepPrefix + i + ".", (JSONObject) value,
						ret, deepResolve);
			} else if (value instanceof JSONArray)
			{
				convertToFlatAttributes(deepPrefix + i + ".", (JSONArray) value,
						ret, deepResolve);
			} else
			{
				ret.put(deepPrefix + i, Arrays.asList(value.toString()));
			}

		}

		return ret;
	}
}
