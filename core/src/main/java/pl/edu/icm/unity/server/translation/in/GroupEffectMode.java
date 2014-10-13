/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;

import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Defines how a mapped {@link IdentityParam} should be handled. 
 * @author K. Benedyczak
 */
public enum GroupEffectMode
{
	/**
	 * If the target group is missing translation will fail.
	 */
	REQUIRE_EXISTING_GROUP,
	
	/**
	 * Identity will be added to the target group only if the group exists.
	 */
	ADD_IF_GROUP_EXISTS,
	
	/**
	 * If a target group is missing then it will be created first, then the identity is added.
	 */
	CREATE_GROUP_IF_MISSING
}
