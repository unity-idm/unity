/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import javax.servlet.Filter;
import javax.servlet.Servlet;

/**
 * Provides servlet which gives access to public well known-links handler. 
 *
 * @author K. Benedyczak
 *
 */
public interface PublicWellKnownURLServlet
{
	public static final String SERVLET_PATH = "/pub";
	
	Servlet getServiceServlet();
	Filter getServiceFilter();
}
