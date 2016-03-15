/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.webui.forms.BaseFormChangedEvent;

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
