/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldaputils;

import java.util.List;

import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Implementations allows to convert {@link LDAPAttributeType} to {@link AttributeType}s.
 * @author K. Benedyczak
 */
public interface LDAPAttributeTypeConverter
{
	/**
	 * @param at
	 * @return true only if the parameter can be handled by the converter
	 */
	public boolean supports(LDAPAttributeType at);
	
	/**
	 * @param at
	 * @return list of attribute types which can be created from the parameter. The list may contain multiple elements 
	 * to cover aliases which may be present in the parameter.
	 */
	public List<AttributeType> convertSingle(LDAPAttributeType at);
}
