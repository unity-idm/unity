/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

public class RegistrationRequestTest extends AbstractNamedWithTSTest<RegistrationRequestState>
{
	@Autowired
	private RegistrationRequestDB dao;
	
	@Autowired
	private CredentialDB credentialDB;
	
	@Test
	public void usedCredUpdateIsRestrictedForPending()
	{
		tx.runInTransaction(() -> {
			CredentialDefinition cred = new CredentialDefinition("typeId", "cred", 
					new I18nString("dName"), new I18nString("desc"));
			cred.setConfiguration("");
			credentialDB.create(cred);
			
			RegistrationRequestState obj = getObject("name1");
			getDAO().create(obj);

			catchException(credentialDB).update(cred);
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Override
	protected NamedCRUDDAOWithTS<RegistrationRequestState> getDAO()
	{
		return dao;
	}

	@Override
	protected RegistrationRequestState getObject(String id)
	{
		RegistrationRequest req = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
					.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						new Attribute("email", "email", "/",
								Lists.newArrayList("foo@example.com")))
				.withAddedCredential()
				.withCredentialId("cred")
				.withSecrets("sec").endCredential()
				.withAddedGroupSelection().withGroup("/some/group").endGroupSelection()
				.withAddedIdentity("x500", "CN=registration test")
				.endIdentity()
				.build();
		
		RegistrationRequestState state = new RegistrationRequestState();
		state.setAdminComments(Lists.newArrayList(new AdminComment("contents", 3, true)));
		state.setRegistrationContext(new RegistrationContext(true, TriggeringMode.afterRemoteLoginWhenUnknownUser));
		state.setRequest(req);
		state.setRequestId(id);
		state.setStatus(RegistrationRequestStatus.pending);
		state.setTimestamp(new Date(123));
		state.setCreatedEntityId(123l);
		return state;
	}

	@Override
	protected RegistrationRequestState mutateObject(RegistrationRequestState state)
	{
		RegistrationRequest req = new RegistrationRequestBuilder()
				.withFormId("f2")
				.withComments("comments2")
				.withRegistrationCode("1232")
				.withAddedAgreement()
					.withSelected(false)
				.endAgreement()
				.withAddedAttribute(
						new Attribute("cn", "string", "/A",
								Lists.newArrayList("AAA")))
				.withAddedCredential()
				.withCredentialId("cred2")
				.withSecrets("sec2").endCredential()
				.withAddedGroupSelection().endGroupSelection()
				.withAddedGroupSelection().withGroup("/other").withGroup("/another").endGroupSelection()
				.withAddedIdentity("username", "ss")
				.endIdentity()
				.build();

		
		state.setAdminComments(Lists.newArrayList(new AdminComment("contents2", 4, false)));
		state.setRegistrationContext(new RegistrationContext(true, TriggeringMode.manualAtLogin));
		state.setRequest(req);
		state.setRequestId("requestId2");
		state.setStatus(RegistrationRequestStatus.accepted);
		state.setTimestamp(new Date(12300));
		state.setCreatedEntityId(678l);
		return state;
	}
}
