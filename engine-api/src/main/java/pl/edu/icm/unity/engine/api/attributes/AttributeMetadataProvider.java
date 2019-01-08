/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.types.DescribedObject;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Implementation defines an {@link AttributeType} metadata. The metadata can be used to give user-controlled semantics 
 * to ordinary attributes, e.g. to mark an attribute as contact email address or define how it should be mapped to 
 * SAML attribute.
 * @author K. Benedyczak
 */
public interface AttributeMetadataProvider extends DescribedObject
{
	/**
	 * Should verify whether the attribute's metadata is correct for the given attribtue type.
	 * @param metadata
	 * @param at
	 * @throws IllegalAttributeTypeException
	 */
	void verify(String metadata, AttributeType at) throws IllegalAttributeTypeException;
	
	/**
	 * @return true if this metadata can be set for a single attribute only.
	 */
	boolean isSingleton();
	
	/**
	 * @return true if the attribute annotated with this attribute is security sensitive
	 */
	boolean isSecuritySensitive();
	
}
