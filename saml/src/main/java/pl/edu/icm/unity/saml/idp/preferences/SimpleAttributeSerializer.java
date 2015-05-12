/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Serializes an attribute name and its values.
 * @author K. Benedyczak
 */
public class SimpleAttributeSerializer
{
	private final ObjectMapper mapper = new ObjectMapper();
	private AttributeSyntaxFactoriesRegistry syntaxReg;
	
	public SimpleAttributeSerializer(AttributeSyntaxFactoriesRegistry syntaxReg)
	{
		this.syntaxReg = syntaxReg;
	}

	public <T> ObjectNode toJson(Attribute<T> src)
	{
		ObjectNode root = mapper.createObjectNode();
		root.put("name", src.getName());
		root.put("syntax", src.getAttributeSyntax().getValueSyntaxId());
		ArrayNode values = root.putArray("values");
		AttributeValueSyntax<T> syntax = src.getAttributeSyntax();
		for (T value: src.getValues())
			values.add(syntax.serialize(value));
		return root;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> Attribute<T> fromJson(ObjectNode main) throws IllegalTypeException
	{
		if (main == null)
			return null;
		
		String name = main.get("name").asText();
		String syntaxId = main.get("syntax").asText();
		AttributeValueSyntax<T> syntax = (AttributeValueSyntax<T>) 
				syntaxReg.getByName(syntaxId).createInstance();
		
		ArrayNode values = main.withArray("values");
		List<T> pValues = new ArrayList<T>(values.size());
		Iterator<JsonNode> it = values.iterator();
		try
		{
			while(it.hasNext())
				pValues.add(syntax.deserialize(it.next().binaryValue()));
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		
		return new Attribute(name, syntax, "/", AttributeVisibility.full, pValues);
	}
}
