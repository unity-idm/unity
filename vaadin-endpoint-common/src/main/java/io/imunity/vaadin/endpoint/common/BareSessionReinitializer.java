/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessorEE10.SessionReinitializer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

class BareSessionReinitializer implements SessionReinitializer
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, BareSessionReinitializer.class);

	private final HttpServletRequest httpRequest;

	BareSessionReinitializer(HttpServletRequest httpRequest)
	{
		this.httpRequest = httpRequest;
	}

	@Override
	public HttpSession reinitialize()
	{
		LOG.debug("Bare session reinitialization.");
		bareSessionReinitialization();
		return httpRequest.getSession();
	}

	private void bareSessionReinitialization()
	{
		HttpSession oldSession = httpRequest.getSession(false);
		if (oldSession != null)
		{
			Enumeration<String> attributeNames = oldSession.getAttributeNames();
			Map<String, Object> attrs = new HashMap<>();

			while (attributeNames.hasMoreElements())
			{
				String name = attributeNames.nextElement();
				Object value = oldSession.getAttribute(name);
				attrs.put(name, value);
			}

			oldSession.invalidate();

			HttpSession newSession = httpRequest.getSession(true);

			for (String name : attrs.keySet())
			{
				Object value = attrs.get(name);
				newSession.setAttribute(name, value);
			}
		}
	}
}