/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import java.io.IOException;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.server.api.internal.SessionParticipant;
import pl.edu.icm.unity.server.api.internal.SessionParticipantType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link SAMLSessionParticipant} type 
 * @author K. Benedyczak
 */
@Component
public class SAMLSessionParticipantFactory implements SessionParticipantType
{
	public static final String TYPE = "SAML2";
	private ObjectMapper mapper = Constants.MAPPER;
	
	@Override
	public String getProtocolType()
	{
		return TYPE;
	}

	@Override
	public SessionParticipant getInstance(String entryValue) throws IOException
	{
		return mapper.readValue(entryValue, SAMLSessionParticipant.class);
	}

	@Override
	public String serialize(SessionParticipant participant) throws JsonProcessingException
	{
		return mapper.writeValueAsString(participant);
	}
}
