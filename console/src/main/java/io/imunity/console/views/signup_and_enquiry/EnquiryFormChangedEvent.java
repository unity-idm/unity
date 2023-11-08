/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import pl.edu.icm.unity.base.registration.EnquiryForm;

public class EnquiryFormChangedEvent extends BaseFormChangedEvent<EnquiryForm>
{
	public EnquiryFormChangedEvent(String name)
	{
		super(name);
	}

	public EnquiryFormChangedEvent(EnquiryForm form)
	{
		super(form);
	}
}
