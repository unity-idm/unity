/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;

/**
 * Implementations defines identity type. 
 * @author K. Benedyczak
 */
public interface IdentityTypeDefinition
{
	/**
	 * @return type id
	 */
	public String getId();
	
	/**
	 * @return identity type default description
	 */
	public String getDefaultDescription();
	
	/**
	 * @return if true then the identity type is special, and can not be neither created or removed manually.
	 */
	public boolean isSystem();
	
	/**
	 * 
	 * @return set of attribute types that can be extracted from the identity of this type.
	 * It can be assumed that at least name, description and syntax are set. The attribute types from this 
	 * set need not be defined in the system.
	 */
	public Set<AttributeType> getAttributesSupportedForExtraction();
	
	/**
	 * Validates if the value is valid
	 * @param value
	 * @throws IllegalIdentityValueException
	 */
	public void validate(String value) throws IllegalIdentityValueException;
	
	/**
	 * Comparable value must be guaranteed to be unique for the type, i.e. if two
	 * values are the same (case sensitive), then the identities represent the same principal. 
	 * @param from
	 * @return
	 */
	public String getComparableValue(String from);
	
	/**
	 * Extract provided attributes.
	 * @param toExtract map: the keys are attribute names as returned by the 
	 * {@link #getAttributesSupportedForExtraction()}. Values are actual names of attribute names to be used
	 * for each extracted.
	 * @return
	 */
	public List<Attribute<?>> extractAttributes(String from, Map<String, String> toExtract);
	
	/**
	 * Similar to {@link #toString()}, but allows for less verbose
	 * and more user-friendly output.
	 * @return
	 */
	public String toPrettyString(String from);

	/**
	 * Similar to {@link #toPrettyString()}, but doesn't return id type prefix.
	 * @return
	 */
	public String toPrettyStringNoPrefix(String from);

	/**
	 * @return full String representation
	 */
	public String toString(String from);
}

