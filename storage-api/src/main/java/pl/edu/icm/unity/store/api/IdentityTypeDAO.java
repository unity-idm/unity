/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import pl.edu.icm.unity.base.identity.IdentityType;

/**
 * Identity type DAO
 * @author K. Benedyczak
 */
public interface IdentityTypeDAO extends NamedCRUDDAO<IdentityType>
{
	String DAO_ID = "IdentityTypeDAO";
}
