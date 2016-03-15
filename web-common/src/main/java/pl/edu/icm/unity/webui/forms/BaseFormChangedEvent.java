/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.webui.bus.Event;

public class BaseFormChangedEvent<T extends BaseForm> implements Event
{
	private T form;
	private String name;

	public BaseFormChangedEvent(String name)
	{
		this.name = name;
	}

	public BaseFormChangedEvent(T form)
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

	public T getForm()
	{
		return form;
	}

	public void setForm(T form)
	{
		this.form = form;
	}
}
