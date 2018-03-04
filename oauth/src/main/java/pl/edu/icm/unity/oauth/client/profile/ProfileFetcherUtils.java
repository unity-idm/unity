/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.oauth.client.AttributeFetchResult;

/**
 * Utils for profile fetchers
 * 
 * @author P.Piernik
 *
 */
public class ProfileFetcherUtils
{

	public static AttributeFetchResult fetchFromJsonObject(JSONObject jsonObject)
	{
		return new AttributeFetchResult(convertToFlatAttributes(jsonObject),
				convertToRawAttributes(jsonObject));
	}

	public static JSONObject convertToRawAttributes(JSONObject toConvert)
	{
		JSONObject res = new JSONObject(toConvert);
		resolveNestedJsonType(res);
		return res;
	}

	public static void resolveNestedJsonType(JSONObject jsonObject)

	{
		for (Entry<String, Object> entry : jsonObject.entrySet())
		{
			if (entry.getValue() == null)
				continue;
			Object value = JSONValue.parse(entry.getValue().toString());
			if (value instanceof JSONObject)
			{
				JSONObject jobject = (JSONObject) value;
				entry.setValue(jobject);
				resolveNestedJsonType(jobject);
			} else if (value instanceof JSONArray)
			{
				JSONArray jarray = (JSONArray) value;
				entry.setValue(jarray);
				resolveNestedJsonType(jarray);
			}
		}
	}

	public static void resolveNestedJsonType(JSONArray array)
	{
		
		ArrayList<Object> copy = new ArrayList<>(array);
		for (Object v : copy)
		{
			Object r = JSONValue.parse(v.toString());
			if (r instanceof JSONObject)
			{
				array.remove(v);
				JSONObject jobject = (JSONObject) r;
				array.add(jobject);
				resolveNestedJsonType(jobject);

			} else if (r instanceof JSONArray)
			{
				array.remove(v);
				JSONArray jarray = (JSONArray) r;
				array.add(jarray);
				resolveNestedJsonType(jarray);
			}
		}
	}

	public static Map<String, List<String>> convertToFlatAttributes(JSONObject profile)
	{
		Map<String, List<String>> ret = new HashMap<>();
		convertToFlatAttributes("", profile, ret);
		return ret;
	}

	public static Map<String, List<String>> convertToFlatAttributes(String prefix,
			JSONObject profile, Map<String, List<String>> ret)
	{
		for (Entry<String, Object> entry : profile.entrySet())
		{
			if (entry.getValue() == null)
				continue;
			Object value = JSONValue.parse(entry.getValue().toString());
			if (value instanceof JSONObject)
			{
				convertToFlatAttributes(prefix + entry.getKey() + ".",
						(JSONObject) value, ret);

			} else if (value instanceof JSONArray)
			{
				convertToFlatAttributes(prefix + entry.getKey() + ".",
						(JSONArray) value, ret);
			} else
			{
				ret.put(prefix + entry.getKey(), Arrays.asList(value.toString()));
			}

		}
		return ret;
	}

	public static Map<String, List<String>> convertToFlatAttributes(String prefix,
			JSONArray element, Map<String, List<String>> ret)
	{
		for (int i = 0; i < element.size(); i++)
		{
			Object value = JSONValue.parse(element.get(i).toString());
			if (value == null)
				continue;

			if (value instanceof JSONObject)
			{
				convertToFlatAttributes(prefix + i + ".", (JSONObject) value, ret);
			} else if (value instanceof JSONArray)
			{
				convertToFlatAttributes(prefix + i + ".", (JSONArray) value, ret);
			} else
			{
				ret.put(prefix + i, Arrays.asList(value.toString()));
			}

		}

		return ret;
	}
}
