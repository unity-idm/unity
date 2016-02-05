/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.types.basic.AttributeParamRepresentation;
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
		
		String jsonStr = UnityTypesFactory.toJsonString(minimal);
		RESTInvitationWithCode minimalParsed = UnityTypesFactory.parse(jsonStr, 
				RESTInvitationWithCode.class);
		
		assertThat(minimalParsed, is(minimal));
	}

	@Test
	public void completeFormSerializationIsIdempotent()
	{
		RESTInvitationWithCode complete = new RESTInvitationWithCode(
				"formId", Instant.now().truncatedTo(ChronoUnit.SECONDS), "add", "chan", "registrationCode");
		AttributeParamRepresentation attrP = new AttributeParamRepresentation();
		attrP.setGroupPath("/");
		attrP.setName("attr");
		List<Object> vals = new ArrayList<>();
		vals.add("value");
		attrP.setValues(vals);
		complete.getAttributes().put(0, new PrefilledEntry<>(attrP, PrefilledEntryMode.READ_ONLY));
		complete.getIdentities().put(0, new PrefilledEntry<>(new IdentityParam("idType", 
				"user-id"), PrefilledEntryMode.READ_ONLY));
		complete.getGroupSelections().put(0, new PrefilledEntry<>(new Selection(true), 
				PrefilledEntryMode.READ_ONLY));
		
		String jsonStr = UnityTypesFactory.toJsonString(complete);
		
		System.out.println(jsonStr);
		RESTInvitationWithCode completeParsed = UnityTypesFactory.parse(jsonStr, 
				RESTInvitationWithCode.class);

		assertThat(completeParsed, is(complete));
	}

}
