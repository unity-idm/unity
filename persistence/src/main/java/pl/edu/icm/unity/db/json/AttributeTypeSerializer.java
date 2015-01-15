/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * @author K. Benedyczak
 */
@Component
public class AttributeTypeSerializer
{
	private final ObjectMapper mapper = new ObjectMapper();

	/**
	 * {@inheritDoc}
	 */
	public byte[] toJson(AttributeType src)
	{
		ObjectNode root = mapper.createObjectNode();
		root.put("flags", src.getFlags());
		root.put("maxElements", src.getMaxElements());
		root.put("minElements", src.getMinElements());
		root.put("selfModificable", src.isSelfModificable());
		root.put("uniqueValues", src.isUniqueValues());
		root.put("visibility", src.getVisibility().name());
		root.put("syntaxState", src.getValueType().getSerializedConfiguration());
		root.set("displayedName", I18nStringJsonUtil.toJson(src.getDisplayedName()));
		root.set("i18nDescription", I18nStringJsonUtil.toJson(src.getDescription()));
		ObjectNode metaN = root.putObject("metadata");
		for (Map.Entry<String, String> entry: src.getMetadata().entrySet())
			metaN.put(entry.getKey(), entry.getValue());
		try
		{
			return mapper.writeValueAsBytes(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void fromJson(byte[] json, AttributeType target)
	{
		if (json == null)
			return;
		ObjectNode main;
		try
		{
			main = mapper.readValue(json, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		target.setFlags(main.get("flags").asInt());
		target.setMaxElements(main.get("maxElements").asInt());
		target.setMinElements(main.get("minElements").asInt());
		target.setSelfModificable(main.get("selfModificable").asBoolean());
		target.setUniqueValues(main.get("uniqueValues").asBoolean());
		target.setVisibility(AttributeVisibility.valueOf(main.get("visibility").asText()));
		target.getValueType().setSerializedConfiguration(main.get("syntaxState").asText());
		target.setDisplayedName(I18nStringJsonUtil.fromJson(main.get("displayedName")));
		target.setDescription(I18nStringJsonUtil.fromJson(main.get("i18nDescription"), 
				main.get("description")));
		if (main.has("metadata"))
		{
			JsonNode metaNode = main.get("metadata");
			Iterator<Entry<String, JsonNode>> it = metaNode.fields();
			Map<String, String> meta = target.getMetadata();
			while(it.hasNext())
			{
				Entry<String, JsonNode> entry = it.next();
				meta.put(entry.getKey(), entry.getValue().asText());
			}	
		}
	}
}
