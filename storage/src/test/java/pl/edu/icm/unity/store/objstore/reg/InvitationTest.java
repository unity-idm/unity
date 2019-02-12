/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

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
		InvitationParam param = new RegistrationInvitationParam("formId", 
				Instant.ofEpochMilli(1000), 
				"contactAddress");
		param.getAttributes().put(0, new PrefilledEntry<>(
				new Attribute("a1", "string", "/", Lists.newArrayList("v1")), 
				PrefilledEntryMode.DEFAULT));
		param.getGroupSelections().put(0, new PrefilledEntry<>(
				new GroupSelection("/A"), 
				PrefilledEntryMode.HIDDEN));
		param.getIdentities().put(0, new PrefilledEntry<>(
				new IdentityParam("id123", "identifier"), 
				PrefilledEntryMode.READ_ONLY));
		return new InvitationWithCode(param, id,
				Instant.ofEpochMilli(2000), 2);
	}

	@Override
	protected InvitationWithCode mutateObject(InvitationWithCode src)
	{
		InvitationParam param = new RegistrationInvitationParam("formId2", 
				Instant.ofEpochMilli(1001), 
				"contactAddress2");
		param.getAttributes().put(0, new PrefilledEntry<>(
				new Attribute("a2", "string", "/", Lists.newArrayList("v1")), 
				PrefilledEntryMode.DEFAULT));
		param.getGroupSelections().put(0, new PrefilledEntry<>(
				new GroupSelection("/A"), 
				PrefilledEntryMode.HIDDEN));
		param.getIdentities().put(0, new PrefilledEntry<>(
				new IdentityParam("id123", "identifier"), 
				PrefilledEntryMode.DEFAULT));
		return new InvitationWithCode(param, "changed-code",
				Instant.ofEpochMilli(2001), 3);
	}
}
