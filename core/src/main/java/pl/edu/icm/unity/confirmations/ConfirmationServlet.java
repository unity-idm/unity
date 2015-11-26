/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import javax.servlet.Filter;
import javax.servlet.Servlet;

/**
 * Provides servlet used as a confirmation endpoint
 *
 * @author P. Piernik
 *
 */
public interface ConfirmationServlet
{
	public static final String SERVLET_PATH = "/confirmation";
	public static final String CONFIRMATION_TOKEN_ARG ="token";
	
	Servlet getServiceServlet();
	Filter getServiceFilter();
}
