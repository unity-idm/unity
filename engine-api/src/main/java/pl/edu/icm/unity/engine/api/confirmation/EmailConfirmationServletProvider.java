/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.confirmation;

import javax.servlet.Filter;

import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Provides servlet used as a email confirmation endpoint
 *
 * @author P. Piernik
 *
 */
public interface EmailConfirmationServletProvider
{
	public static final String SERVLET_PATH = "/confirmation";
	public static final String CONFIRMATION_TOKEN_ARG ="token";
	
	ServletHolder getServiceServlet();
	Filter getServiceFilter();
}
