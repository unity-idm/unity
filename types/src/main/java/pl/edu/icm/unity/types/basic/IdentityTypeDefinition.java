/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;

/**
 * Implementation defined identity type. 
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
	 * @return if true then the identity type is dynamic, and can not be neither created or removed manually.
	 * Dynamic identities are created automatically.
	 */
	public boolean isDynamic();
	
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
	
	/**
	 * Converts the in-DB representation to external form. The implementation may perform arbitrary modifications:
	 * from none to produce a totally new version.
	 * @param realm authentication realm identifier or null if no realm is defined
	 * @param target null or an identifier of a receiver of the identity.
	 * @param inDbValue the in-db representation
	 * @return null if no identity should be returned or an external version of identity.
	 */
	public String toExternalForm(String realm, String target, String inDbValue);
	
	/**
	 * Tries to create a new identity. 
	 * @param realm authentication realm identifier or null if no realm is defined
	 * @param target null or the receiver of the created identity
	 * @param inDbValue the current data of the identity.
	 * @return null if the inDbValue was unchanged or an updated version 
	 * of the inDbValue that should be written to the DB. 
	 * @throws IllegalTypeException if the creation failed
	 */
	public String createNewIdentity(String realm, String target, String inDbValue) 
			throws IllegalTypeException;
	
	/**
	 * Tries to reset a dynamic identity. Optionally the reset may be done only for a specified realm and target. 
	 * @param realm authentication realm identifier or null if no realm filtering shall be done
	 * @param target the receiver of the identity to be reset or null if no target filtering should be done
	 * @param inDbValue the current data of the identity.
	 * @return an updated version of the inDbValue that should be written to the DB. 
	 * @throws IllegalTypeException if the reset failed
	 */
	public String resetIdentity(String realm, String target, String inDbValue) 
			throws IllegalTypeException;
	
}

