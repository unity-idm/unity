/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;


/**
 * Role of a user. User can have several roles. Each role provides a set of capabilities. Capabilities
 * are required to perform operations.
 * 
 * @author K. Benedyczak
 */
public interface AuthzRole
{
	public String getName();
	public String getDescription();
	public AuthzCapability[] getCapabilities(boolean withSelf);
}
