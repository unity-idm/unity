/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.net.MalformedURLException;
import java.net.URL;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.data.validator.AbstractStringValidator;

/**
 * Vaadin text field validator. Checks if the value is a HTTP(S) URL.
 * @author K. Benedyczak
 */
public class URLValidator extends AbstractStringValidator
{
	public URLValidator(UnityMessageSource msg)
	{
		super(msg.getMessage("URLValidator.notAValidURL"));
	}

	@Override
	protected boolean isValidValue(String value)
	{
		if (value == null || value.isEmpty())
			return true;
		try
		{
			new URL(value);
		} catch (MalformedURLException e)
		{
			return false;
		}
		if (!value.startsWith("http://") && !value.startsWith("https://"))
			return false;
		return true;
	}
}
