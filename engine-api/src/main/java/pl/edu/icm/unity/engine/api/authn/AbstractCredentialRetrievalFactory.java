/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import org.springframework.beans.factory.ObjectFactory;

/**
 * Common boilerplate for {@link CredentialRetrievalFactory} implementations.
 * @author K. Benedyczak
 */
public abstract class AbstractCredentialRetrievalFactory<T extends CredentialRetrieval> 
		implements CredentialRetrievalFactory
{
	private String name;
	private String description;
	private String binding;
	private ObjectFactory<T> factory;
	private String supportedExchange;
	
	public AbstractCredentialRetrievalFactory(String name, String description, String binding,
			ObjectFactory<T> factory, String supportedExchange)
	{
		this.description = description;
		this.name = name;
		this.binding = binding;
		this.factory = factory;
		this.supportedExchange = supportedExchange;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return factory.getObject();
	}

	@Override
	public String getSupportedBinding()
	{
		return binding;
	}

	@Override
	public boolean isCredentialExchangeSupported(String credentialExchangeId)
	{
		return supportedExchange.equals(credentialExchangeId);
	}
}
