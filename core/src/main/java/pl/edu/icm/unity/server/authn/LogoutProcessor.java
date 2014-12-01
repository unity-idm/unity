/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.server.api.internal.LoginSession;

/**
 * Performs a logout, including logout of additional session participants, in case of logout initiated directly
 * in Unity.
 * 
 * @author K. Benedyczak
 */
public interface LogoutProcessor
{

	/**
	 * Performs async logout of SAML peers attached to the current login session. It is assumed that the logout 
	 * itself was not initiated with SAML. After the full logout the browser is redirected to a given return URL
	 * with relayState given as parameter. The login session itself is not terminated here.
	 * @param relayState
	 * @param response
	 * @param returnUrl
	 * @throws IOException
	 */
	void handleAsyncLogout(LoginSession session, String relayState, String returnUrl,
			HttpServletResponse response) throws IOException;

	/**
	 * Performs sync logout of SAML peers attached to the current login session. It is assumed that the logout 
	 * itself was not initiated with SAML. The login session itself is not terminated here.
	 * @return if all participants were logged out
	 */
	boolean handleSynchronousLogout(LoginSession session);

}