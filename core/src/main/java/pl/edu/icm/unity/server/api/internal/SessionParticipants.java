/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.internal.SessionManagement.AttributeUpdater;
import pl.edu.icm.unity.server.utils.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Holds multiple {@link SessionParticipant}s. Is stored as a {@link LoginSession} attribute. 
 * @author K. Benedyczak
 */
public class SessionParticipants
{
	public static final String KEY = "sessionParticipants";
	
	private Set<SessionParticipant> participants = new HashSet<>();
	private ObjectMapper jsonMapper = new ObjectMapper();
	
	public SessionParticipants(String serializedState)
	{
		try
		{
			participants = jsonMapper.readValue(serializedState, 
					new TypeReference<Set<SessionParticipant>>(){});
		} catch (Exception e)
		{
			throw new InternalException("Can not deserialize session participants from JSON", e);
		}
	}
	
	public SessionParticipants()
	{
	}
	
	public static SessionParticipants getFromSession(Map<String, String> sessionAttributes)
	{
		String participantsSerialized = sessionAttributes.get(KEY);
		return participantsSerialized == null ? 
				new SessionParticipants() : new SessionParticipants(participantsSerialized);
	}
	
	public String serialize()
	{
		try
		{
			return jsonMapper.writeValueAsString(participants);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can not serialize session participants to JSON", e);
		}
	}
	
	public void addParticipant(SessionParticipant participant)
	{
		participants.add(participant);
	}
	
	public void removeParticipant(SessionParticipant participant)
	{
		participants.remove(participant);
	}
	
	public Set<SessionParticipant> getParticipants()
	{
		Set<SessionParticipant> ret = new HashSet<>(participants);
		return ret;
	}

	@Override
	public String toString()
	{
		return participants.toString();
	}
	
	/**
	 * Session attribute updater adding a new SessionParticipant to the session.
	 * @author K. Benedyczak
	 */
	public static class AddParticipantToSessionTask implements AttributeUpdater
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER,
				SessionParticipants.AddParticipantToSessionTask.class);
		
		private SessionParticipant[] toBeAdded;
		
		public AddParticipantToSessionTask(SessionParticipant... toBeAdded)
		{
			this.toBeAdded = toBeAdded;
		}

		@Override
		public void updateAttributes(Map<String, String> sessionAttributes)
		{
			SessionParticipants participants = getFromSession(sessionAttributes);
			for (SessionParticipant p: toBeAdded)
				participants.addParticipant(p);
			if (log.isDebugEnabled())
				log.debug("Updated session participants to: " + participants);
			sessionAttributes.put(KEY, participants.serialize());
		}
	}
}
