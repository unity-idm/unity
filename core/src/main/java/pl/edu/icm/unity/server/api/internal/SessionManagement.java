/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.Date;
import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Internal login sessions management
 * @author K. Benedyczak
 */
public interface SessionManagement
{
	/**
	 * Tries to find a session for the entity in the given realm. If the session is not found then a new session
	 * is established.
	 * @param loggedEntity
	 * @param realm
	 * @param label used only when a new session is created
	 * @param outdatedCredential used only if a new session is created
	 * @param absoluteExpiration can be null or absolute expiration time (then session is not closed after 
	 * inactive timeout).
	 * @return
	 */
	public LoginSession getCreateSession(long loggedEntity, AuthenticationRealm realm, 
			String label, boolean outdatedCredential, Date absoluteExpiration);
	
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
	 * @param soft  if true then only the login data is removed from the HTTP session. Otherwise the whole
	 * session is invalidated
	 */
	void removeSession(String id, boolean soft);
	
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
	 * Adds given participands to the current login session
	 * @param participant
	 */
	void addSessionParticipant(SessionParticipant... participant);
	
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
