/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import pl.edu.icm.unity.server.authn.AuthenticatedEntity;

/**
 * Holds information stored in the HTTP session.
 * @author K. Benedyczak
 */
public class WebSession
{
	/**
	 * Under this key, the object {@link AuthenticatedEntity} 
	 * with authenticated user is stored in the session.
	 */
	public static final String USER_SESSION_KEY = WebSession.class.getName();
}
