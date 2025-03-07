/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.client.AttributeFetchResult;

/**
 * Utils for profile fetchers
 * 
 * @author P.Piernik
 *
 */
public class ProfileFetcherUtils
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH,
			ProfileFetcherUtils.class);
	
	public static AttributeFetchResult fetchFromJsonObject(JSONObject jsonObject)
	{
		return new AttributeFetchResult(convertToAttributes(jsonObject),
				convertToRawAttributes(jsonObject));
	}

	public static Map<String, List<String>> convertToAttributes(JSONObject jsonObject)
	{
		JSONObject profile = parseToMiniDevJsonObject(jsonObject);
		Map<String, List<String>> ret = new HashMap<>();
		if (profile == null)
			return ret;

		for (Entry<String, Object> entry : profile.entrySet())
		{
			if (entry.getValue() == null)
				continue;
			Object value = JSONValue.parse(entry.getValue().toString());
			if (value==null)
				continue;
			
			if (value instanceof JSONObject)
			{
				ret.put(entry.getKey(), Arrays.asList(value.toString()));

			} else if (value instanceof JSONArray)
			{
				ArrayList<String> vList = new ArrayList<>();
				for (Object v : (JSONArray) value)
				{
					vList.add(v.toString());
				}

				ret.put(entry.getKey(), vList);
			} else
			{
				ret.put(entry.getKey(), Arrays.asList(value.toString()));
			}

		}
		return ret;
	}
	
	static JSONObject convertToRawAttributes(JSONObject toConvert)
	{
		JSONObject res = parseToMiniDevJsonObject(toConvert);
		resolveNestedJsonType(res);
		return res;
	}

	//workaround for GSON parsing
	private static JSONObject parseToMiniDevJsonObject(JSONObject toConvert)
	{
		try
		{
			return (JSONObject) JSONValue.parseWithException(toConvert.toJSONString());
		} catch (ParseException e)
		{
			log.debug("Can not parse to JSONObject" ,e);
			return null;
		}
	}
	
	private static void resolveNestedJsonType(JSONObject jsonObject)
	{
		if (jsonObject == null)
			return;
		
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

	private static void resolveNestedJsonType(JSONArray array)
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
}
