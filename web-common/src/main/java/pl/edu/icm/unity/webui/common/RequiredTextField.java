/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.TextField;


/**
 * Extension of {@link TextField} with a one change: the field is required 
 * and the exclamation mark is shown for all empty fields except for the initial rendering. 
 * @author K. Benedyczak
 */
public class RequiredTextField extends TextField
{
	public RequiredTextField(UnityMessageSource msg)
	{
		super();
		setRequired(true);
		setRequiredError(msg.getMessage("fieldRequired"));
	}

	public RequiredTextField(String caption, String value, UnityMessageSource msg)
	{
		super(caption, value);
		setRequired(true);
		setRequiredError(msg.getMessage("fieldRequired"));
	}

	public RequiredTextField(String caption, UnityMessageSource msg)
	{
		super(caption);
		setRequired(true);
		setRequiredError(msg.getMessage("fieldRequired"));
	}
	
	protected boolean shouldHideErrors() {
		return false;
	}
}
