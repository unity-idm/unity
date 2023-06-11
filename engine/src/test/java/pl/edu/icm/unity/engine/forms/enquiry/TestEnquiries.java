/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.Entity;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.EntityState;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponseBuilder;
import pl.edu.icm.unity.base.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.AttributesAssertion;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector;
import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector.AccessMode;
import pl.edu.icm.unity.engine.api.enquiry.EnquirySelector.Type;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;

public class TestEnquiries extends DBIntegrationTestBase
{
	@Autowired
	private InitializerCommon commonInitializer;
	
	@Autowired
	private EnquiryManagement enquiryManagement;


	@Before
	public void init() throws EngineException
	{
		setupPasswordAuthn();
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
	}

	
	@Test 
	public void addedEnquiryIsReturned() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		
		List<EnquiryForm> forms = enquiryManagement.getEnquires();
		
		assertThat(forms.size(), is(1));
		assertThat(forms.get(0), is(form));
	}
	
	@Test 
	public void removedFormIsNotReturned() throws Exception
	{
		initAndCreateEnquiry(null);
		
		enquiryManagement.removeEnquiry("f1", true);
		
		assertThat(enquiryManagement.getEnquires().isEmpty(), is(true));
	}

	@Test 
	public void missingFormCantBeRemoved() throws Exception
	{
		Throwable error = catchThrowable(() -> enquiryManagement.removeEnquiry("missing", true));
		
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test 
	public void formWithDuplicateNameCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		
		Throwable error = catchThrowable(() -> enquiryManagement.addEnquiry(form));
		
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test 
	public void formWithMissingAttributeCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);

		AttributeRegistrationParam attrReg = form.getAttributeParams().get(0);
		attrReg.setAttributeType("missing");
		testFormBuilder.withAttributeParams(Collections.singletonList(attrReg));
		
		checkUpdateOrAdd(testFormBuilder.build(), "attr(2)", IllegalArgumentException.class);
	}
	
	@Test 
	public void formWithMissingCredentialCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);

		CredentialRegistrationParam credParam = form.getCredentialParams().get(0);
		credParam.setCredentialName("missing");
		testFormBuilder.withCredentialParams(Collections.singletonList(credParam));
		
		checkUpdateOrAdd(testFormBuilder.build(), "cred", IllegalArgumentException.class);
	}

	@Test 
	public void formWithMissingIdentityCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);
		IdentityRegistrationParam idParam = form.getIdentityParams().get(0);
		idParam.setIdentityType("missing");
		testFormBuilder.withIdentityParams(Collections.singletonList(idParam));
		checkUpdateOrAdd(testFormBuilder.build(), "id", IllegalArgumentException.class);
	}

	@Test
	public void artefactsPresentInFormCantBeRemoved() throws Exception
	{
		initAndCreateEnquiry(null);
		
		Throwable error1 = catchThrowable(() -> aTypeMan.removeAttributeType(InitializerCommon.EMAIL_ATTR, true));
		assertThat(error1).isInstanceOf(IllegalArgumentException.class);

		Throwable error2 = catchThrowable(() -> groupsMan.removeGroup("/B", true));
		assertThat(error2).isInstanceOf(IllegalArgumentException.class);

		Throwable error3 = catchThrowable(() -> credMan.removeCredentialDefinition(EngineInitialization.DEFAULT_CREDENTIAL));
		assertThat(error3).isInstanceOf(IllegalArgumentException.class);
	}
	
	
	@Test
	public void requestWithoutOptionalFieldsIsAccepted() throws Exception
	{
		initAndCreateEnquiry("true");
		EnquiryResponse response = new EnquiryResponseBuilder()
			.withFormId("f1")
			.withComments("comments")
			.withAddedAgreement()
				.withSelected(true)
			.endAgreement()
			.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abc").toJson())
			.endCredential()
			.withAddedAttribute(null)
			.withAddedIdentity(null)
			.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "test-user"))
			.withAddedGroupSelection(null)
			.build();
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid);
		
		setupUserContext("tuser", null);
		enquiryManagement.submitEnquiryResponse(response, new RegistrationContext(false, 
				TriggeringMode.manualStandalone));
		setupAdmin();
		
		
		Entity entity = idsMan.getEntity(new EntityParam(new IdentityParam(UsernameIdentity.ID, "test-user")));
		Collection<Identity> usernames = getIdentitiesByType(entity.getIdentities(), UsernameIdentity.ID);
		assertThat(usernames.size(), is(2));
		assertThat(usernames, hasItem(identity));
	}

	@Test
	public void matchingOnlyByTargetGroupEnquiryIsPendingForRegularUser() throws Exception
	{
		Identity identity = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam entityParam = new EntityParam(identity);
		groupsMan.addMemberFromParent("/A", entityParam);
		EnquiryForm form = new EnquiryFormBuilder()
			.withTargetGroups(new String[] {"/A"})
			.withType(EnquiryType.REQUESTED_OPTIONAL)
			.withName("tenquiry")
			.build();
		enquiryManagement.addEnquiry(form);
		
		setupUserContext(DEF_USER, null);
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getAvailableEnquires(entityParam, EnquirySelector.builder()
				.withAccessMode(AccessMode.NOT_BY_INVITATION_ONLY)
				.withType(Type.REGULAR)
				.build());
		
		assertThat(pendingEnquires.size(), is(1));
		assertThat(pendingEnquires.get(0), is(form));
	}
	
	@Test
	public void matchingByTargetConditionEnquiryIsPendingForRegularUser() throws Exception
	{
		Identity identity = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam entityParam = new EntityParam(identity);
		EnquiryForm form = new EnquiryFormBuilder().withTargetGroups(new String[] { "/" })
				.withTargetCondition("status == \"valid\" && credReq == \"" + CRED_REQ_PASS + "\"")
				.withType(EnquiryType.REQUESTED_OPTIONAL).withName("tenquiry").build();
		enquiryManagement.addEnquiry(form);
		
		setupUserContext(DEF_USER, null);
		List<EnquiryForm> pendingEnquires = enquiryManagement.getAvailableEnquires(entityParam, EnquirySelector.builder()
				.withAccessMode(AccessMode.NOT_BY_INVITATION_ONLY)
				.withType(Type.REGULAR)
				.build());

		assertThat(pendingEnquires.size(), is(1));
		assertThat(pendingEnquires.get(0), is(form));
	}
	
	@Test
	public void notMatchingByTargetConditionEnquiryIsNotReturnedForRegularUser() throws Exception
	{
		Identity identity = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam entityParam = new EntityParam(identity);
		groupsMan.addMemberFromParent("/A", entityParam);	
		EnquiryForm form = new EnquiryFormBuilder().withTargetGroups(new String[] { "/" })
				.withTargetCondition("!(groups contains \"/A\")")
				.withType(EnquiryType.REQUESTED_OPTIONAL).withName("tenquiry").build();
		enquiryManagement.addEnquiry(form);
	
		setupUserContext(DEF_USER, null);
		List<EnquiryForm> pendingEnquires = enquiryManagement.getAvailableEnquires(entityParam, EnquirySelector.builder()
				.withAccessMode(AccessMode.NOT_BY_INVITATION_ONLY)
				.withType(Type.REGULAR)
				.build());
		assertThat(pendingEnquires.size(), is(0));	
	}
	
	@Test
	public void byInvitationEnquiryIsNotReturned() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid);
		EntityParam entityParam = new EntityParam(identity);
		groupsMan.addMemberFromParent("/A", entityParam);
		EnquiryForm form = new EnquiryFormBuilder()
			.withTargetGroups(new String[] {"/A"})
			.withType(EnquiryType.REQUESTED_OPTIONAL)
			.withName("invenquiry")
			.withByInvitationOnly(true)
			.build();
		enquiryManagement.addEnquiry(form);
		
		EnquiryForm form2 = new EnquiryFormBuilder()
				.withTargetGroups(new String[] {"/A"})
				.withType(EnquiryType.REQUESTED_OPTIONAL)
				.withName("nenquiry")
				.withByInvitationOnly(false)
				.build();
			enquiryManagement.addEnquiry(form2);
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getAvailableEnquires(entityParam, EnquirySelector.builder()
				.withAccessMode(AccessMode.NOT_BY_INVITATION_ONLY)
				.withType(Type.REGULAR)
				.build());
		
		assertThat(pendingEnquires.size(), is(1));
		assertThat(pendingEnquires.get(0), is(form2));
	}
	
	@Test
	public void byInvitationOrStickyEnquiryIsAlsoReturned() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid);
		EntityParam entityParam = new EntityParam(identity);
		groupsMan.addMemberFromParent("/A", entityParam);
		
		EnquiryForm form = new EnquiryFormBuilder()
			.withTargetGroups(new String[] {"/A"})
			.withType(EnquiryType.REQUESTED_OPTIONAL)
			.withName("invenquiry")
			.withByInvitationOnly(true)
			.build();
		enquiryManagement.addEnquiry(form);
		
		EnquiryForm form2 = new EnquiryFormBuilder()
				.withTargetGroups(new String[] {"/A"})
				.withType(EnquiryType.STICKY)
				.withName("nenquiry")
				.withByInvitationOnly(false)
				.build();
			enquiryManagement.addEnquiry(form2);
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getAvailableEnquires(entityParam, EnquirySelector.builder()
				.withType(Type.ALL).withAccessMode(AccessMode.ANY).build());
		
		assertThat(pendingEnquires.size(), is(2));
	}

	@Test
	public void enquiryForDifferentGroupIsNotPending() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid);
		EntityParam entityParam = new EntityParam(identity);
		EnquiryForm form = new EnquiryFormBuilder()
			.withTargetGroups(new String[] {"/A"})
			.withType(EnquiryType.REQUESTED_OPTIONAL)
			.withName("tenquiry")
			.build();
		enquiryManagement.addEnquiry(form);
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getAvailableEnquires(entityParam, EnquirySelector.builder()
				.withAccessMode(AccessMode.NOT_BY_INVITATION_ONLY)
				.withType(Type.REGULAR)
				.build());
		
		assertThat(pendingEnquires.isEmpty(), is(true));
	}
	
	@Test
	public void submittedEnquiryIsNotPendingAnymore() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid);
		EntityParam entityParam = new EntityParam(identity);
		EnquiryForm form = new EnquiryFormBuilder()
			.withTargetGroups(new String[] {"/"})
			.withType(EnquiryType.REQUESTED_OPTIONAL)
			.withName("enquiry1")
			.build();
		enquiryManagement.addEnquiry(form);

		setupUserContext("tuser", null);
		EnquiryResponse response = new EnquiryResponseBuilder()
			.withFormId("enquiry1")
			.build();
		enquiryManagement.submitEnquiryResponse(response, 
				new RegistrationContext(false, TriggeringMode.manualStandalone));
		setupAdmin();
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getAvailableEnquires(entityParam, EnquirySelector.builder()
				.withAccessMode(AccessMode.NOT_BY_INVITATION_ONLY)
				.withType(Type.REGULAR)
				.build());
		
		assertThat(pendingEnquires.isEmpty(), is(true));
	}
	
	@Test
	public void ignoredEnquiryIsNotPendingAnymore() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid);
		EntityParam entityParam = new EntityParam(identity);
		EnquiryForm form = new EnquiryFormBuilder()
			.withName("e1")
			.withType(EnquiryType.REQUESTED_OPTIONAL)
			.withTargetGroups(new String[] {"/"})
			.build();
		enquiryManagement.addEnquiry(form);
		enquiryManagement.ignoreEnquiry("e1", entityParam);
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getAvailableEnquires(entityParam, EnquirySelector.builder()
				.withAccessMode(AccessMode.NOT_BY_INVITATION_ONLY)
				.withType(Type.REGULAR)
				.build());
		
		assertThat(pendingEnquires.isEmpty(), is(true));
	}
	
	@Test
	public void mandatoryEnquiryCanNotBeIgnored() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid);
		EntityParam entityParam = new EntityParam(identity);
		EnquiryForm form = new EnquiryFormBuilder()
			.withName("e1")
			.withTargetGroups(new String[] {"/"})
			.withType(EnquiryType.REQUESTED_MANDATORY)
			.build();
		enquiryManagement.addEnquiry(form);
		
		Throwable error = catchThrowable(() -> enquiryManagement.ignoreEnquiry("e1", entityParam));
		
		assertThat(error).isInstanceOf(WrongArgumentException.class);
	}

	@Test 
	public void enquiryWithoutIdentityIsAccepted() throws Exception
	{
		EnquiryForm form = new EnquiryFormBuilder()
				.withName("f1")
				.withTargetGroups(new String[] {"/"})
				.withType(EnquiryType.REQUESTED_MANDATORY)
				.withAddedAttributeParam()
				.withAttributeType(InitializerCommon.CN_ATTR).withGroup("/")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endAttributeParam()
				.build();
		
		enquiryManagement.addEnquiry(form);
		
		EnquiryResponse response = new EnquiryResponseBuilder()
				.withFormId("f1")
				.withAddedAttribute(StringAttribute.of(InitializerCommon.CN_ATTR, 
						"/", "some"))
				.build();
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid);
		
		setupUserContext("tuser", null);
		
		String id = enquiryManagement.submitEnquiryResponse(response, 
					new RegistrationContext(false, 
							TriggeringMode.manualStandalone));
		
		setupAdmin();
			
		enquiryManagement.processEnquiryResponse(id, response, 
				RegistrationRequestAction.accept, "", "");
		
		Collection<AttributeExt> attributes = attrsMan.getAttributes(
				new EntityParam(identity.getEntityId()), "/", 
				InitializerCommon.CN_ATTR);
		
		assertThat(attributes.size(), is(1));
		assertThat(attributes.iterator().next().getName(), is(InitializerCommon.CN_ATTR));
		assertThat(attributes.iterator().next().getValues().get(0), is("some"));
	}
	
	private EnquiryFormBuilder getFormBuilder(String autoAcceptCondition)
	{
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				new String[] {AutomaticRequestAction.accept.toString()});
		TranslationAction a2 = new TranslationAction(AddToGroupActionFactory.NAME, 
				new String[] {"'/A'"});
		TranslationAction a3 = new TranslationAction(AddAttributeActionFactory.NAME, 
				new String[] {"cn", "/", "'val'"});
		
		String autoAcceptCnd = autoAcceptCondition == null ? "false" : autoAcceptCondition;
		
		List<TranslationRule> rules = Lists.newArrayList(new TranslationRule(autoAcceptCnd, a1),
				new TranslationRule("true", a2),
				new TranslationRule("true", a3));
		
		TranslationProfile translationProfile = new TranslationProfile("form", "", 
				ProfileType.REGISTRATION, rules);
		
		return new EnquiryFormBuilder()
				.withName("f1")
				.withDescription("desc")
				.withTranslationProfile(translationProfile)
				.withTargetGroups(new String[] {"/"})
				.withType(EnquiryType.REQUESTED_MANDATORY)
				.withCollectComments(true)
				.withFormInformation(new I18nString("formInformation"))
				.withAddedCredentialParam(new CredentialRegistrationParam(EngineInitialization.DEFAULT_CREDENTIAL))
				.withAddedAgreement(new AgreementRegistrationParam(new I18nString("a"), false))
				.withAddedIdentityParam()
				.withIdentityType(X500Identity.ID)
				.withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedIdentityParam()
				.withIdentityType(UsernameIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedAttributeParam()
				.withAttributeType(InitializerCommon.EMAIL_ATTR).withGroup("/")
				.withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.withShowGroups(true).endAttributeParam()
				.withAddedGroupParam()
				.withGroupPath("/B")
				.withRetrievalSettings(ParameterRetrievalSettings.automatic)
				.endGroupParam();
	}
	
	@Test
	public void shouldAddAttributeCollectedInEnquiry() throws Exception
	{
		// given
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				new String[] {AutomaticRequestAction.accept.toString()});
		List<TranslationRule> rules = Lists.newArrayList(new TranslationRule("true", a1));
		TranslationProfile translationProfile = new TranslationProfile("form", "", 
				ProfileType.REGISTRATION, rules);
		EnquiryForm form = new EnquiryFormBuilder()
				.withName("enquiry1")
				.withTargetGroups(new String[] {"/A"})
				.withTranslationProfile(translationProfile)
				.withType(EnquiryType.REQUESTED_OPTIONAL)
				.withAddedAttributeParam()
					.withAttributeType(InitializerCommon.CN_ATTR).withGroup("/A")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endAttributeParam()
				.withAddedGroupParam()
					.withGroupPath("/A")
					.withRetrievalSettings(ParameterRetrievalSettings.automatic)
				.endGroupParam()
				.build();
		enquiryManagement.addEnquiry(form);
		
		createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam ep = new EntityParam(new IdentityTaV(UsernameIdentity.ID, DEF_USER));
		groupsMan.addMemberFromParent("/A", ep);
		setupUserContext(DEF_USER, null);
		
		EnquiryResponse response = new EnquiryResponseBuilder()
				.withFormId("enquiry1")
				.withAddedAttribute(StringAttribute.of(InitializerCommon.CN_ATTR, 
						"/A", "some"))
				.withAddedGroupSelection(null)
				.build();
		
		// when
		setupUserContext(DEF_USER, null);
		enquiryManagement.submitEnquiryResponse(response, 
					new RegistrationContext(false, TriggeringMode.manualStandalone));
		
		// then
		AttributesAssertion.assertThat(attrsMan, UsernameIdentity.ID, DEF_USER)
				.hasAttribute(InitializerCommon.CN_ATTR, "/A").count(1).attr(0).hasValues("some");
	}
	
	private void checkUpdateOrAdd(EnquiryForm form, String msg, Class<?> exception) throws EngineException
	{
		try
		{
			enquiryManagement.addEnquiry(form);
			fail("Added the form with illegal " + msg);
		} catch (Exception e) 
		{
			assertTrue(e.toString(), e.getClass().isAssignableFrom(exception));
		}
		try
		{
			enquiryManagement.updateEnquiry(form, true);
			fail("Updated the form with illegal " + msg);
		} catch (Exception e) 
		{
			assertTrue(e.toString(), e.getClass().isAssignableFrom(exception));
		}
	}
	
	private EnquiryForm initAndCreateEnquiry(String autoAcceptCondition) throws EngineException
	{
		EnquiryForm form = getFormBuilder(autoAcceptCondition).build();
		enquiryManagement.addEnquiry(form);
		return form;
	}
}
