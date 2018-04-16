/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;

/**
 * Creates {@link ASConsentDeciderServlet}s.
 * 
 * @author K. Benedyczak
 */
@Component
public class ASConsentDeciderServletFactory
{
	protected PreferencesManagement preferencesMan;
	protected IdPEngine idpEngine;
	protected TokensManagement tokensMan;
	private SessionManagement sessionMan;

	@Autowired
	public ASConsentDeciderServletFactory(PreferencesManagement preferencesMan,
			IdPEngine idpEngine, 
			TokensManagement tokensMan, SessionManagement sessionMan)
	{
		this.preferencesMan = preferencesMan;
		this.idpEngine = idpEngine;
		this.tokensMan = tokensMan;
		this.sessionMan = sessionMan;
	}


	public ASConsentDeciderServlet getInstance(String oauthUiServletPath)
	{
		return new ASConsentDeciderServlet(preferencesMan, idpEngine,  
				tokensMan, sessionMan, oauthUiServletPath);
	}
}
