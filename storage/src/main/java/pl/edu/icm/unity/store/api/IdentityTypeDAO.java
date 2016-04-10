/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.Map;

import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Identity type DAO
 * @author K. Benedyczak
 */
public interface IdentityTypeDAO
{
	Map<String, IdentityType> getIdentityTypes();

	void updateIdentityType(IdentityType idType);

	void createIdentityType(IdentityType idType);

	void deleteIdentityType(String idType);
}
