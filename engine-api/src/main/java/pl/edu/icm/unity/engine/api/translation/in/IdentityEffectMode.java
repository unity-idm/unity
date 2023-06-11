/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.in;

import pl.edu.icm.unity.base.entity.IdentityParam;

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
	CREATE_OR_MATCH(true),
	
	/**
	 * If an identity is found the remote principal will be mapped to this identity.
	 */
	MATCH(false),
	
	/**
	 * The remote principal must exist in the local database, and will be mapped to it.
	 */
	REQUIRE_MATCH(false),
	
	/**
	 * Identity will be created if it doesn't exist and if the user was matched by any other of identity mapping rules.
	 * If identity exists the remote principal will be mapped to the existing entity.
	 */
	UPDATE_OR_MATCH(true);
	
	private boolean mayModify;
	
	
	private IdentityEffectMode(boolean mayModify)
	{
		this.mayModify = mayModify;
	}
	
	public boolean mayModifyIdentity()
	{
		return mayModify;
	}
}
