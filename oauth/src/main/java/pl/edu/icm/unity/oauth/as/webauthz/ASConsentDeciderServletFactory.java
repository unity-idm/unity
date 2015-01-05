/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.TokensManagement;

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
	protected FreemarkerHandler freemarker;
	protected TokensManagement tokensMan;

	@Autowired
	public ASConsentDeciderServletFactory(PreferencesManagement preferencesMan,
			IdPEngine idpEngine, FreemarkerHandler freemarker,
			TokensManagement tokensMan)
	{
		this.preferencesMan = preferencesMan;
		this.idpEngine = idpEngine;
		this.freemarker = freemarker;
		this.tokensMan = tokensMan;
	}


	public ASConsentDeciderServlet getInstance(String oauthUiServletPath)
	{
		return new ASConsentDeciderServlet(preferencesMan, idpEngine, freemarker, 
				tokensMan, oauthUiServletPath);
	}
}
