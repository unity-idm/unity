/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import pl.edu.icm.unity.types.registration.RegistrationForm;

public class RegistrationFormChangedEvent extends BaseFormChangedEvent<RegistrationForm>
{
	public RegistrationFormChangedEvent(String name)
	{
		super(name);
	}

	public RegistrationFormChangedEvent(RegistrationForm form)
	{
		super(form);
	}
}
