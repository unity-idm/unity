/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.identity;

/**
 * Internally useful support API for handling identity types.
 * @author K. Benedyczak
 */
public interface IdentityTypeSupport
{

	/**
	 * @param typeDef
	 * @return {@link IdentityTypeDefinition} for the given IdentityType.
	 */
	IdentityTypeDefinition getTypeDefinition(String idType);

}