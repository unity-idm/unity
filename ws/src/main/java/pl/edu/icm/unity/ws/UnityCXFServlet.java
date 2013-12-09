/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;

/**
 * Custom CXF servlet. Configures {@link UnsuccessfulAuthenticationCounter}.
 * 
 * @author K. Benedyczak
 */
public class UnityCXFServlet extends CXFNonSpringServlet
{
	CXFEndpointProperties config;

	public UnityCXFServlet(CXFEndpointProperties config)
	{
		this.config = config;
	}

	@Override
	public void init(ServletConfig sc) throws ServletException {
		super.init(sc);
		Object counter = getServletContext().getAttribute(UnsuccessfulAuthenticationCounter.class.getName());
		if (counter == null)
		{
			int blockAfter = config.getIntValue(
					CXFEndpointProperties.BLOCK_AFTER_UNSUCCESSFUL);
			int blockFor = config.getIntValue(CXFEndpointProperties.BLOCK_FOR) * 1000;
			getServletContext().setAttribute(UnsuccessfulAuthenticationCounter.class.getName(),
					new UnsuccessfulAuthenticationCounter(blockAfter, blockFor));
		}
	}
}
