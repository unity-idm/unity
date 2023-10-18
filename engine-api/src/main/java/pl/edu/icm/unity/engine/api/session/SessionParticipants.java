/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.session;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagementEE8.AttributeUpdater;

/**
 * Holds multiple {@link SessionParticipant}s. Is stored as a {@link LoginSession} attribute. 
 * @author K. Benedyczak
 */
public class SessionParticipants
{
	public static final String KEY = "sessionParticipants";
	
	private Set<SessionParticipant> participants = new HashSet<>();
	private ObjectMapper jsonMapper = new ObjectMapper();
	private SessionParticipantTypesRegistry registry;
	
	public SessionParticipants(String serializedState, SessionParticipantTypesRegistry registry)
	{
		this.registry = registry;
		try
		{
			ArrayNode root = (ArrayNode) jsonMapper.readTree(serializedState);
			for (int i=0; i<root.size(); i++)
			{
				ObjectNode entry = (ObjectNode) root.get(i);
				String type = entry.get("type").asText();
				String entryValue = entry.get("value").asText();
				SessionParticipantType factory = registry.getByName(type);
				participants.add(factory.getInstance(entryValue));
			}
		} catch (Exception e)
		{
			throw new InternalException("Can not deserialize session participants from JSON", e);
		}
	}
	
	public SessionParticipants(SessionParticipantTypesRegistry registry)
	{
		this.registry = registry;
	}
	
	public static SessionParticipants getFromSession(Map<String, String> sessionAttributes, 
			SessionParticipantTypesRegistry registry)
	{
		String participantsSerialized = sessionAttributes.get(KEY);
		return participantsSerialized == null ? new SessionParticipants(registry) 
				: new SessionParticipants(participantsSerialized, registry);
	}
	
	public String serialize()
	{
		try
		{
			ArrayNode root = jsonMapper.createArrayNode();
			for (SessionParticipant sp: participants)
			{
				ObjectNode entry = root.addObject();
				entry.put("type", sp.getProtocolType());
				SessionParticipantType factory = registry.getByName(sp.getProtocolType());
				entry.put("value", factory.serialize(sp));
			}
			return jsonMapper.writeValueAsString(root);
		} catch (IOException e)
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
		private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN,
				SessionParticipants.AddParticipantToSessionTask.class);
		
		private SessionParticipant[] toBeAdded;
		private SessionParticipantTypesRegistry registry;
		
		public AddParticipantToSessionTask(SessionParticipantTypesRegistry registry, 
				SessionParticipant... toBeAdded)
		{
			this.toBeAdded = toBeAdded;
			this.registry = registry;
		}

		@Override
		public void updateAttributes(Map<String, String> sessionAttributes)
		{
			SessionParticipants participants = getFromSession(sessionAttributes, registry);
			for (SessionParticipant p: toBeAdded)
				participants.addParticipant(p);
			if (log.isDebugEnabled())
				log.info("Updated session participants to: " + participants);
			sessionAttributes.put(KEY, participants.serialize());
		}
	}
}
