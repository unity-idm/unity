/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.builders;

import pl.edu.icm.unity.types.registration.RegistrationRequest;


/**
 * Builder of {@link RegistrationRequest}
 */
public class RegistrationRequestBuilder extends BaseRegistrationInputBuilder<RegistrationRequest, RegistrationRequestBuilder>
{
	public RegistrationRequestBuilder()
	{
		super(new RegistrationRequest());
	}

	public RegistrationRequestBuilder withRegistrationCode(String aValue)
	{
		instance.setRegistrationCode(aValue);

		return this;
	}
}
