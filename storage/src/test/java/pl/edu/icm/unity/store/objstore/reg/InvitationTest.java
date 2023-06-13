/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.base.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;

public class InvitationTest extends AbstractNamedWithTSTest<InvitationWithCode>
{
	@Autowired
	private InvitationDB dao;
	
	@Override
	protected NamedCRUDDAOWithTS<InvitationWithCode> getDAO()
	{
		return dao;
	}

	@Override
	protected InvitationWithCode getObject(String id)
	{
		RegistrationInvitationParam param = new RegistrationInvitationParam("formId", 
				Instant.ofEpochMilli(1000), 
				"contactAddress");
		param.getFormPrefill().getAttributes().put(0, new PrefilledEntry<>(
				new Attribute("a1", "string", "/", Lists.newArrayList("v1")), 
				PrefilledEntryMode.DEFAULT));
		param.getFormPrefill().getGroupSelections().put(0, new PrefilledEntry<>(
				new GroupSelection("/A"), 
				PrefilledEntryMode.HIDDEN));
		param.getFormPrefill().getIdentities().put(0, new PrefilledEntry<>(
				new IdentityParam("id123", "identifier"), 
				PrefilledEntryMode.READ_ONLY));
		return new InvitationWithCode(param, id,
				Instant.ofEpochMilli(2000), 2);
	}

	@Override
	protected InvitationWithCode mutateObject(InvitationWithCode src)
	{
		RegistrationInvitationParam param = new RegistrationInvitationParam("formId2", 
				Instant.ofEpochMilli(1001), 
				"contactAddress2");
		param.getFormPrefill().getAttributes().put(0, new PrefilledEntry<>(
				new Attribute("a2", "string", "/", Lists.newArrayList("v1")), 
				PrefilledEntryMode.DEFAULT));
		param.getFormPrefill().getGroupSelections().put(0, new PrefilledEntry<>(
				new GroupSelection("/A"), 
				PrefilledEntryMode.HIDDEN));
		param.getFormPrefill().getIdentities().put(0, new PrefilledEntry<>(
				new IdentityParam("id123", "identifier"), 
				PrefilledEntryMode.DEFAULT));
		return new InvitationWithCode(param, "changed-code",
				Instant.ofEpochMilli(2001), 3);
	}
}
