/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.io.IOException;

/**
 * Defines an implementation of {@link SessionParticipant}, with factory method.
 * @author K. Benedyczak
 */
public interface SessionParticipantType
{
	public String getProtocolType();
	
	public SessionParticipant getInstance(String entryValue) throws IOException;
	public String serialize(SessionParticipant participant) throws IOException;
}
