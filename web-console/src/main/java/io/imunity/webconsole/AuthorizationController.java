/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
class AuthorizationController
{
	private AuthorizationManagement authzMan;
	private UnityMessageSource msg;

	public AuthorizationController(UnityMessageSource msg, AuthorizationManagement authzMan)
	{
		this.msg = msg;
		this.authzMan = authzMan;
	}

	public void hasAdminAccess() throws ControllerException
	{
		try
		{
			if (!authzMan.hasAdminAccess())
			{
				throw new ControllerException(msg.getMessage("AuthorizationController.notAdminUser"),
						null);
			}
		} catch (AuthorizationException e)
		{
			throw new ControllerException(msg.getMessage("AuthorizationController.notAuthenticatedUser"),
					null);
		}
	}
}
