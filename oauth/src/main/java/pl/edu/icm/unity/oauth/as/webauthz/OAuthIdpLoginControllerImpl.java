/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController.IdPLoginHandler;

@Component
public class OAuthIdpLoginControllerImpl implements IdPLoginHandler
{
	private final OAuthSessionService oauthSessionService;
	
	@Autowired
	OAuthIdpLoginControllerImpl(OAuthSessionService oauthSessionService)
	{
		this.oauthSessionService = oauthSessionService;
	}

	@Override
	public boolean isLoginInProgress()
	{
		return OAuthSessionService.hasVaadinContext();
	}

	@Override
	public void breakLogin()
	{
		oauthSessionService.cleanupComplete(LoginInProgressService.VaadinContextSession.getCurrent(), false);
	}
}