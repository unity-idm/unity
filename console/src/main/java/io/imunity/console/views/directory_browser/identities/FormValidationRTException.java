/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import org.apache.logging.log4j.util.Strings;

public class FormValidationRTException extends RuntimeException
{
	public FormValidationRTException()
	{
		super();
	}

	public FormValidationRTException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public FormValidationRTException(String message)
	{
		super(message);
	}

	public FormValidationRTException(Throwable cause)
	{
		super(cause);
	}
	
	public boolean hasMessage()
	{
		return !Strings.isEmpty(getMessage());
	}
}
