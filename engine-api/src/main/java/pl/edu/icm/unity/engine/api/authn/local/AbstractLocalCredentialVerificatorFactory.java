/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.local;

import org.springframework.beans.factory.ObjectFactory;

import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;

/**
 * Helper to easily create credential factories.
 * @author K. Benedyczak
 */
public class AbstractLocalCredentialVerificatorFactory extends AbstractCredentialVerificatorFactory 
		implements LocalCredentialVerificatorFactory
{
	private final boolean supportInvalidation;
	private ObjectFactory<? extends LocalCredentialVerificator> factory2;

	public AbstractLocalCredentialVerificatorFactory(String name, String desc, boolean supportInvalidation,
			ObjectFactory<? extends LocalCredentialVerificator> factory)
	{
		super(name, desc, factory);
		this.supportInvalidation = supportInvalidation;
		factory2 = factory;
	}

	@Override
	public LocalCredentialVerificator newInstance()
	{
		return factory2.getObject();
	}

	@Override
	public boolean isSupportingInvalidation()
	{
		return supportInvalidation;
	}
}
