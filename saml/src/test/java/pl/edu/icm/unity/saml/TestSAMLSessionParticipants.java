/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.session.SessionParticipantType;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.session.SessionParticipants;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;



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
		assertThat(sps.getParticipants()).hasSize(1);
		assertThat(sps.getParticipants().iterator().next()).isEqualTo(sp);
	}
}
