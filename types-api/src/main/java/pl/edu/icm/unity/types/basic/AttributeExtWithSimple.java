/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;

/**
 * Used on REST endpoint, available in types module for rest clients.
 */
public class AttributeExtWithSimple extends AttributeExt
{
	private List<String> simpleValues;

	public AttributeExtWithSimple(AttributeExt source)
	{
		this(source, null);
	}

	public AttributeExtWithSimple(AttributeExt source, List<String> simpleValues)
	{
		super(source);
		this.simpleValues = simpleValues;
	}
	
	public List<String> getSimpleValues()
	{
		return simpleValues;
	}

	@JsonCreator
	public AttributeExtWithSimple(ObjectNode src)
	{
		super(src);
		if (JsonUtil.notNull(src, "simpleValues"))
		{
			ArrayNode values = src.withArray("simpleValues");
			this.simpleValues = new ArrayList<>(values.size());
			Iterator<JsonNode> it = values.iterator();
			while (it.hasNext())
				this.simpleValues.add(it.next().asText());
		}
	}

	@Override
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode ret = super.toJson();
		if (simpleValues != null && !simpleValues.isEmpty())
		{
			ArrayNode simpleValuesNode = ret.putArray("simpleValues");
			for (String value : simpleValues)
				simpleValuesNode.add(value);
		}
		return ret;
	}
}
