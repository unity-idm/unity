/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration.invite;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invitation.ComboInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;

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
		
		assertThat(minimalParsed).isEqualTo(minimal);
	}

	@Test
	public void completeRegFormSerializationIsIdempotent()
	{
		InvitationWithCode complete = new InvitationWithCode(new RegistrationInvitationParam(
				"formId", Instant.now().truncatedTo(ChronoUnit.SECONDS), "add"), "registrationCode");
		List<String> vals = new ArrayList<>();
		vals.add("value");
		Attribute attrP = new Attribute("attr", "string", "/", vals);
		RegistrationInvitationParam registrationInvitationParam = (RegistrationInvitationParam) complete.getInvitation();
		
		registrationInvitationParam.getFormPrefill().getAttributes().put(0, new PrefilledEntry<>(attrP, PrefilledEntryMode.READ_ONLY));
		registrationInvitationParam.getFormPrefill().getIdentities().put(0, new PrefilledEntry<>(new IdentityParam("idType", 
				"user-id"), PrefilledEntryMode.READ_ONLY));
		registrationInvitationParam.getFormPrefill().getGroupSelections().put(0, new PrefilledEntry<>(new GroupSelection("/foo"), 
				PrefilledEntryMode.READ_ONLY));
		registrationInvitationParam.getFormPrefill().getAllowedGroups().put(0, new GroupSelection(Arrays.asList("/foo","/bar")));
		
		String jsonStr = JsonUtil.toJsonString(complete);
		
		System.out.println(jsonStr);
		InvitationWithCode completeParsed = JsonUtil.parse(jsonStr, 
				InvitationWithCode.class);

		assertThat(completeParsed).isEqualTo(complete);
	}
	
	@Test
	public void completeComboFormSerializationIsIdempotent()
	{
		InvitationWithCode complete = new InvitationWithCode(new ComboInvitationParam(
				"formId", "formId2", Instant.now().truncatedTo(ChronoUnit.SECONDS), "add"), "registrationCode");
		List<String> vals = new ArrayList<>();
		vals.add("value");
		Attribute attrP = new Attribute("attr", "string", "/", vals);
		ComboInvitationParam comboInvitationParam = (ComboInvitationParam) complete.getInvitation();
		
		comboInvitationParam.getRegistrationFormPrefill().getAttributes().put(0, new PrefilledEntry<>(attrP, PrefilledEntryMode.READ_ONLY));
		comboInvitationParam.getRegistrationFormPrefill().getIdentities().put(0, new PrefilledEntry<>(new IdentityParam("idType", 
				"user-id"), PrefilledEntryMode.READ_ONLY));
		comboInvitationParam.getRegistrationFormPrefill().getGroupSelections().put(0, new PrefilledEntry<>(new GroupSelection("/foo"), 
				PrefilledEntryMode.READ_ONLY));
		comboInvitationParam.getRegistrationFormPrefill().getAllowedGroups().put(0, new GroupSelection(Arrays.asList("/foo","/bar")));
		
		comboInvitationParam.getEnquiryFormPrefill().getAttributes().put(0, new PrefilledEntry<>(attrP, PrefilledEntryMode.READ_ONLY));
		comboInvitationParam.getEnquiryFormPrefill().getIdentities().put(0, new PrefilledEntry<>(new IdentityParam("idType", 
				"user-id"), PrefilledEntryMode.READ_ONLY));
		comboInvitationParam.getEnquiryFormPrefill().getGroupSelections().put(0, new PrefilledEntry<>(new GroupSelection("/foo"), 
				PrefilledEntryMode.READ_ONLY));
		comboInvitationParam.getEnquiryFormPrefill().getAllowedGroups().put(0, new GroupSelection(Arrays.asList("/foo","/bar")));
		
		String jsonStr = JsonUtil.serializeHumanReadable(JsonUtil.toJsonNode(complete));
		
		System.out.println(jsonStr);
		InvitationWithCode completeParsed = JsonUtil.parse(jsonStr, InvitationWithCode.class);

		assertThat(completeParsed).isEqualTo(complete);
	}

}
