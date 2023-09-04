/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;

public class ExceptionMessageHumanizer
{
	public static String getHumanReadableMessage(Throwable e)
	{
		return getHumanReadableMessage(e, "; ");
	}

	public static String getHumanReadableMessage(Throwable e, String separator)
	{
		StringBuilder sb = new StringBuilder();
		if (e instanceof AuthorizationException)
			return e.getMessage();
		String lastMessage = "";
		if (e.getMessage() != null)
		{
			lastMessage = e.getMessage();
			sb.append(lastMessage);
		}
		while (e.getCause() != null)
		{
			e = e.getCause();
			if (e.getMessage() == null)
				break;
			if (e.getMessage().equals(lastMessage))
				continue;
			if (!(e instanceof EngineException || e instanceof ConfigurationException))
				break;
			lastMessage = e.getMessage();
			sb.append(separator).append(lastMessage);
		}
		return sb.toString();
	}
}
