/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;

/**
 * Base interface defining attribute value syntax. It provides handling of the 
 * configuration (typically used server side to validate values), (de)serialization to String
 * and equality test.
 * <p>
 * Note that validation is only meaningful when the implementation was properly 
 * populated with configuration.
 * 
 * @author K. Benedyczak
 */
public interface AttributeValueSyntax<T>
{
	/**
	 * @return current serialized configuration of the syntax
	 */
	JsonNode getSerializedConfiguration();
	
	/**
	 * Initializes
	 * @param json
	 */
	void setSerializedConfiguration(JsonNode json);
	
	/**
	 * @return attribute value syntax ID
	 */
	String getValueSyntaxId();

	/**
	 * Validates the value
	 * @param value
	 */
	void validate(T value) throws IllegalAttributeValueException;

	/**
	 * Converts value to string and then validates it
	 * @param value
	 */
	default void validateStringValue(String value) throws IllegalAttributeValueException
	{
		validate(convertFromString(value));
	}
	
	/**
	 * @param value
	 * @param another
	 * @return true only if the two values are the same.
	 */
	boolean areEqual(T value, Object another);

	/**
	 * Converts values from string and then performs equality checking
	 * @param value
	 * @param another
	 * @return true only if the two values are the same.
	 */
	default boolean areEqualStringValue(String value, String another)
	{
		return areEqual(convertFromString(value), convertFromString(another));
	}

	/**
	 * @param value, must be of T type, otherwise the standard hash should be returned.
	 * @return java hashcode of the value
	 */
	int hashCode(Object value);
	
	/**
	 * Performs a simplified serialization of the value object to string. Note that some of the information
	 * may be lost during this serialization: it is intended for exporting the value to outside world, so the value 
	 * must be simple. For instance verifiable email will be serialized as an email address string, 
	 * without confirmation information.
	 * 
	 * @param domain object
	 * @return value in the string form, possibly simplified
	 */
	default String serializeSimple(T value)
	{
		return convertToString(value);
	}

	/**
	 * Many attributes are passed in a string form, especially when obtained externally. Whenever it is possible
	 * this method should convert string representation to the domain object. Note that this 
	 * method may not be able to initialize all properties of the domain value object. 
	 * @see {@link #serializeSimple(Object)}  
	 * @param value to deserialize
	 * @return domain object
	 * @throws IllegalAttributeValueException 
	 */
	default T deserializeSimple(String value) throws IllegalAttributeValueException
	{
		try
		{
			T ret = convertFromString(value);
			validate(ret);
			return ret;
		} catch (Exception e)
		{
			throw new IllegalAttributeValueException(value + " can not be deserialized to " +
					getValueSyntaxId(), e);
		}
	}
	
	/**
	 * Converts the value from string representation as produced by {@link #convertToString(Object)}
	 * @param stringRepresentation
	 * @return
	 * @throws IllegalAttributeValueException if the conversion can not be performed.
	 */
	T convertFromString(String stringRepresentation);
	
	/**
	 * Dumps a domain value to string. This method (conversly to {@link #serializeSimple(Object)}) must 
	 * output a complete value.
	 * @param value
	 * @return value converted to string.
	 */
	String convertToString(T value);
	
	
	/**
	 * @return true if values can be confirmed by user using out-of bounds verification (via email)
	 * Note that if this method returns true then {@link #isUserVerifiable()} must also return true
	 */
	boolean isEmailVerifiable();

	/**
	 * @return true if values are implementing VerifiableElement and can be in confirmed or not state
	 */
	boolean isUserVerifiable();

	
	/**
	 * If syntax is verifiable by email return confirmation configuration
	 * @return
	 */
	default Optional<EmailConfirmationConfiguration> getEmailConfirmationConfiguration()
	{
		return Optional.empty();
	}
	
}
