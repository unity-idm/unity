/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;

/**
 * Enumeration attribute syntax. Accepts strings which are from a defined set.
 * Case sensitive.
 * @author K. Benedyczak
 */
public class EnumAttributeSyntax extends AbstractStringAttributeSyntax
{
	public static final String ID = "enumeration";
	private Set<String> allowed;
	
	public EnumAttributeSyntax()
	{
	}
	
	public EnumAttributeSyntax(String... allowed)
	{
		setAllowed(allowed);
	}
	
	public EnumAttributeSyntax(Set<String> allowed)
	{
		setAllowed(allowed);
	}
	
	public Set<String> getAllowed()
	{
		return allowed;
	}

	public void setAllowed(String... allowed)
	{
		this.allowed = new HashSet<String>(allowed.length);
		for (String allow: allowed)
			this.allowed.add(allow);
		this.allowed = Collections.unmodifiableSet(this.allowed);
	}

	public void setAllowed(Set<String> allowed)
	{
		this.allowed = new HashSet<String>(allowed.size());
		for (String allow: allowed)
			this.allowed.add(allow);
		this.allowed = Collections.unmodifiableSet(this.allowed);
	}

	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		ArrayNode allow = main.putArray("allowed");
		for (String a: allowed)
			allow.add(a);
		try
		{
			return Constants.MAPPER.writeValueAsString(main);
		} catch (JsonProcessingException e)
		{
			throw new RuntimeEngineException("Can't serialize EnumAttributeSyntax to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		JsonNode jsonN;
		try
		{
			jsonN = Constants.MAPPER.readTree(json);
		} catch (Exception e)
		{
			throw new RuntimeEngineException("Can't deserialize StringAttributeSyntax from JSON", e);
		}
		ArrayNode allow = (ArrayNode) jsonN.get("allowed");
		this.allowed = new HashSet<String>(allow.size());
		for (int i=0; i<allow.size(); i++)
			this.allowed.add(allow.get(i).asText());
		this.allowed = Collections.unmodifiableSet(this.allowed);
	}

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public void validate(String value) throws IllegalAttributeValueException
	{
		if (!allowed.contains(value))
			throw new IllegalAttributeValueException("The value is not a valid enumeration element");
	}
}
