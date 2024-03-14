/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.exceptions;

import org.apache.logging.log4j.util.Strings;

public class FormValidationException extends Exception
{
	public FormValidationException()
	{
		super();
	}

	public FormValidationException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public FormValidationException(String message)
	{
		super(message);
	}

	public FormValidationException(Throwable cause)
	{
		super(cause);
	}
	
	public boolean hasMessage()
	{
		return !Strings.isEmpty(getMessage());
	}
}
