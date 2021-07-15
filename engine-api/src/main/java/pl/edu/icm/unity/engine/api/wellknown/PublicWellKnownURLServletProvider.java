/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.wellknown;

import pl.edu.icm.unity.engine.api.endpoint.ServletProvider;

/**
 * Provides servlet which gives access to public well known-links handler. 
 *
 * @author K. Benedyczak
 *
 */
public interface PublicWellKnownURLServletProvider extends ServletProvider
{
	public static final String SERVLET_PATH = "/pub";
}
