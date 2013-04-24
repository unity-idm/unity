/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.data.Validatable;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;

/**
 * Simple form validator. Instead of employing the whole Vaadin infrastructure, this one
 * simply iterates over all components of a container recursively. If error is found
 * then error state is set and finally an exception is thrown.
 * 
 * Disabled components are ignored.
 * 
 * @author K. Benedyczak
 */
public class FormValidator
{
	private HasComponents container;
	
	public FormValidator(HasComponents container)
	{
		this.container = container;
	}
	
	public void validate() throws FormValidationException
	{
		if (validate(container))
			throw new FormValidationException();
	}

	private boolean validate(HasComponents container)
	{
		boolean invalid = false;
		for (Component c: container)
		{
			if (c instanceof HasComponents)
				invalid |= validate((HasComponents)c);
			if (c instanceof Validatable && c instanceof AbstractComponent)
				invalid |= validateComponent((Validatable)c);
		}
		return invalid;
	}
	
	private boolean validateComponent(Validatable c)
	{
		if (!((AbstractComponent)c).isEnabled())
			return false;
		try
		{
			c.validate();
			return false;
		} catch (InvalidValueException e)
		{
			return true;
		}
	}
}
