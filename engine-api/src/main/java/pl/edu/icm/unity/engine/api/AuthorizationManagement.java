/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api;

import pl.edu.icm.unity.exceptions.AuthorizationException;

/**
 * API for authorization management.
 * @author P.Piernik
 *
 */
public interface AuthorizationManagement
{
	/**
	 * Checks if caller has admin access
	 * @return
	 * @throws AuthorizationException 
	 */
	boolean hasAdminAccess() throws AuthorizationException;
}
