/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.IdPLoginController.IdPLoginHandler;

@Component
public class SAMLIdPLoginHandlerImpl implements IdPLoginHandler
{
	@Override
	public boolean isLoginInProgress()
	{
		return SAMLContextSupport.hasContext();
	}

	@Override
	public void breakLogin()
	{
		SAMLContextSupport.cleanContext();
	}
}