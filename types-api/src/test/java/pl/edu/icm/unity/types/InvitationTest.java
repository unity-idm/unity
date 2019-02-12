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
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

public class InvitationTest
{
	@Test
	public void mnimalFormSerializationIsIdempotent()
	{
		InvitationWithCode minimal = new InvitationWithCode(new RegistrationInvitationParam(
				"formId", Instant.now().truncatedTo(ChronoUnit.SECONDS), null), "registrationCode");
		
		String jsonStr = JsonUtil.toJsonString(minimal);
		InvitationWithCode minimalParsed = JsonUtil.parse(jsonStr, 
				InvitationWithCode.class);
		
		assertThat(minimalParsed, is(minimal));
	}

	@Test
	public void completeFormSerializationIsIdempotent()
	{
		InvitationWithCode complete = new InvitationWithCode(new RegistrationInvitationParam(
				"formId", Instant.now().truncatedTo(ChronoUnit.SECONDS), "add"), "registrationCode");
		List<String> vals = new ArrayList<>();
		vals.add("value");
		Attribute attrP = new Attribute("attr", "string", "/", vals);
		complete.getInvitation().getAttributes().put(0, new PrefilledEntry<>(attrP, PrefilledEntryMode.READ_ONLY));
		complete.getInvitation().getIdentities().put(0, new PrefilledEntry<>(new IdentityParam("idType", 
				"user-id"), PrefilledEntryMode.READ_ONLY));
		complete.getInvitation().getGroupSelections().put(0, new PrefilledEntry<>(new GroupSelection("/foo"), 
				PrefilledEntryMode.READ_ONLY));
		complete.getInvitation().getAllowedGroups().put(0, new GroupSelection(Arrays.asList("/foo","/bar")));
		
		String jsonStr = JsonUtil.toJsonString(complete);
		
		System.out.println(jsonStr);
		InvitationWithCode completeParsed = JsonUtil.parse(jsonStr, 
				InvitationWithCode.class);

		assertThat(completeParsed, is(complete));
	}

}
