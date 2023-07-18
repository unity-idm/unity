/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.session.SessionParticipantType;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.session.SessionParticipants;

public class TestSessionParticipants
{
	@Test
	public void testSerialization()
	{
		final SessionParticipant sp =  new SessionParticipant()
		{
			@Override
			public String getProtocolType()
			{
				return "tp";
			}
			@Override
			public String getIdentifier()
			{
				return "1";
			}
		}; 
		
		List<SessionParticipantType> impls = new ArrayList<SessionParticipantType>();
		impls.add(new SessionParticipantType()
		{
			@Override
			public String serialize(SessionParticipant participant) throws IOException
			{
				return "aaa";
			}
			
			@Override
			public String getProtocolType()
			{
				return "tp";
			}
			
			@Override
			public SessionParticipant getInstance(String entryValue) throws IOException
			{
				if (!"aaa".equals(entryValue))
					throw new IOException("wrong serialized state");
				return sp;
			}
		});
		SessionParticipantTypesRegistry reg = new SessionParticipantTypesRegistry(impls);
		
		SessionParticipants sps = new SessionParticipants(reg);
		sps.addParticipant(sp);
		String serialized = sps.serialize();
		sps = new SessionParticipants(serialized, reg);
		assertThat(sps.getParticipants()).hasSize(1);
	}
}
