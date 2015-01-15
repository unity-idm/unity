/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.util.Iterator;
import java.util.Map;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.I18nString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper to (de)serialize {@link I18nString}.
 * @author K. Benedyczak
 */
public class I18nStringJsonUtil
{
	private static final ObjectMapper mapper = Constants.MAPPER;
	
	public static ObjectNode toJson(I18nString value)
	{
		ObjectNode root = mapper.createObjectNode();
		root.put("DefaultValue", value.getDefaultValue());
		ObjectNode mapN = mapper.createObjectNode();
		Map<String, String> map = value.getMap();
		for (Map.Entry<String, String> e: map.entrySet())
			mapN.put(e.getKey(), e.getValue());
		root.set("Map", mapN);
		return root;
	}
	
	public static I18nString fromJson(JsonNode node)
	{
		I18nString ret = new I18nString();
		return fromJson(ret, node);
	}

	public static I18nString fromJson(JsonNode node, JsonNode defaultVal)
	{
		String defaultValStr = defaultVal != null && !defaultVal.isNull() ? defaultVal.asText() : null;
		I18nString ret = new I18nString(defaultValStr);
		return fromJson(ret, node);
	}
	
	private static I18nString fromJson(I18nString ret, JsonNode node)
	{
		if (node == null || node.isNull())
			return ret;

		ObjectNode root = (ObjectNode) node;
		JsonNode defV = root.get("DefaultValue");
		ret.setDefaultValue(defV.isNull() ? null : defV.asText());
		ObjectNode mapN = (ObjectNode) root.get("Map");
		Iterator<String> fields = mapN.fieldNames();
		while (fields.hasNext())
		{
			String key = fields.next();
			JsonNode val = mapN.get(key);
			ret.addValue(key, val.isNull() ? null : val.asText());
		}
		return ret;
	}

}
