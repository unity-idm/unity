/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base code used by both {@link AttributeSerializer} and {@link FullAttributeSerializer}.
 * @author K. Benedyczak
 */
public abstract class AbstractAttributeSerializer
{
	private final ObjectMapper mapper = new ObjectMapper();
	
	protected <T> ObjectNode toJsonBase(Attribute<T> src)
	{
		ObjectNode root = mapper.createObjectNode();
		root.put("visibility", src.getVisibility().name());
		if (src.getRemoteIdp() != null)
			root.put("remoteIdp", src.getRemoteIdp());
		if (src.getTranslationProfile() != null)
			root.put("translationProfile", src.getTranslationProfile());
		ArrayNode values = root.putArray("values");
		AttributeValueSyntax<T> syntax = src.getAttributeSyntax();
		for (T value: src.getValues())
			values.add(syntax.serialize(value));
		return root;
	}
	
	protected <T> void fromJsonBase(ObjectNode main, Attribute<T> target)
	{
		target.setVisibility(AttributeVisibility.valueOf(main.get("visibility").asText()));
		
		if (main.has("translationProfile"))
			target.setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("remoteIdp"))
			target.setRemoteIdp(main.get("remoteIdp").asText());
		
		ArrayNode values = main.withArray("values");
		List<T> pValues = new ArrayList<T>(values.size());
		Iterator<JsonNode> it = values.iterator();
		AttributeValueSyntax<T> syntax = target.getAttributeSyntax();
		try
		{
			while(it.hasNext())
				pValues.add(syntax.deserialize(it.next().binaryValue()));
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		target.setValues(pValues);
	}

	
	public <T> void fromJson(byte[] json, AttributeExt<T> target)
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
		
		fromJsonBase(main, target);
		
		if (main.has("creationTs"))
			target.setCreationTs(new Date(main.get("creationTs").asLong()));
		if (main.has("updateTs"))
			target.setUpdateTs(new Date(main.get("updateTs").asLong()));
	}
}
