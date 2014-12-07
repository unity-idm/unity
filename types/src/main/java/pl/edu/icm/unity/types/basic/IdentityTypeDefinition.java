/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.MessageSource;
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
	 * @return if true then the identity type is dynamic, and can not be created manually.
	 * Dynamic identities are created automatically.
	 */
	public boolean isDynamic();
	
	/**
	 * @return true is returned only for dynamic identities, which can not be removed manually. This happens for 
	 * volatile identities, for instance session-scoped. Those identities can be only reset, i.e. all instances
	 * of its type can be removed.
	 */
	public boolean isRemovable();
	
	/**
	 * @return if true then identities of this type are targeted, i.e. can have a different value 
	 * for each and every receiver (target). This implies that the authentication realm and target are mandatory
	 * parameters for the methods as e.g. the {@link #getComparableValue(String, String, String)}.
	 */
	public boolean isTargeted();
	
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
	 * @param from mandatory raw identity value
	 * @param realm realm value, can be null
	 * @param target target for which the identity is going to be used, can be null
	 * @return comparable value of the string
	 * @throws IllegalIdentityValueException if some parameters are null and the implementation 
	 * requires them to create a comparable value.
	 */
	public String getComparableValue(String from, String realm, String target) throws IllegalIdentityValueException;
	
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
	 * @param msg
	 * @param from
	 * @return string representation which is most useful for end-user. Note that this representation may
	 * even hide the actual value if it is considered cryptic.
	 */
	public String toHumanFriendlyString(MessageSource msg, String from);

	/**
	 * @param msg
	 * @return Description of the type which can be presented to end user.
	 */
	public String getHumanFriendlyDescription(MessageSource msg);
	
	/**
	 * Converts the in-DB representation to external form. The implementation may perform arbitrary modifications
	 * of the inDbValue.
	 * @param realm authentication realm identifier or null if no realm is defined
	 * @param target null or an identifier of a receiver of the identity.
	 * @param inDbValue the in-db representation
	 * @return 
	 * @throws IllegalIdentityValueException 
	 */
	public String toExternalForm(String realm, String target, String inDbValue, String comparableValue) 
			throws IllegalIdentityValueException;

	/**
	 * Converts the in-DB representation to the external form. The implementation must not use any 
	 * context information, ignore target or realm.
	 * @param inDbValue the in-db representation
	 * @return identity value in external form
	 */
	public String toExternalFormNoContext(String inDbValue, String comparableValue);
	
	/**
	 * Tries to create a new identity. 
	 * @param realm authentication realm identifier or null if no realm is defined
	 * @param target null or the receiver of the created identity
	 * @param value externally provided value or null if the implementation is expected to create the value dynamically.
	 * @return new representation of identity to be stored in database
	 * @throws IllegalTypeException if the creation failed
	 */
	public IdentityRepresentation createNewIdentity(String realm, String target, String value) 
			throws IllegalTypeException;
	
	/**
	 * Checks if the identity is expired. 
	 * @param representation in db representation of the identity
	 * @return true if expired, false otherwise 
	 */
	public boolean isExpired(IdentityRepresentation representation); 
}

