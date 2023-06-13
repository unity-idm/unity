/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration;


/**
 * Builder of {@link RegistrationRequest}
 */
public class RegistrationRequestBuilder extends BaseRegistrationInputBuilder<RegistrationRequest, RegistrationRequestBuilder>
{
	public RegistrationRequestBuilder()
	{
		super(new RegistrationRequest());
	}
}
