/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.bus.Event;

public class RegistrationFormChangedEvent implements Event
{
	private RegistrationForm form;
	private String name;

	public RegistrationFormChangedEvent(String name)
	{
		this.name = name;
	}

	public RegistrationFormChangedEvent(RegistrationForm form)
	{
		this.form = form;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public RegistrationForm getForm()
	{
		return form;
	}

	public void setForm(RegistrationForm form)
	{
		this.form = form;
	}
}
