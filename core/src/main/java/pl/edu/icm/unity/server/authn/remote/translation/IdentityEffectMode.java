/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Defines how a mapped {@link IdentityParam} should be handled. 
 * @author K. Benedyczak
 */
public enum IdentityEffectMode
{
	/**
	 * Identity will be created if doesn't exists. 
	 * If exists the remote principal will be mapped to the existing entity.
	 */
	CREATE_OR_MATCH,
	
	/**
	 * If an identity is found the remote principal will be mapped to this identity.
	 */
	MATCH,
	
	/**
	 * The remote principal must exist in the local database, and will be mapped to it.
	 */
	REQUIRE_MATCH
}
