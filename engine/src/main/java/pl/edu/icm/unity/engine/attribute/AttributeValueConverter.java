/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Performs conversion of {@link Attribute} values in a convenient to use way.
 * Under the hood delegates to proper attribute syntax.
 * @author K. Benedyczak
 */
@Component
public class AttributeValueConverter
{
	@Autowired
	private AttributeTypeHelper atHelper;
	
	/**
	 * Converts a list of external values to the internal representation which is ready to be stored in database 
	 * @param attributeName
	 * @param externalValues
	 * @return
	 * @throws IllegalAttributeValueException 
	 */
	public List<String> externalValuesToInternal(String attributeName, List<?> externalValues) 
			throws IllegalAttributeValueException
	{
		AttributeValueSyntax<?> syntax = atHelper.getSyntaxForAttributeName(attributeName);
		return externalValuesToInternal(syntax, externalValues);
	}

	/**
	 * As {@link #externalValuesToInternal(String, List)} but requires full syntax as argument
	 * @param syntax
	 * @param externalValues
	 * @return
	 * @throws IllegalAttributeValueException
	 */
	public <T> List<String> externalValuesToInternal(AttributeValueSyntax<T> syntax, List<?> externalValues) 
			throws IllegalAttributeValueException
	{
		List<String> ret = new ArrayList<>(externalValues.size());
		for (Object o: externalValues)
		{
			T deserialized = syntax.deserializeSimple(o.toString());
			ret.add(syntax.convertToString(deserialized));
		}
		return ret;
	}
	
	/**
	 * Converts a list of internal values to the external representation which is ready to be exposed 
	 * to outside world. 
	 * @param attributeName
	 * @param internalValues
	 * @return
	 */
	public List<String> internalValuesToExternal(String attributeName, List<String> internalValues) 
	{
		AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntaxForAttributeName(attributeName);
		return internalValuesToExternal(syntax, internalValues);
	}

	/**
	 * As {@link #internalValuesToExternal(AttributeValueSyntax, List)} but requires full syntax object as argument
	 * @param syntax
	 * @param internalValues
	 * @return
	 */
	public <T> List<String> internalValuesToExternal(AttributeValueSyntax<T> syntax,
			List<String> internalValues)
	{
		List<String> ret = new ArrayList<>(internalValues.size());
		for (String o: internalValues)
		{
			T deserialized = syntax.convertFromString(o);
			ret.add(syntax.serializeSimple(deserialized));
		}
		return ret;
	}
	
	/**
	* Converts a list of internal values to the object value
	 * @param attributeName
	 * @param internalValue
	 * @return
	 * @throws IllegalAttributeValueException
	 */
	public <T> List<?> internalValuesToObjectValues(String attributeName, List<String> internalValues) 
			throws IllegalAttributeValueException
	{
		AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntaxForAttributeName(attributeName);
		return internalValuesToObjectValues(syntax, internalValues);
	}
	
	/**
	 * As {@link #internalValuesToObjectValues(AttributeValueSyntax, List)} but requires full syntax object as argument
	 * @param syntax
	 * @param internalValues
	 * @return
	 */
	public <T> List<?> internalValuesToObjectValues(AttributeValueSyntax<T> syntax, List<String> internalValues) 
			throws IllegalAttributeValueException
	{
		List<T> ret = new ArrayList<>(internalValues.size());
		for (String internal : internalValues)
		{
			T deserialized = syntax.convertFromString(internal);
			ret.add(deserialized);
		}
		return ret;
	}	
}
