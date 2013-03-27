/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

/**
 * Holds information stored in the HTTP session.
 * @author K. Benedyczak
 */
public class WebSession
{
	/**
	 * Under this key this object is stored in the HTTP session
	 */
	public static final String SESSION_KEY = WebSession.class.getName();
	/**
	 * Under this key, the object with authenticated user is stored in the session
	 */
	public static final String USER_SESSION_KEY = WebSession.class.getName();
}
