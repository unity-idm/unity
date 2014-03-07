/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.EntityParam;

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
	 * Updates the extra attributes of the session. Update is done via callback to enable transactional access.
	 * @param id
	 * @throws WrongArgumentException 
	 */
	void updateSessionAttributes(String id, AttributeUpdater updater) 
			throws WrongArgumentException;

	/**
	 * Updates the lastUsed timestamp of a session. The implementation may delay this action if the 
	 * previous update happened recently.
	 * @param id
	 * @throws WrongArgumentException 
	 */
	void updateSessionActivity(String id) throws WrongArgumentException;

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
	
	/**
	 * Tries to find a session owned by a given entity in a given realm.
	 * @param owner
	 * @param realm
	 * @return
	 * @throws WrongArgumentException
	 * @throws EngineException 
	 */
	LoginSession getOwnedSession(EntityParam owner, String realm) throws WrongArgumentException, EngineException;
	
	/**
	 * Callback interface. Implementation can update the attributes. It should return quickly as 
	 * it is invoked inside of a DB transaction.
	 * @author K. Benedyczak
	 */
	interface AttributeUpdater
	{
		public void updateAttributes(Map<String, String> sessionAttributes);
	}
}
