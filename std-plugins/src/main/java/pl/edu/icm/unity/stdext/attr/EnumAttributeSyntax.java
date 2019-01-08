/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

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
	
	/**
	 * Utility: converts string values to enums list
	 * @param type
	 * @param vals
	 * @return
	 */
	public static <T extends Enum<T>> List<T> getEnumValues(Class<T> type, List<String> vals)
	{
		if (vals == null)
			return null;
		List<T> ret = new ArrayList<T>();
		Map<String, T> enumVals = new HashMap<String, T>();
		T[] constants = type.getEnumConstants();
		for (T label: constants) 
			enumVals.put(label.name(), label);
		
		for (String val: vals)
			ret.add(enumVals.get(val));
		return ret;
	}
	
	public Set<String> getAllowed()
	{
		return allowed;
	}

	public void setAllowed(String... allowed)
	{
		if (allowed.length == 0)
			throw new IllegalArgumentException("At least one enumeration value must be defined");
		this.allowed = new HashSet<String>(allowed.length);
		for (String allow: allowed)
			this.allowed.add(allow);
		this.allowed = Collections.unmodifiableSet(this.allowed);
	}

	public void setAllowed(Set<String> allowed)
	{
		if (allowed.isEmpty())
			throw new IllegalArgumentException("At least one enumeration value must be defined");
		this.allowed = new HashSet<String>(allowed.size());
		for (String allow: allowed)
			this.allowed.add(allow);
		this.allowed = Collections.unmodifiableSet(this.allowed);
	}

	@Override
	public JsonNode getSerializedConfiguration()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		ArrayNode allow = main.putArray("allowed");
		for (String a: allowed)
			allow.add(a);
		return main;
	}

	@Override
	public void setSerializedConfiguration(JsonNode jsonN)
	{
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
		if (allowed == null)
			throw new IllegalAttributeValueException("Trying to validate enum attribute value on not configured syntax");
		if (!allowed.contains(value))
			throw new IllegalAttributeValueException("The value is not a valid enumeration element");
	}
	
	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<String>
	{
		public Factory()
		{
			super(EnumAttributeSyntax.ID, EnumAttributeSyntax::new);
		}
	}
}
