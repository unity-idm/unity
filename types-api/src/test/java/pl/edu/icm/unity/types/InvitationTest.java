/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.invite.RESTInvitationWithCode;

/**
 * Unit tests of RESTInvitation* classes
 * @author Krzysztof Benedyczak
 */
public class InvitationTest
{
	@Test
	public void mnimalFormSerializationIsIdempotent()
	{
		RESTInvitationWithCode minimal = new RESTInvitationWithCode(
				"formId", Instant.now().truncatedTo(ChronoUnit.SECONDS), null, null, "registrationCode");
		
		String jsonStr = JsonUtil.toJsonString(minimal);
		RESTInvitationWithCode minimalParsed = JsonUtil.parse(jsonStr, 
				RESTInvitationWithCode.class);
		
		assertThat(minimalParsed, is(minimal));
	}

	@Test
	public void completeFormSerializationIsIdempotent()
	{
		RESTInvitationWithCode complete = new RESTInvitationWithCode(
				"formId", Instant.now().truncatedTo(ChronoUnit.SECONDS), "add", "chan", "registrationCode");
		List<String> vals = new ArrayList<>();
		vals.add("value");
		Attribute attrP = new Attribute("attr", "string", "/", vals);
		complete.getAttributes().put(0, new PrefilledEntry<>(attrP, PrefilledEntryMode.READ_ONLY));
		complete.getIdentities().put(0, new PrefilledEntry<>(new IdentityParam("idType", 
				"user-id"), PrefilledEntryMode.READ_ONLY));
		complete.getGroupSelections().put(0, new PrefilledEntry<>(new Selection(true), 
				PrefilledEntryMode.READ_ONLY));
		
		String jsonStr = JsonUtil.toJsonString(complete);
		
		System.out.println(jsonStr);
		RESTInvitationWithCode completeParsed = JsonUtil.parse(jsonStr, 
				RESTInvitationWithCode.class);

		assertThat(completeParsed, is(complete));
	}

}
