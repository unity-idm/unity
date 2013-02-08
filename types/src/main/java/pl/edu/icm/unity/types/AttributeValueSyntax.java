/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

/**
 * Base interface defining attribute value type. It provides handling of the 
 * configuration (typically used server side to validate values), (de)serialization
 * which can be used on both sides and equality test.
 * <p>
 * Implementations can offer arbitrary constructors, however it is preferable to
 * offer also a special extension of {@link Attribute} class, which uses the 
 * provided implementation of this interface so {@link Attribute} class instances
 * can be easily created without an instance of {@link AttributeValueSyntax}.
 * 
 * Note that validation is only meaningful when the implementation was properly 
 * populated with configuration. So if an instance is created ad-hoc on the client side, 
 * then validation probably won't work as on the server side.
 * 
 * @author K. Benedyczak
 */
public interface AttributeValueSyntax<T> extends JsonSerializable
{
	/**
	 * @return attribute value syntax ID
	 */
	public String getValueSyntaxId();
	
	/**
	 * Validates the value
	 * @param value
	 */
	public void validate(T value) throws IllegalAttributeValueException;
	
	/**
	 * @param value
	 * @param another
	 * @return true only if the two values are the same.
	 */
	public boolean areEqual(T value, T another);
	
	/**
	 * @param domain object
	 * @return value in the byte array form
	 */
	public byte[] serialize(T value);
	
	/**
	 * @param raw data
	 * @return domain object
	 */
	public T deserialize(byte []raw);
}
