/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.List;

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
	 * 
	 * @return list of ids of attribute types that can be extracted from the identity of this type.
	 */
	public List<String> getAttributesSupportedForExtraction();
	
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
	 * Extract provided attributes or all if null is given as argument
	 * @return
	 */
	public List<Attribute<?>> extractAttributes(String from, List<String> toExtract);
	
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

