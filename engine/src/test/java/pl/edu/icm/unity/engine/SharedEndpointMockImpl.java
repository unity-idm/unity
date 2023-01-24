/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.exceptions.EngineException;

@Component
class SharedEndpointMockImpl implements SharedEndpointManagement
{
	@Override
	public void deployInternalEndpointServlet(String contextPath, ServletHolder servlet, boolean mapVaadinResource) throws EngineException
	{

	}

	@Override
	public String getBaseContextPath()
	{
		return null;
	}

	@Override
	public String getServletUrl(String servletPath)
	{
		return null;
	}

	@Override
	public void deployInternalEndpointFilter(String contextPath, FilterHolder filter) throws EngineException
	{

	}

	@Override
	public String getServerAddress()
	{
		return null;
	}
}
