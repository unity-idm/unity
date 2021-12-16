/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.List;
import java.util.Optional;

import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.types.authn.IdPInfo;

/**
 * Abstract {@link CredentialVerificator} with a common boilerplate code.
 * @author K. Benedyczak
 */
public abstract class AbstractVerificator implements CredentialVerificator
{
	private String name;
	protected String instanceName;
	private String description;
	private String exchangeId;
	protected IdentityResolver identityResolver;
	
	public AbstractVerificator(String name, String description, String exchangeId)
	{
		this.name = name;
		this.description = description;
		this.exchangeId = exchangeId;
	}

	@Override
	public String getExchangeId()
	{
		return exchangeId;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public void setIdentityResolver(IdentityResolver identityResolver)
	{
		this.identityResolver = identityResolver;
	}
	
	@Override
	public void setInstanceName(String instanceName)
	{
		this.instanceName = instanceName;
	}
	
	@Override
	public Optional<List<IdPInfo>> getIdPs()
	{
		return Optional.empty();
	}
}
