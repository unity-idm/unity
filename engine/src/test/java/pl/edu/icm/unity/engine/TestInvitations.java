/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.notifications.NotificationChannel;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.mock.MockNotificationFacility;
import pl.edu.icm.unity.engine.mock.MockNotificationFacility.Message;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class TestInvitations  extends DBIntegrationTestBase
{
	public static final String TEST_FORM = "form-1";
	
	private static final RegistrationContext REG_CONTEXT = new RegistrationContext(
			false, TriggeringMode.manualStandalone);
	
	@Autowired
	private InvitationManagement invitationMan;
	
	@Autowired
	private InitializerCommon commonInitializer;
	
	@Autowired
	private MockNotificationFacility mockNotificationFacility;
	
	@Test
	public void addedInvitationIsReturned() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100).
				truncatedTo(ChronoUnit.SECONDS));
		String code = invitationMan.addInvitation(invitation);
		InvitationWithCode invitationEnh = new InvitationWithCode(invitation, code, null, 0);
		
		List<InvitationWithCode> invitations = invitationMan.getInvitations();
		assertThat(invitations).hasSize(1);
		assertThat(invitations.get(0)).isEqualTo(invitationEnh);
	}

	@Test
	public void removedInvitationIsNotReturned() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		String code = invitationMan.addInvitation(invitation);
		
		invitationMan.removeInvitation(code);
		
		List<InvitationWithCode> invitations = invitationMan.getInvitations();
		assertThat(invitations).isEmpty();
	}
	
	@Test
	public void invitationIsSentWhenRequested() throws Exception
	{
		initAndCreateForm(true);
		notMan.removeNotificationChannel(UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL);
		notMan.addNotificationChannel(new NotificationChannel(UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL, 
				"", "", MockNotificationFacility.NAME));
		InvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100), 
				"someAddr");
		String code = invitationMan.addInvitation(invitation);
		mockNotificationFacility.resetSent();
		
		invitationMan.sendInvitation(code);
		
		List<Message> sent = mockNotificationFacility.getSent();
		assertThat(sent).hasSize(1);
		assertThat(sent.get(0).address).isEqualTo("someAddr");
		assertThat(sent.get(0).subject).isEqualTo("Registration invitation");
		
		InvitationWithCode invitation2 = invitationMan.getInvitation(code);
		assertThat(invitation2.getLastSentTime()).isNotNull();
		assertThat(invitation2.getNumberOfSends()).isEqualTo(1);
	}

	@Test
	public void invitationIsDisposedAfterRequestSubmission() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		String code = invitationMan.addInvitation(invitation);
		RegistrationRequest request = getRequest(code);
		
		registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
		
		List<InvitationWithCode> invitations = invitationMan.getInvitations();
		assertThat(invitations).isEmpty();
	}

	@Test
	public void invalidInvitationCodeIsNotAcceptedInRequest() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		invitationMan.addInvitation(invitation);
		RegistrationRequest request = getRequest("invalid");
		
		Throwable error = catchThrowable(() -> registrationsMan
			.submitRegistrationRequest(request, REG_CONTEXT));
		
		assertThat(error).isInstanceOf(WrongArgumentException.class)
			.hasMessageContaining("code is invalid");
	}
	
	@Test
	public void expiredInvitationCodeIsNotAcceptedInRequest() throws Exception
	{
		initAndCreateForm(true);
		InvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().minusMillis(1));
		String code = invitationMan.addInvitation(invitation);
		RegistrationRequest request = getRequest(code);
		
		Throwable error = catchThrowable(() -> registrationsMan
			.submitRegistrationRequest(request, REG_CONTEXT));
		
		assertThat(error).isInstanceOf(WrongArgumentException.class)
				.hasMessageContaining("invitation");
	}

	@Test
	public void mandatoryInvitationAttributeOverwritesRequestedAttribute() throws Exception
	{
		initAndCreateForm(true);
		RegistrationInvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		Attribute enforcedAttribute = VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/",
				"enforced@example.com");
		invitation.getFormPrefill().getAttributes().put(0, new PrefilledEntry<>(enforcedAttribute, PrefilledEntryMode.HIDDEN));
		String code = invitationMan.addInvitation(invitation);
		RegistrationRequest request = getRequest(code);
		
		registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
	
		RegistrationRequestState storedReq = registrationsMan.getRegistrationRequests().get(0);
		assertThat(storedReq.getRequest().getAttributes()).hasSize(1);
		assertThat(storedReq.getRequest().getAttributes().get(0)).isEqualTo(enforcedAttribute);
	}

	@Test
	public void mandatoryInvitationIdentityOverwritesRequestedIdentity() throws Exception
	{
		initAndCreateForm(true);
		RegistrationInvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		IdentityParam enforced = new IdentityParam(UsernameIdentity.ID, "some-user");
		invitation.getFormPrefill().getIdentities().put(0, new PrefilledEntry<>(enforced, PrefilledEntryMode.READ_ONLY));
		String code = invitationMan.addInvitation(invitation);
		RegistrationRequest request = getRequest(code);
		
		registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
	
		RegistrationRequestState storedReq = registrationsMan.getRegistrationRequests().get(0);
		assertThat(storedReq.getRequest().getIdentities()).hasSize(1);
		assertThat(storedReq.getRequest().getIdentities().get(0)).isEqualTo(enforced);
	}
	
	@Test
	public void mandatoryInvitationGroupOverridesRequested() throws Exception
	{
		initAndCreateForm(true);
		RegistrationInvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		GroupSelection enforced = new GroupSelection("/A");
		invitation.getFormPrefill().getGroupSelections().put(0, new PrefilledEntry<>(enforced, PrefilledEntryMode.READ_ONLY));
		String code = invitationMan.addInvitation(invitation);
		RegistrationRequest request = getRequest(code);
		
		registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
	
		RegistrationRequestState storedReq = registrationsMan.getRegistrationRequests().get(0);
		assertThat(storedReq.getRequest().getGroupSelections()).hasSize(1);
		assertThat(storedReq.getRequest().getGroupSelections().get(0)).isEqualTo(enforced);
	}

	@Test
	public void mandatoryInvitationAttributeIsAdded() throws Exception
	{
		initAndCreateForm(true);
		RegistrationInvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		invitation.getFormPrefill().getAttributes().put(0, new PrefilledEntry<>(
				VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/",
						"enforced@example.com"), PrefilledEntryMode.HIDDEN));
		String code = invitationMan.addInvitation(invitation);
		RegistrationRequest request = getRequestWithIdentity(code);
		
		String requestId = registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
	
		RegistrationRequestState storedReq = registrationsMan.getRegistrationRequests().get(0);
		assertThat(storedReq.getRequestId()).isEqualTo(requestId);
		assertThat(storedReq.getRequest().getAttributes()).hasSize(1);
		assertThat(storedReq.getRequest().getAttributes().get(0).getValues()).hasSize(1);
		assertThat(storedReq.getRequest().getAttributes().get(0).getValues().get(0)). 
				isEqualTo(new VerifiableEmail("enforced@example.com").toJsonString());
	}

	@Test
	public void mandatoryInvitationIdentityIsAdded() throws Exception
	{
		initAndCreateForm(true);
		RegistrationInvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		invitation.getFormPrefill().getIdentities().put(0, new PrefilledEntry<>(
				new IdentityParam(UsernameIdentity.ID, "some-user"), PrefilledEntryMode.READ_ONLY));
		String code = invitationMan.addInvitation(invitation);
		RegistrationRequest request = getEmptyRequest(code);
		
		String requestId = registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
	
		RegistrationRequestState storedReq = registrationsMan.getRegistrationRequests().get(0);
		assertThat(storedReq.getRequestId()).isEqualTo(requestId);
		assertThat(storedReq.getRequest().getIdentities()).hasSize(1);
		assertThat(storedReq.getRequest().getIdentities().get(0).getValue()).isEqualTo("some-user");
	}
	
	@Test
	public void mandatoryInvitationGroupIsAdded() throws Exception
	{
		initAndCreateForm(true);
		RegistrationInvitationParam invitation = new RegistrationInvitationParam(TEST_FORM, Instant.now().plusSeconds(100));
		invitation.getFormPrefill().getGroupSelections().put(0, new PrefilledEntry<>(
				new GroupSelection("/A"), PrefilledEntryMode.READ_ONLY));
		String code = invitationMan.addInvitation(invitation);
		RegistrationRequest request = getRequestWithIdentity(code);
		
		String requestId = registrationsMan.submitRegistrationRequest(request, REG_CONTEXT);
	
		RegistrationRequestState storedReq = registrationsMan.getRegistrationRequests().get(0);
		assertThat(storedReq.getRequestId()).isEqualTo(requestId);
		assertThat(storedReq.getRequest().getGroupSelections()).hasSize(1);
		assertThat(storedReq.getRequest().getGroupSelections().get(0)).isEqualTo(new GroupSelection("/A"));
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
		return new RegistrationFormBuilder()
				.withName(TEST_FORM)
				.withDescription("desc")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withTranslationProfile(new TranslationProfile("", "", ProfileType.REGISTRATION, 
						Collections.emptyList()))
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
		return new RegistrationRequestBuilder()
				.withFormId(TEST_FORM)
				.withRegistrationCode(code)
				.withAddedAttribute(
						VerifiableEmailAttribute.of(
								InitializerCommon.EMAIL_ATTR, "/",
								"foo@example.com"))
				.withAddedGroupSelection().withGroup("/A").endGroupSelection()
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "invitedUser"))
				.build();
	}

	private RegistrationRequest getRequestWithIdentity(String code)
	{
		return new RegistrationRequestBuilder()
				.withFormId(TEST_FORM)
				.withRegistrationCode(code)
				.withAddedAttribute(null)
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "invitedUser"))
				.withAddedGroupSelection(null)
				.build();
	}

	private RegistrationRequest getEmptyRequest(String code)
	{
		return new RegistrationRequestBuilder()
				.withFormId(TEST_FORM)
				.withRegistrationCode(code)
				.withAddedAttribute(null)
				.withAddedIdentity(null)
				.withAddedGroupSelection(null)
				.build();
	}

}
