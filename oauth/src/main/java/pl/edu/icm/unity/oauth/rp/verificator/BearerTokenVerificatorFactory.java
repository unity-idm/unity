/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;

/**
 * Factory of {@link BearerTokenVerificator}.
 * 
 * @author K. Benedyczak
 */
@Component
public class BearerTokenVerificatorFactory implements CredentialVerificatorFactory
{
	public static final String NAME = "oauth-rp";
	
	private TranslationProfileManagement profileManagement;
	private InputTranslationEngine trEngine;
	private PKIManagement pkiManagement;
	private TokensManagement tokensMan;
	
	@Autowired
	public BearerTokenVerificatorFactory(@Qualifier("insecure") TranslationProfileManagement profileManagement, 
			InputTranslationEngine trEngine, PKIManagement pkiManagement, TokensManagement tokensMan)
	{
		this.profileManagement = profileManagement;
		this.trEngine = trEngine;
		this.pkiManagement = pkiManagement;
		this.tokensMan = tokensMan;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Verifies OAuth access tokens against an OAuth Authorization Server";
	}

	@Override
	public CredentialVerificator newInstance()
	{
		return new BearerTokenVerificator(getName(), getDescription(), profileManagement, pkiManagement,
				trEngine, tokensMan);
	}
}
