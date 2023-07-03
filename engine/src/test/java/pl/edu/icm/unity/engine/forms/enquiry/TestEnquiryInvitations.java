/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.forms.enquiry;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.ConfirmationMode;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponseBuilder;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * 
 * @author P.Piernik
 *
 */
public class TestEnquiryInvitations extends DBIntegrationTestBase
{
	@Autowired
	private InvitationDB invitationDB;
	@Autowired
	private TransactionalRunner txRunner;
	@Autowired
	private InvitationManagement invitationMan;
	@Autowired
	private EnquiryManagement enquiryMan;
	@Autowired
	private EnquiryResponsePreprocessor validator;

	@Test
	public void shouldRespectIdentityConfirmationFromInvitationWhenVerifiedOnSubmit() throws EngineException
	{
		EnquiryResponse response = getIdentityResponse();
		InvitationWithCode invitationWithCode = getIdentityInvitation();
		txRunner.runInTransactionThrowing(() -> {
			invitationDB.create(invitationWithCode);
			validator.validateSubmittedResponse(getIdentityForm(ConfirmationMode.ON_SUBMIT), getEnquiryResponseState(response),
					true);
		});

		assertThat(response.getIdentities().get(0).isConfirmed(), is(true));
	}

	@Test
	public void shouldRespectIdentityConfirmationFromInvitationWhenVerifiedOnAcceptance() throws EngineException
	{
		EnquiryResponse response = getIdentityResponse();
		InvitationWithCode invitationWithCode = getIdentityInvitation();

		txRunner.runInTransactionThrowing(() -> {
			invitationDB.create(invitationWithCode);
			validator.validateSubmittedResponse(getIdentityForm(ConfirmationMode.ON_ACCEPT),
					getEnquiryResponseState(response), true);
		});

		assertThat(response.getIdentities().get(0).isConfirmed(), is(true));
	}

	@Test
	public void shouldRespectAttributeConfirmationFromInvitationWhenVerifiedOnSubmit() throws EngineException
	{
		EnquiryResponse response = getAttributeResponse();
		InvitationWithCode invitationWithCode = getAttributeInvitation();
		txRunner.runInTransactionThrowing(() -> {
			invitationDB.create(invitationWithCode);
			validator.validateSubmittedResponse(getAttributeForm(ConfirmationMode.ON_SUBMIT), 
					getEnquiryResponseState(response),
					true);
		});

		String rawValue = response.getAttributes().get(0).getValues().get(0);
		assertThat(VerifiableEmail.fromJsonString(rawValue).isConfirmed(), is(true));
	}

	@Test
	public void shouldRespectAttributeConfirmationFromInvitationWhenVerifiedOnAcceptance() throws EngineException
	{
		EnquiryResponse response = getAttributeResponse();
		InvitationWithCode invitationWithCode = getAttributeInvitation();
		txRunner.runInTransactionThrowing(() -> {
			invitationDB.create(invitationWithCode);
			validator.validateSubmittedResponse(getAttributeForm(ConfirmationMode.ON_ACCEPT), 
					getEnquiryResponseState(response),
					true);
		});

		String rawValue = response.getAttributes().get(0).getValues().get(0);
		assertThat(VerifiableEmail.fromJsonString(rawValue).isConfirmed(), is(true));
	}

	@Test
	public void shouldOverrideIdentityConfirmationFromInvitationWhenSetToUnverified() throws EngineException
	{
		EnquiryResponse response = getIdentityResponse();
		InvitationWithCode invitationWithCode = getIdentityInvitation();
		txRunner.runInTransactionThrowing(() -> {
			invitationDB.create(invitationWithCode);
			validator.validateSubmittedResponse(getIdentityForm(ConfirmationMode.DONT_CONFIRM), 
					getEnquiryResponseState(response),
					true);
		});

		assertThat(response.getIdentities().get(0).isConfirmed(), is(false));
	}

	@Test
	public void shouldOverrideAttributeConfirmationFromInvitationWhenSetToUnverified() throws EngineException
	{
		EnquiryResponse response = getAttributeResponse();
		InvitationWithCode invitationWithCode = getAttributeInvitation();
		txRunner.runInTransactionThrowing(() -> {
			invitationDB.create(invitationWithCode);
			validator.validateSubmittedResponse(getAttributeForm(ConfirmationMode.DONT_CONFIRM), 
					getEnquiryResponseState(response),
					true);
		});

		String rawValue = response.getAttributes().get(0).getValues().get(0);
		assertThat(VerifiableEmail.fromJsonString(rawValue).isConfirmed(), is(false));
	}

	@Test
	public void shouldReturnUpdatedInvitation() throws EngineException
	{
		EnquiryInvitationParam invitation = EnquiryInvitationParam.builder().withForm("form").withEntity(1L)
				.withExpiration(Instant.now().plusSeconds(1000)).build();
		enquiryMan.addEnquiry(new EnquiryFormBuilder().withTargetGroups(new String[] { "/" })
				.withType(EnquiryType.REQUESTED_OPTIONAL).withName("form").build());
		String code = invitationMan.addInvitation(invitation);
		invitation.getFormPrefill().getMessageParams().put("added", "param");

		invitationMan.updateInvitation(code, invitation);

		InvitationWithCode returnedInvitation = invitationMan.getInvitation(code);
		assertThat(((EnquiryInvitationParam) returnedInvitation.getInvitation()).getFormPrefill().getMessageParams().get("added"), is("param"));
	}

	@Test
	public void shouldBlockSendInvitationWithoutEntity() throws EngineException
	{
		enquiryMan.addEnquiry(new EnquiryFormBuilder().withTargetGroups(new String[] { "/" })
				.withType(EnquiryType.REQUESTED_OPTIONAL).withName("form").build());
		EnquiryInvitationParam param = EnquiryInvitationParam.builder().withForm("form")
				.withExpiration(Instant.now().plusSeconds(1000)).withContactAddress("demo@demo.pl")
				.build();
		String code = invitationMan.addInvitation(param);
		Throwable exception = catchThrowable(() -> invitationMan.sendInvitation(code));
		assertExceptionType(exception, WrongArgumentException.class);
	}

	@Test
	public void shouldBlockSubmitResponseRelatedToInvalidInvitation() throws EngineException
	{
		EnquiryResponse response = addCompleteInvitationAndGetResponse(-1000);

		Throwable exception = catchThrowable(() -> enquiryMan.submitEnquiryResponse(response,
				new RegistrationContext(false, TriggeringMode.manualStandalone)));
		assertExceptionType(exception, IllegalFormContentsException.class);	
	}
	
	@Test
	public void shouldBlockSubmitResponseWithUsedInvitationCode() throws EngineException
	{
		EnquiryResponse response = addCompleteInvitationAndGetResponse(1000);

		enquiryMan.submitEnquiryResponse(response,
				new RegistrationContext(false, TriggeringMode.manualStandalone));
		
		
		Throwable exception = catchThrowable(() -> enquiryMan.submitEnquiryResponse(response,
				new RegistrationContext(false, TriggeringMode.manualStandalone)));
		assertExceptionType(exception, IllegalFormContentsException.class);	
	}

	private  EnquiryResponse addCompleteInvitationAndGetResponse(int expirationTime) throws EngineException
	{
		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), EntityState.valid);

		enquiryMan.addEnquiry(new EnquiryFormBuilder().withTargetGroups(new String[] { "/" })
				.withType(EnquiryType.REQUESTED_OPTIONAL).withName("form").withAddedIdentityParam()
				.withIdentityType(EmailIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive)
				.withConfirmationMode(ConfirmationMode.CONFIRMED).endIdentityParam()
				.withAddedGroupParam().withGroupPath("/**").endGroupParam().build());

		IdentityParam newIdentity = new IdentityParam(EmailIdentity.ID, "test@example.com");
		newIdentity.setConfirmationInfo(new ConfirmationInfo(true));
		InvitationParam invitation = EnquiryInvitationParam.builder().withForm("form")
				.withExpiration(Instant.now().plusSeconds(expirationTime))
				.withIdentity(newIdentity, PrefilledEntryMode.HIDDEN).withEntity(added.getEntityId())
				.withContactAddress("demo@demo.com").build();

		String code = invitationMan.addInvitation(invitation);
		assertThat(invitationMan.getInvitations().size(), is(1));

		return new EnquiryResponseBuilder().withRegistrationCode(code).withFormId("form")
				.withIdentities(Lists.newArrayList(newIdentity))
				.withGroupSelections(Arrays.asList(new GroupSelection(Arrays.asList("/")))).build();
	}
	
	@Test
	public void testFullInvitationFlow() throws EngineException
	{

		Identity added = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), EntityState.valid);
		aTypeMan.addAttributeType(new AttributeType("email", VerifiableEmailAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/A/B"));
		groupsMan.addGroup(new Group("/A/C"));

		enquiryMan.addEnquiry(new EnquiryFormBuilder().withTargetGroups(new String[] { "/" })
				.withType(EnquiryType.REQUESTED_OPTIONAL).withName("form").withAddedAttributeParam()
				.withAttributeType("email").withGroup("/").endAttributeParam().withAddedIdentityParam()
				.withIdentityType(EmailIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive)
				.withConfirmationMode(ConfirmationMode.CONFIRMED).endIdentityParam()
				.withAddedGroupParam().withGroupPath("/A/**").endGroupParam().build());

		// Add Invitation
		IdentityParam newIdentity = new IdentityParam(EmailIdentity.ID, "test@example.com");
		newIdentity.setConfirmationInfo(new ConfirmationInfo(true));
		InvitationParam invitation = EnquiryInvitationParam.builder().withForm("form")
				.withExpiration(Instant.now().plusSeconds(1000))
				.withIdentity(newIdentity, PrefilledEntryMode.HIDDEN).withEntity(added.getEntityId())
				.withContactAddress("demo@demo.com").build();

		String code = invitationMan.addInvitation(invitation);
		assertThat(invitationMan.getInvitations().size(), is(1));

		// Submit response related to the invitation
		Attribute email = VerifiableEmailAttribute.of("email", "/",
				new VerifiableEmail("test@example.com", new ConfirmationInfo(true)));
		EnquiryResponse response = new EnquiryResponseBuilder().withRegistrationCode(code).withFormId("form")
				.withIdentities(Lists.newArrayList(newIdentity))
				.withAttributes(Lists.newArrayList(email))
				.withGroupSelections(Arrays.asList(new GroupSelection(Arrays.asList("/A/B")))).build();

		String id = enquiryMan.submitEnquiryResponse(response,
				new RegistrationContext(false, TriggeringMode.manualStandalone));
		
		assertThat(invitationMan.getInvitations().size(), is(0));

		// Accept request
		enquiryMan.processEnquiryResponse(id, response, RegistrationRequestAction.accept, null, null);

		// Check result after accept
		Entity entity = idsMan.getEntity(new EntityParam(added.getEntityId()));
		Collection<AttributeExt> allAttributes = attrsMan.getAllAttributes(new EntityParam(entity.getId()),
				false, "/", "email", false);
		assertThat(allAttributes.size(), is(1));
		assertThat(allAttributes.iterator().next().getValues().get(0), is(email.getValues().iterator().next()));
		Identity emailId = entity.getIdentities().stream().filter(i -> i.getTypeId().equals(EmailIdentity.ID))
				.findFirst().orElse(null);
		assertThat(emailId, is(notNullValue()));
		assertThat(emailId.getValue(), is("test@example.com"));
		GroupContents contents = groupsMan.getContents("/A/B", GroupContents.MEMBERS);
		assertThat(contents.getMembers().size(), is(1));
		assertThat(contents.getMembers().iterator().next().getEntityId(), is(entity.getId()));
	}

	private EnquiryResponseState getEnquiryResponseState(EnquiryResponse response)
	{
		EnquiryResponseState state = new EnquiryResponseState();
		state.setRequest(response);
		return state;
	}
	
	private EnquiryForm getAttributeForm(ConfirmationMode confirmationMode)
	{
		return new EnquiryFormBuilder().withName("form").withType(EnquiryType.REQUESTED_OPTIONAL)
				.withTargetGroups(new String[] { "/" }).withAddedAttributeParam()
				.withAttributeType(InitializerCommon.EMAIL_ATTR).withGroup("/")
				.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive)
				.withConfirmationMode(confirmationMode).endAttributeParam().build();
	}

	private InvitationWithCode getAttributeInvitation()
	{
		Attribute email = VerifiableEmailAttribute.of("email", "/",
				new VerifiableEmail("test@example.com", new ConfirmationInfo(true)));
		String invitationCode = "code";
		InvitationParam invitation = EnquiryInvitationParam.builder().withForm("form").withEntity(1L)
				.withAttribute(email, PrefilledEntryMode.HIDDEN)
				.withExpiration(Instant.now().plusSeconds(1000)).build();
		return new InvitationWithCode(invitation, invitationCode, null, 0);
	}

	private EnquiryResponse getAttributeResponse() throws EngineException
	{
		aTypeMan.addAttributeType(new AttributeType("email", VerifiableEmailAttributeSyntax.ID));
		return new EnquiryResponseBuilder().withRegistrationCode("code").withFormId("form")
				.withAttributes(Lists.newArrayList((Attribute) null)).build();
	}

	private EnquiryResponse getIdentityResponse()
	{
		return new EnquiryResponseBuilder().withRegistrationCode("code").withFormId("form")
				.withIdentities(Lists.newArrayList((IdentityParam) null)).build();
	}

	private InvitationWithCode getIdentityInvitation()
	{
		IdentityParam identity = new IdentityParam(EmailIdentity.ID, "test@example.com");
		identity.setConfirmationInfo(new ConfirmationInfo(true));
		String invitationCode = "code";
		InvitationParam invitation = EnquiryInvitationParam.builder().withForm("form")
				.withIdentity(identity, PrefilledEntryMode.HIDDEN).withEntity(1L)
				.withExpiration(Instant.now().plusSeconds(1000)).build();

		return new InvitationWithCode(invitation, invitationCode, null, 0);
	}

	private EnquiryForm getIdentityForm(ConfirmationMode confirmationMode)
	{
		return new EnquiryFormBuilder().withType(EnquiryType.REQUESTED_OPTIONAL)
				.withTargetGroups(new String[] { "/" }).withName("form").withAddedIdentityParam()
				.withIdentityType(EmailIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive)
				.withConfirmationMode(confirmationMode).endIdentityParam().build();
	}
}
