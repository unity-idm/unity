/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.base.attributes;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.base.utils.JsonSerializer;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * @author K. Benedyczak
 */
@Component
public class AttributeTypeSerializer implements JsonSerializer<AttributeType>
{
	private final ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	private AttributeSyntaxFactoriesRegistry typesRegistry;
	
	public ObjectNode toJsonNode(AttributeType src)
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
		return root;
	}
	
	/**
	 * As {@link #toJsonNode(AttributeType)} but also adds information about attribute type name and syntax
	 * @param src
	 * @return
	 */
	@Override
	public ObjectNode toJson(AttributeType src)
	{
		ObjectNode root = toJsonNode(src);
		root.put("name", src.getName());
		root.put("syntaxId", src.getValueType().getValueSyntaxId());
		return root;
	}	
	
	public void fromJson(ObjectNode main, AttributeType target)
	{
		target.setFlags(main.get("flags").asInt());
		target.setMaxElements(main.get("maxElements").asInt());
		target.setMinElements(main.get("minElements").asInt());
		target.setSelfModificable(main.get("selfModificable").asBoolean());
		target.setUniqueValues(main.get("uniqueValues").asBoolean());
		target.setVisibility(AttributeVisibility.valueOf(main.get("visibility").asText()));
		target.getValueType().setSerializedConfiguration(main.get("syntaxState").asText());
		target.setDisplayedName(I18nStringJsonUtil.fromJson(main.get("displayedName")));
		if (target.getDisplayedName().getDefaultValue() == null)
			target.getDisplayedName().setDefaultValue(target.getName());
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
	
	@Override
	public AttributeType fromJson(ObjectNode main) 
	{
		String name = main.get("name").asText();
		AttributeValueSyntaxFactory<?> syntax = typesRegistry.getByName(main.get("syntaxId").asText());
		AttributeType newAT = new AttributeType(name, syntax.createInstance());
		fromJson(main, newAT);
		return newAT;
	}
}
