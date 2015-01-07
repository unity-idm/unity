/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import javax.servlet.Servlet;

/**
 * Provides servlet using as endpoint for confirmation
 *
 * @author P. Piernik
 *
 */
public interface ConfirmationServlet
{
	public static final String SERVLET_PATH = "/confirmation";
	public static final String CONFIRMATION_TOKEN_ARG ="token";
	
	public Servlet getServiceServlet();
}
