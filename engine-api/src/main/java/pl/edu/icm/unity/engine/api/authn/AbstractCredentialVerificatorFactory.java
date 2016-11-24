/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import org.springframework.beans.factory.ObjectFactory;

/**
 * Helper to easily create credential factories.
 * @author K. Benedyczak
 */
public abstract class AbstractCredentialVerificatorFactory implements CredentialVerificatorFactory
{
	private final String name;
	private final String desc;
	protected final ObjectFactory<? extends CredentialVerificator> factory;

	public AbstractCredentialVerificatorFactory(String name, String desc,
			ObjectFactory<? extends CredentialVerificator> factory)
	{
		this.name = name;
		this.desc = desc;
		this.factory = factory;
	}

	@Override
	public String getDescription()
	{
		return desc;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public CredentialVerificator newInstance()
	{
		return factory.getObject();
	}
}
