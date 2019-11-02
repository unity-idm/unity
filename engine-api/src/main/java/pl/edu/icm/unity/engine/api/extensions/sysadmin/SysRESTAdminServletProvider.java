/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.extensions.sysadmin;

import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * Provides servlet used as a system rest admin endpoint
 * 
 * @author P.Piernik
 *
 */
public interface SysRESTAdminServletProvider
{
	public static final String SERVLET_CONTEXT_PATH = "/_sys_man";

	ServletContextHandler getServletContextHandler(String token);
}
