/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.server.api.internal.SessionParticipant;
import pl.edu.icm.unity.server.api.internal.SessionParticipantType;
import pl.edu.icm.unity.server.api.internal.SessionParticipants;
import pl.edu.icm.unity.server.registries.SessionParticipantTypesRegistry;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

import com.google.gwt.dev.util.collect.HashSet;


public class TestSAMLSessionParticipants
{
	@Test
	public void testEquality()
	{
		NameIDType subjectAtParticipant = NameIDType.Factory.newInstance();
		subjectAtParticipant.setStringValue("name");
		final SessionParticipant sp =  new SAMLSessionParticipant("id", subjectAtParticipant,
				"idx", new ArrayList<>(), "saml-id", "cred", new HashSet<>()); 
		
		List<SessionParticipantType> impls = new ArrayList<SessionParticipantType>();
		impls.add(new SAMLSessionParticipantFactory());
		SessionParticipantTypesRegistry reg = new SessionParticipantTypesRegistry(impls);
		
		SessionParticipants sps = new SessionParticipants(reg);
		sps.addParticipant(sp);
		sps.addParticipant(sp);
		String serialized = sps.serialize();
		sps = new SessionParticipants(serialized, reg);
		assertThat(sps.getParticipants().size(), is(1));
		assertThat(sps.getParticipants().iterator().next(), is(sp));
	}
}
