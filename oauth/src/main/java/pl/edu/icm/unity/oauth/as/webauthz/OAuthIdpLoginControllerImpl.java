/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.IdPLoginController.IdPLoginHandler;

@Component
public class OAuthIdpLoginControllerImpl implements IdPLoginHandler
{
	@Override
	public boolean isLoginInProgress()
	{
		return OAuthContextUtils.hasContext();
	}

	@Override
	public void breakLogin()
	{
		OAuthContextUtils.cleanContext();
	}
}