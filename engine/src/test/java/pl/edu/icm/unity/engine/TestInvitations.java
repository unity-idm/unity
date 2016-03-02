/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.builders.RegistrationRequestBuilder;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.engine.mock.MockNotificationFacility;
import pl.edu.icm.unity.engine.mock.MockNotificationFacility.Message;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfileBuilder;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.NotificationChannel;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 *
 * @author Krzysztof Benedyczak
 */
public class TestInvitations  extends DBIntegrationTestBase
{
	public static final String TEST_FORM = "form-1";
	
	private static final RegistrationContext REG_CONTEXT = new RegistrationContext(
			false, false, TriggeringMode.manualStandalone);
	
	@Autowired
	private RegistrationActionsRegistry regActionsRegistry;
	
	@Autowired
	private InitializerCommon commonInitializer;
	
	@Autowired
	private MockNotificationFacility mockNotificationFacility;
	
	@Test
	public void addedInvitationIsReturned() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100).
				truncatedTo(ChronoUnit.SECONDS));
		String code = registrationsMan.addInvitation(invitation);
		InvitationWithCode invitationEnh = new InvitationWithCode(invitation, code, null, 0);
		
		List<InvitationWithCode> invitations = registrationsMan.getInvitations();
		assertThat(invitations.size(), is(1));
		assertThat(invitations.get(0), is(invitationEnh));
	}

	@Test
	public void removedInvitationIsNotReturned() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		String code = registrationsMan.addInvitation(invitation);
		
		registrationsMan.removeInvitation(code);
		
		List<InvitationWithCode> invitations = registrationsMan.getInvitations();
		assertThat(invitations.isEmpty(), is(true));
	}
	
	@Test
	public void invitationIsSentWhenRequested() throws Exception
	{
		initAndCreateForm(true);
		notMan.addNotificationChannel(new NotificationChannel("mock-chan", "", "", MockNotificationFacility.NAME));
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100), 
				"someAddr", "mock-chan");
		String code = registrationsMan.addInvitation(invitation);
		mockNotificationFacility.resetSent();
		
		registrationsMan.sendInvitation(code);
		
		List<Message> sent = mockNotificationFacility.getSent();
		assertThat(sent.size(), is(1));
		assertThat(sent.get(0).address, is("someAddr"));
		assertThat(sent.get(0).subject, is("Registration invitation"));
		
		InvitationWithCode invitation2 = registrationsMan.getInvitation(code);
		assertThat(invitation2.getLastSentTime(), is(notNullValue()));
		assertThat(invitation2.getNumberOfSends(), is(1));
	}

	@Test
	public void invitationIsDisposedAfterRequestSubmission() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		String code = registrationsMan.addInvitation(invitation);
		RegistrationRequest request = getRequest(code);
		
		registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
		
		List<InvitationWithCode> invitations = registrationsMan.getInvitations();
		assertThat(invitations.isEmpty(), is(true));
	}

	@Test
	public void invalidInvitationCodeIsNotAcceptedInRequest() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		registrationsMan.addInvitation(invitation);
		RegistrationRequest request = getRequest("invalid");
		
		catchException(registrationsMan)
			.submitRegistrationRequest(request, REG_CONTEXT);
		
		assertThat(caughtException(), allOf(
				isA(WrongArgumentException.class),
				hasMessageThat(containsString("code is invalid"))));
	}
	
	@Test
	public void expiredInvitationCodeIsNotAcceptedInRequest() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().minusMillis(1));
		String code = registrationsMan.addInvitation(invitation);
		RegistrationRequest request = getRequest(code);
		
		catchException(registrationsMan)
			.submitRegistrationRequest(request, REG_CONTEXT);
		
		assertThat(caughtException(), allOf(
				isA(WrongArgumentException.class),
				hasMessageThat(containsString("invitation"))));
	}

	@Test
	public void overridingMandatoryInvitationAttributeIsProhibited() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		invitation.getAttributes().put(0, new PrefilledEntry<>(
				new VerifiableEmailAttribute(InitializerCommon.EMAIL_ATTR, "/",
						AttributeVisibility.full, "enforced@example.com"), PrefilledEntryMode.HIDDEN));
		String code = registrationsMan.addInvitation(invitation);
		RegistrationRequest request = getRequest(code);
		
		catchException(registrationsMan)
			.submitRegistrationRequest(request, REG_CONTEXT);
	
		assertThat(caughtException(), allOf(
				isA(WrongArgumentException.class),
				hasMessageThat(containsString("invitation"))));
	}

	@Test
	public void overridingMandatoryInvitationIdentityIsProhibited() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		invitation.getIdentities().put(0, new PrefilledEntry<>(
				new IdentityParam(UsernameIdentity.ID, "some-user"), PrefilledEntryMode.READ_ONLY));
		String code = registrationsMan.addInvitation(invitation);
		RegistrationRequest request = getRequest(code);
		
		catchException(registrationsMan)
			.submitRegistrationRequest(request, REG_CONTEXT);
	
		assertThat(caughtException(), allOf(
				isA(WrongArgumentException.class),
				hasMessageThat(containsString("invitation"))));
	}
	
	@Test
	public void overridingMandatoryInvitationGroupIsProhibited() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		invitation.getGroupSelections().put(0, new PrefilledEntry<>(
				new Selection(false), PrefilledEntryMode.READ_ONLY));
		String code = registrationsMan.addInvitation(invitation);
		RegistrationRequest request = getRequest(code);
		
		catchException(registrationsMan)
			.submitRegistrationRequest(request, REG_CONTEXT);
	
		assertThat(caughtException(), allOf(
				isA(WrongArgumentException.class),
				hasMessageThat(containsString("invitation"))));
	}

	@Test
	public void mandatoryInvitationAttributeIsAdded() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		invitation.getAttributes().put(0, new PrefilledEntry<>(
				new VerifiableEmailAttribute(InitializerCommon.EMAIL_ATTR, "/",
						AttributeVisibility.full, "enforced@example.com"), PrefilledEntryMode.HIDDEN));
		String code = registrationsMan.addInvitation(invitation);
		RegistrationRequest request = getRequestWithIdentity(code);
		
		String requestId = registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
	
		RegistrationRequestState storedReq = registrationsMan.getRegistrationRequests().get(0);
		assertThat(storedReq.getRequestId(), is(requestId));
		assertThat(storedReq.getRequest().getAttributes().size(), is(1));
		assertThat(storedReq.getRequest().getAttributes().get(0).getValues().size(), is(1));
		assertThat(storedReq.getRequest().getAttributes().get(0).getValues().get(0), 
				is(new VerifiableEmail("enforced@example.com")));
	}

	@Test
	public void mandatoryInvitationIdentityIsAdded() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		invitation.getIdentities().put(0, new PrefilledEntry<>(
				new IdentityParam(UsernameIdentity.ID, "some-user"), PrefilledEntryMode.READ_ONLY));
		String code = registrationsMan.addInvitation(invitation);
		RegistrationRequest request = getEmptyRequest(code);
		
		String requestId = registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
	
		RegistrationRequestState storedReq = registrationsMan.getRegistrationRequests().get(0);
		assertThat(storedReq.getRequestId(), is(requestId));
		assertThat(storedReq.getRequest().getIdentities().size(), is(1));
		assertThat(storedReq.getRequest().getIdentities().get(0).getValue(), is("some-user"));
	}
	
	@Test
	public void mandatoryInvitationGroupIsAdded() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new InvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		invitation.getGroupSelections().put(0, new PrefilledEntry<>(
				new Selection(true), PrefilledEntryMode.READ_ONLY));
		String code = registrationsMan.addInvitation(invitation);
		RegistrationRequest request = getRequestWithIdentity(code);
		
		String requestId = registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
	
		RegistrationRequestState storedReq = registrationsMan.getRegistrationRequests().get(0);
		assertThat(storedReq.getRequestId(), is(requestId));
		assertThat(storedReq.getRequest().getGroupSelections().size(), is(1));
		assertThat(storedReq.getRequest().getGroupSelections().get(0), is(new Selection(true)));
	}

	
	private RegistrationForm initAndCreateForm(boolean nullCode) throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		groupsMan.addGroup(new Group("/A"));
		
		RegistrationForm form = getForm(nullCode);

		registrationsMan.addForm(form);
		return form;
	}
	
	private RegistrationForm getForm(boolean nullCode)
	{
		TranslationProfile translationProfile = new RegistrationTranslationProfileBuilder(
				regActionsRegistry, "form").build();
		return RegistrationFormBuilder
				.registrationForm()
				.withName(TEST_FORM)
				.withDescription("desc")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withTranslationProfile(translationProfile)
				.withAddedIdentityParam()
					.withIdentityType(UsernameIdentity.ID)
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endIdentityParam()
				.withAddedAttributeParam()
					.withAttributeType(InitializerCommon.EMAIL_ATTR).withGroup("/")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withOptional(true)
					.withShowGroups(true)
				.endAttributeParam()
				.withAddedGroupParam()
					.withGroupPath("/A")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam()
				.withRegistrationCode(nullCode ? null : "123")
				.withNotificationsConfiguration()
					.withInvitationTemplate("invitationWithCode")
				.endNotificationsConfiguration()
				.build();
	}
	
	private RegistrationRequest getRequest(String code)
	{
		return RegistrationRequestBuilder.registrationRequest()
				.withFormId(TEST_FORM)
				.withRegistrationCode(code)
				.withAddedAttribute(
						new VerifiableEmailAttribute(
								InitializerCommon.EMAIL_ATTR, "/",
								AttributeVisibility.full, "foo@example.com"))
				.withAddedGroupSelection().withSelected(false).endGroupSelection()
				.withAddedIdentity()
					.withTypeId(UsernameIdentity.ID)
					.withValue("invitedUser")
				.endIdentity()
				.build();
	}

	private RegistrationRequest getRequestWithIdentity(String code)
	{
		return RegistrationRequestBuilder.registrationRequest()
				.withFormId(TEST_FORM)
				.withRegistrationCode(code)
				.withAddedAttribute(null)
				.withAddedIdentity()
					.withTypeId(UsernameIdentity.ID)
					.withValue("invitedUser")
				.endIdentity()
				.withAddedGroupSelection(null)
				.build();
	}

	private RegistrationRequest getEmptyRequest(String code)
	{
		return RegistrationRequestBuilder.registrationRequest()
				.withFormId(TEST_FORM)
				.withRegistrationCode(code)
				.withAddedAttribute(null)
				.withAddedIdentity(null)
				.withAddedGroupSelection(null)
				.build();
	}

}
