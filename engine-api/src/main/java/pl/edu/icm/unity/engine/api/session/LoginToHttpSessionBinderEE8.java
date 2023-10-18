/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.session;

import javax.servlet.http.HttpSession;

import pl.edu.icm.unity.engine.api.authn.LoginSession;

/**
 * Maintains an association of Unity's {@link LoginSession}s with {@link HttpSession}s. 
 * The main purpose is to invalidate the latter when Unity session is expired.
 * The implementation also takes care about memory consumption: whenever a {@link HttpSession} is expired
 * it is removed from the registry. 
 * <p>
 * Thread safe.
 * @author K. Benedyczak
 */
public interface LoginToHttpSessionBinderEE8
{
	/**
	 * Under this key the {@link LoginSession} id is stored in the HTTP session.
	 */
	String USER_SESSION_KEY = "pl.edu.icm.unity.web.WebSession";
	
	/**
	 * @param toRemove
	 * @param soft if true then only the login data is removed from the HTTP session. Otherwise the whole
	 * session is invalidated 
	 */
	void removeLoginSession(String toRemove, boolean soft);
	
	void bindHttpSession(HttpSession session, LoginSession owning);
}
