/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.ConfirmationMode;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.base.registration.invite.InvitationParam;
import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.base.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

public class TestRegistrationInvitations extends DBIntegrationTestBase
{
	@Autowired
	private RegistrationRequestPreprocessor validator;
	@Autowired
	private InvitationDB invitationDB;
	@Autowired
	private TransactionalRunner txRunner;
	@Autowired
	private InvitationManagement invitationMan;
	
	@Test
	public void shouldRespectIdentityConfirmationFromInvitationWhenVerifiedOnSubmit() throws EngineException
	{
		RegistrationRequest request = getRegistrationRequestWithNullIdentity();
		InvitationWithCode invitationWithCode = getIdentityInvitation();
		txRunner.runInTransactionThrowing(() -> 
		{
			invitationDB.create(invitationWithCode);	
			validator.validateSubmittedRequest(getIdentityForm(ConfirmationMode.ON_SUBMIT), request, true);
		});
		
		assertThat(request.getIdentities().get(0).isConfirmed(), is(true));
	}

	@Test
	public void shouldRespectIdentityConfirmationFromInvitationWhenVerifiedOnAcceptance() throws EngineException
	{
		RegistrationRequest request = getRegistrationRequestWithNullIdentity();
		InvitationWithCode invitationWithCode = getIdentityInvitation();

		txRunner.runInTransactionThrowing(() -> 
		{
			invitationDB.create(invitationWithCode);	
			validator.validateSubmittedRequest(getIdentityForm(ConfirmationMode.ON_ACCEPT), request, true);
		});
		
		assertThat(request.getIdentities().get(0).isConfirmed(), is(true));
	}
	
	@Test
	public void shouldRespectAttributeConfirmationFromInvitationWhenVerifiedOnSubmit() throws EngineException
	{
		RegistrationRequest request = getRegistrationRequestWithEmailAttr();
		InvitationWithCode invitationWithCode = getAttributeInvitation();
		txRunner.runInTransactionThrowing(() -> 
		{
			invitationDB.create(invitationWithCode);	
			validator.validateSubmittedRequest(getAttributeForm(ConfirmationMode.ON_SUBMIT), request, true);
		});
		
		String rawValue = request.getAttributes().get(0).getValues().get(0);
		assertThat(VerifiableEmail.fromJsonString(rawValue).isConfirmed(), is(true));
	}

	@Test
	public void shouldRespectAttributeConfirmationFromInvitationWhenVerifiedOnAcceptance() throws EngineException
	{
		RegistrationRequest request = getRegistrationRequestWithEmailAttr();
		InvitationWithCode invitationWithCode = getAttributeInvitation();
		txRunner.runInTransactionThrowing(() -> 
		{
			invitationDB.create(invitationWithCode);	
			validator.validateSubmittedRequest(getAttributeForm(ConfirmationMode.ON_ACCEPT), request, true);
		});
		
		String rawValue = request.getAttributes().get(0).getValues().get(0);
		assertThat(VerifiableEmail.fromJsonString(rawValue).isConfirmed(), is(true));
	}

	
	@Test
	public void shouldOverrideIdentityConfirmationFromInvitationWhenSetToUnverified() throws EngineException
	{
		RegistrationRequest request = getRegistrationRequestWithNullIdentity();
		InvitationWithCode invitationWithCode = getIdentityInvitation();
		txRunner.runInTransactionThrowing(() -> 
		{
			invitationDB.create(invitationWithCode);	
			validator.validateSubmittedRequest(getIdentityForm(ConfirmationMode.DONT_CONFIRM), request, true);
		});
		
		assertThat(request.getIdentities().get(0).isConfirmed(), is(false));
	}

	@Test
	public void shouldOverrideAttributeConfirmationFromInvitationWhenSetToUnverified() throws EngineException
	{
		RegistrationRequest request = getRegistrationRequestWithEmailAttr();
		InvitationWithCode invitationWithCode = getAttributeInvitation();
		txRunner.runInTransactionThrowing(() -> 
		{
			invitationDB.create(invitationWithCode);	
			validator.validateSubmittedRequest(getAttributeForm(ConfirmationMode.DONT_CONFIRM), request, true);
		});
		
		String rawValue = request.getAttributes().get(0).getValues().get(0);
		assertThat(VerifiableEmail.fromJsonString(rawValue).isConfirmed(), is(false));
	}

	@Test
	public void shouldReturnUpdatedInvitation() throws EngineException
	{
		RegistrationInvitationParam invitation = RegistrationInvitationParam.builder()
				.withForm("form")
				.withExpiration(Instant.now().plusSeconds(1000))
				.build();
		
		registrationsMan.addForm(new RegistrationFormBuilder()
				.withName("form")
				.withPubliclyAvailable(true)
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.build());
		String code = invitationMan.addInvitation(invitation);
		invitation.getFormPrefill().getMessageParams().put("added", "param");
		
		invitationMan.updateInvitation(code, invitation);
		
		InvitationWithCode returnedInvitation = invitationMan.getInvitation(code);
		assertThat(((RegistrationInvitationParam) returnedInvitation.getInvitation()).getFormPrefill().getMessageParams().get("added"), is("param"));
	}
	
	@Test
	public void formWithInvitationCantBeUpdated() throws Exception
	{
		InvitationParam invitation = RegistrationInvitationParam.builder()
				.withForm("form")
				.withExpiration(Instant.now().plusSeconds(1000))
				.build();
		
		RegistrationForm form = new RegistrationFormBuilder()
				.withName("form")
				.withPubliclyAvailable(true)
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.build();
		registrationsMan.addForm(form);
		invitationMan.addInvitation(invitation);
		Throwable exception = catchThrowable(() -> registrationsMan.updateForm(form, false));
		Assertions.assertThat(exception).isNotNull().isInstanceOf(SchemaConsistencyException.class);	
	}

	
	@Test
	public void invitationWithNotMatchingIdentityIsRejected() throws Exception
	{
		InvitationParam invitation = RegistrationInvitationParam.builder()
				.withForm("form")
				.withIdentity(new IdentityParam(UsernameIdentity.ID, "wrong"), PrefilledEntryMode.READ_ONLY)
				.withExpiration(Instant.now().plusSeconds(1000))
				.build();
		
		RegistrationForm form = new RegistrationFormBuilder()
				.withName("form")
				.withPubliclyAvailable(true)
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedIdentityParam().withIdentityType(EmailIdentity.ID).endIdentityParam()
				.build();
		registrationsMan.addForm(form);
		
		Throwable exception = catchThrowable(() -> invitationMan.addInvitation(invitation));

		assertThat(exception).isNotNull().isInstanceOf(IllegalArgumentException.class);	
	}
	
	
	
	private RegistrationForm getIdentityForm(ConfirmationMode confirmationMode)
	{
		return new RegistrationFormBuilder()
				.withName("form")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedIdentityParam()
					.withIdentityType(EmailIdentity.ID)
					.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive)
					.withConfirmationMode(confirmationMode)
				.endIdentityParam()
				.build();
	}

	private InvitationWithCode getIdentityInvitation()
	{
		IdentityParam identity = new IdentityParam(EmailIdentity.ID, "test@example.com");
		identity.setConfirmationInfo(new ConfirmationInfo(true));
		String invitationCode = "code";
		InvitationParam invitation = RegistrationInvitationParam.builder()
			.withForm("form")
			.withIdentity(identity, PrefilledEntryMode.HIDDEN)
			.withExpiration(Instant.now().plusSeconds(1000))
			.build();
		return new InvitationWithCode(invitation, invitationCode, null, 0);
	}
	
	private RegistrationForm getAttributeForm(ConfirmationMode confirmationMode)
	{
		return new RegistrationFormBuilder()
				.withName("form")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedAttributeParam()
					.withAttributeType(InitializerCommon.EMAIL_ATTR).withGroup("/")
					.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive)
					.withConfirmationMode(confirmationMode)
				.endAttributeParam()
				.build();
	}
	
	private InvitationWithCode getAttributeInvitation()
	{
		Attribute email = VerifiableEmailAttribute.of("email", "/", 
				new VerifiableEmail("test@example.com", new ConfirmationInfo(true)));
		String invitationCode = "code";
		InvitationParam invitation = RegistrationInvitationParam.builder()
			.withForm("form")
			.withAttribute(email, PrefilledEntryMode.HIDDEN)
			.withExpiration(Instant.now().plusSeconds(1000))
			.build();
		return new InvitationWithCode(invitation, invitationCode, null, 0);
	}
	
	private RegistrationRequest getRegistrationRequestWithNullIdentity()
	{
		return new RegistrationRequestBuilder()
				.withRegistrationCode("code")
				.withFormId("form")
				.withIdentities(Lists.newArrayList((IdentityParam)null))
				.build();
	}

	private RegistrationRequest getRegistrationRequestWithEmailAttr() throws EngineException
	{
		aTypeMan.addAttributeType(new AttributeType("email", VerifiableEmailAttributeSyntax.ID));
		return new RegistrationRequestBuilder()
				.withRegistrationCode("code")
				.withFormId("form")
				.withAttributes(Lists.newArrayList((Attribute)null))
				.build();
	}
}
