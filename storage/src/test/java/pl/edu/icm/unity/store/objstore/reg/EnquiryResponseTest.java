/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseBuilder;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

public class EnquiryResponseTest extends BaseRequestTest<EnquiryResponseState>
{
	@Autowired
	private EnquiryResponseDB dao;
	
	@Override
	protected NamedCRUDDAOWithTS<EnquiryResponseState> getDAO()
	{
		return dao;
	}

	@Override
	protected EnquiryResponseState getObject(String id)
	{
		EnquiryResponse req = new EnquiryResponseBuilder()
				.withFormId("f1")
				.withComments("comments")
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
		
		EnquiryResponseState state = new EnquiryResponseState();
		state.setAdminComments(Lists.newArrayList(new AdminComment("contents", 3, true)));
		state.setRegistrationContext(new RegistrationContext(true, TriggeringMode.manualAtLogin));
		state.setRequest(req);
		state.setRequestId(id);
		state.setStatus(RegistrationRequestStatus.pending);
		state.setTimestamp(new Date(123));
		state.setEntityId(2);
		return state;
	}

	@Override
	protected EnquiryResponseState mutateObject(EnquiryResponseState state)
	{
		EnquiryResponse req = new EnquiryResponseBuilder()
				.withFormId("f2")
				.withComments("comments2")
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
		state.setEntityId(100);
		return state;
	}
}
