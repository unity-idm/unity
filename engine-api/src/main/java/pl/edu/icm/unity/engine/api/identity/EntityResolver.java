/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.identity;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityTaV;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;

/**
 * Various helper operations allowing for converting and checking identity and entity related parameters.
 * @author K. Benedyczak
 */
public interface EntityResolver
{
	/**
	 * Resolves {@link IdentityTaV} into a full Identity, if missing throws exception
	 */
	Identity getFullIdentity(IdentityTaV entity);
	
	/**
	 * Resolves {@link IdentityTaV}, if missing throws exception
	 */
	long getEntityId(IdentityTaV entity) throws IllegalIdentityValueException;

	/**
	 * Resolves {@link EntityParam}, if missing throws exception
	 */
	long getEntityId(EntityParam entity) throws IllegalIdentityValueException;
}