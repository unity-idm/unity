/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.builders;

import pl.edu.icm.unity.types.registration.EnquiryResponse;


/**
 * Builder of {@link EnquiryResponse}
 */
public class EnquiryResponseBuilder extends BaseRegistrationInputBuilder<EnquiryResponse, EnquiryResponseBuilder>
{
	public EnquiryResponseBuilder()
	{
		super(new EnquiryResponse());
	}
}
