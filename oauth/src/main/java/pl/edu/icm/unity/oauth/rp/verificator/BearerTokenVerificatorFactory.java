/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;

/**
 * Factory of {@link BearerTokenVerificator}.
 * 
 * @author K. Benedyczak
 */
@Component
public class BearerTokenVerificatorFactory implements CredentialVerificatorFactory
{
	public static final String NAME = "oauth-rp";
	
	private PKIManagement pkiManagement;
	private TokensManagement tokensMan;
	private CacheProvider cacheProvider;
	private RemoteAuthnResultProcessor processor;
	
	@Autowired
	public BearerTokenVerificatorFactory(PKIManagement pkiManagement, TokensManagement tokensMan,
			CacheProvider cacheProvider, RemoteAuthnResultProcessor processor)
	{
		this.pkiManagement = pkiManagement;
		this.tokensMan = tokensMan;
		this.cacheProvider = cacheProvider;
		this.processor = processor;
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
		return new BearerTokenVerificator(getName(), getDescription(), pkiManagement,
				tokensMan, cacheProvider, processor);
	}
}
