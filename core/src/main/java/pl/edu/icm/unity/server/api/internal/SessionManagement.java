/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.Date;

import pl.edu.icm.unity.exceptions.WrongArgumentException;

/**
 * Internal login sessions management
 * @author K. Benedyczak
 */
public interface SessionManagement
{
	/**
	 * Creates a new session. The session id is established automatically and it is ignored even if 
	 * set in the parameter.
	 * @param session
	 * @return the new session's id
	 */
	String createSession(LoginSession session);
	
	/**
	 * Updates the expiration time of the session.
	 * @param id
	 * @param newExpires
	 * @throws WrongArgumentException 
	 */
	void updateSessionExpirtaion(String id, Date newExpires) throws WrongArgumentException;
	
	/**
	 * Updates the extra attributes of the session. Set the value to null to remove the attribute.
	 * @param id
	 * @param attributeKey
	 * @param attributeValue
	 * @throws WrongArgumentException 
	 */
	void updateSessionAttributes(String id, String attributeKey, String attributeValue) 
			throws WrongArgumentException;

	/**
	 * Removes a given session. Missing session is silently ignored.
	 * @param id
	 */
	void removeSession(String id);
	
	/**
	 * @param id
	 * @return session
	 * @throws WrongArgumentException 
	 */
	LoginSession getSession(String id) throws WrongArgumentException;
}
