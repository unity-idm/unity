/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.msgtemplates.confirm.EmailConfirmationTemplateDef;
import pl.edu.icm.unity.base.notifications.FacilityName;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.states.EmailAttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.EmailIdentityConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.UserEmailConfirmationState;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.builders.NotificationChannelBuilder;
import pl.edu.icm.unity.engine.confirmation.EmailConfirmationManagerImpl;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * 
 * @author P. Piernik
 * 
 */
public class TestEmailConfirmations extends DBIntegrationTestBase
{
	@Autowired
	private MessageTemplateManagement templateMan;
	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	private EmailConfirmationManagerImpl confirmationMan;
	@Autowired
	private InitializerCommon commonInitializer;
	@Autowired
	private UnityServerConfiguration mainConfig;

	@Test
	public void shouldNotPreserveConfirmationStateIfChangedByAdmin() throws Exception
	{
		setupMockAuthn();
		setupAdmin();

		Identity id = idsMan.addEntity(new IdentityParam(EmailIdentity.ID,
				"example1@ex.com"), "crMock", EntityState.valid, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		AttributeType atT = new AttributeType(InitializerCommon.EMAIL_ATTR,
				VerifiableEmailAttributeSyntax.ID);
		atT.setMaxElements(5);
		atT.setSelfModificable(true);
		aTypeMan.addAttributeType(atT);

		VerifiableEmail e1 = new VerifiableEmail("a@example.com",
				new ConfirmationInfo(true));
		VerifiableEmail e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(
				false));
		Attribute at1 = VerifiableEmailAttribute.of(
				InitializerCommon.EMAIL_ATTR, "/", e1, e2);
		attrsMan.createAttribute(entity, at1);

		AttributeExt returned = attrsMan
				.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertTrue(VerifiableEmail.fromJsonString(returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(VerifiableEmail.fromJsonString(returned.getValues().get(1)).isConfirmed());

		
		VerifiableEmail e1P = new VerifiableEmail("a@example.com", new ConfirmationInfo(false));
		VerifiableEmail e2P = new VerifiableEmail("b@example.com", new ConfirmationInfo(true));
		VerifiableEmail e3P = new VerifiableEmail("c@example.com", new ConfirmationInfo(true));
		
		Attribute at1P = VerifiableEmailAttribute.of(
				InitializerCommon.EMAIL_ATTR, "/", e1P,
				e2P, e3P);
		attrsMan.setAttribute(entity, at1P);

		AttributeExt returnedP = attrsMan
				.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertFalse(VerifiableEmail.fromJsonString(returnedP.getValues().get(0)).isConfirmed());
		Assert.assertTrue(VerifiableEmail.fromJsonString(returnedP.getValues().get(1)).isConfirmed());
		Assert.assertTrue(VerifiableEmail.fromJsonString(returnedP.getValues().get(2)).isConfirmed());
	}

	@Test
	public void shouldNotAddConfirmedAttributeIfAddedByUser() throws Exception
	{
		setupPasswordAuthn();
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam entity = new EntityParam(id.getEntityId());
		AttributeType atT = new AttributeType(InitializerCommon.EMAIL_ATTR,
				VerifiableEmailAttributeSyntax.ID);
		atT.setMaxElements(5);
		atT.setSelfModificable(true);
		aTypeMan.addAttributeType(atT);

		setupUserContext(DEF_USER, null);

		VerifiableEmail e1 = new VerifiableEmail("a@example.com", new ConfirmationInfo(true));
		VerifiableEmail e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		Attribute at1 = VerifiableEmailAttribute.of(
				InitializerCommon.EMAIL_ATTR, "/", e1, e2);
		attrsMan.createAttribute(entity, at1);

		AttributeExt returned = attrsMan
				.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertFalse(VerifiableEmail.fromJsonString(returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(VerifiableEmail.fromJsonString(returned.getValues().get(1)).isConfirmed());
	}

	@Test
	public void shouldPreserveOneConfirmationStateIfChangedByUser() throws Exception
	{
		setupPasswordAuthn();
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam entity = new EntityParam(id.getEntityId());
		AttributeType atT = new AttributeType(InitializerCommon.EMAIL_ATTR,
				VerifiableEmailAttributeSyntax.ID);
		atT.setMaxElements(5);
		atT.setSelfModificable(true);
		aTypeMan.addAttributeType(atT);

		setupAdmin();
		VerifiableEmail e1 = new VerifiableEmail("a@example.com", new ConfirmationInfo(true));
		VerifiableEmail e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		Attribute at1 = VerifiableEmailAttribute.of(
				InitializerCommon.EMAIL_ATTR, "/", e1, e2);
		attrsMan.setAttribute(entity, at1);

		AttributeExt returned = attrsMan
				.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertTrue(VerifiableEmail.fromJsonString(returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(VerifiableEmail.fromJsonString(returned.getValues().get(1)).isConfirmed());

		setupUserContext(DEF_USER, null);

		e1 = new VerifiableEmail("a@example.com", new ConfirmationInfo(false));
		e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(true));
		VerifiableEmail e3 = new VerifiableEmail("c@example.com",
				new ConfirmationInfo(true));
		at1 = VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/",
				e3, e2, e1);
		attrsMan.setAttribute(entity, at1);

		returned = attrsMan.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertFalse(VerifiableEmail.fromJsonString(returned.getValues().get(0)).isConfirmed()); // reset
		Assert.assertFalse(VerifiableEmail.fromJsonString(returned.getValues().get(1)).isConfirmed()); // preserved old, reset
		Assert.assertTrue(VerifiableEmail.fromJsonString(returned.getValues().get(2)).isConfirmed()); // preserved old, set

	}

	@Test
	public void shouldThrowExceptionIfUserRemoveLastConfirmedValue() throws Exception
	{
		setupPasswordAuthn();
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam entity = new EntityParam(id.getEntityId());
		AttributeType atT = new AttributeType(InitializerCommon.EMAIL_ATTR,
				VerifiableEmailAttributeSyntax.ID);
		atT.setMaxElements(5);
		atT.setSelfModificable(true);
		aTypeMan.addAttributeType(atT);

		setupAdmin();
		VerifiableEmail e1 = new VerifiableEmail("a@example.com",new ConfirmationInfo(true));
		VerifiableEmail e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		Attribute at1 = VerifiableEmailAttribute.of(
				InitializerCommon.EMAIL_ATTR, "/", e1, e2);
		attrsMan.setAttribute(entity, at1);

		AttributeExt returned = attrsMan
				.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertTrue(VerifiableEmail.fromJsonString(returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(VerifiableEmail.fromJsonString(returned.getValues().get(1)).isConfirmed());

		setupUserContext(DEF_USER, null);
		e1 = new VerifiableEmail("c@example.com", new ConfirmationInfo(false));
		e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		at1 = VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/",
				e1, e2);
		try
		{
			attrsMan.setAttribute(entity, at1);
			fail("Ordinary user managed to remove the last confirmed attribute value");
		} catch (IllegalAttributeValueException e)
		{
			// OK
		}
	}

	@Test
	public void shouldNotSendConfirmationRequestIfConfigurationIsEmpty() throws Exception
	{
		setupMockAuthn();
		groupsMan.addGroup(new Group("/test"));
		Identity id = idsMan.addEntity(new IdentityParam(EmailIdentity.ID,
				"example1@ex.com"), "crMock", EntityState.valid, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		groupsMan.addMemberFromParent("/test", entity);

		aTypeMan.addAttributeType(new AttributeType(InitializerCommon.EMAIL_ATTR,
				VerifiableEmailAttributeSyntax.ID));
		Attribute at1 = VerifiableEmailAttribute.of(
				InitializerCommon.EMAIL_ATTR, "/test", "example2@ex.com");
		attrsMan.createAttribute(entity, at1);
		EmailAttribiuteConfirmationState attrState = new EmailAttribiuteConfirmationState(
				entity.getEntityId(), InitializerCommon.EMAIL_ATTR,
				"example2@ex.com", "pl", "/test");

		confirmationMan.sendConfirmationRequest(attrState);

		VerifiableElement vemail = getFirstEmailAttributeValueFromEntity(entity, "/test");
		Assert.assertFalse(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
	}

	@Test
	public void checkAttributeConfirmationProcess() throws Exception
	{
		setupMockAuthn();
		groupsMan.addGroup(new Group("/test"));
		Identity id = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID,
				"username"), "crMock", EntityState.valid, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		groupsMan.addMemberFromParent("/test", entity);
		aTypeMan.addAttributeType(new AttributeType(InitializerCommon.EMAIL_ATTR,
				VerifiableEmailAttributeSyntax.ID));
		Attribute at1 = VerifiableEmailAttribute.of(
				InitializerCommon.EMAIL_ATTR, "/test", "example2@ex.com");
		attrsMan.createAttribute(entity, at1);
		addSimpleConfirmationConfiguration(
				true,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");

		Assert.assertEquals(0,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		
		EmailAttribiuteConfirmationState attrState = new EmailAttribiuteConfirmationState(
				entity.getEntityId(), InitializerCommon.EMAIL_ATTR,
				"example2@ex.com", "pl", "/test");
		try
		{
			confirmationMan.sendConfirmationRequest(attrState);
		} catch (Exception e)
		{
			fail("Cannot send confirmation request");
		}
		
		VerifiableElement vemail = getFirstEmailAttributeValueFromEntity(entity, "/test");
		Assert.assertFalse(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(1, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertEquals(1, tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();
		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			fail("Cannot proccess confirmation");
		}
		
		Assert.assertEquals(0, tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		vemail = getFirstEmailAttributeValueFromEntity(entity, "/test");
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());
	}

	@Test
	public void checkIdentityConfirmationProcess() throws Exception
	{
		setupMockAuthn();
		groupsMan.addGroup(new Group("/test"));
		Identity id = idsMan.addEntity(new IdentityParam(EmailIdentity.ID,
				"example1@ex.com"), "crMock", EntityState.valid, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		groupsMan.addMemberFromParent("/test", entity);
		addSimpleConfirmationConfiguration(
				false,
				EmailIdentity.ID, "demoTemplate", "demoChannel");
	
		Assert.assertEquals(0,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());

		EmailIdentityConfirmationState idState = new EmailIdentityConfirmationState(
				entity.getEntityId(), EmailIdentity.ID, "example1@ex.com", "en");
		try
		{
			confirmationMan.sendConfirmationRequest(idState);
		} catch (Exception e)
		{
			fail("Cannot send confirmation request");
		}
		VerifiableElement identity = getFirstEmailIdentityFromEntity(entity);
		Assert.assertFalse(identity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(1, identity.getConfirmationInfo().getSentRequestAmount());
		Assert.assertEquals(1,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();
		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			fail("Cannot proccess confirmation");
		}
		Assert.assertEquals(0,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		identity = getFirstEmailIdentityFromEntity(entity);
		Assert.assertTrue(identity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, identity.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, identity.getConfirmationInfo().getConfirmationDate());

	}

	@Test
	public void shouldConfirmAttributeInRegistrationForm() throws Exception
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));

		RegistrationForm form = getFormBuilder().build();
		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				true,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");

		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						VerifiableEmailAttribute.of(
								InitializerCommon.EMAIL_ATTR, "/",
								"test1@example.com"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abs").toJson()).endCredential()
				.withAddedGroupSelection().withGroup("/B").endGroupSelection()
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "username"))
				.build();

		registrationsMan.submitRegistrationRequest(request, new RegistrationContext( 
				false, TriggeringMode.manualAtLogin));
		Assert.assertEquals(1,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();

		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot proccess confirmation");
		}

		VerifiableElement vemail = getFirstEmailAttributeValueFromRegistration();
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());

	}

	@Test
	public void shouldConfirmIdentityInRegistrationForm() throws Exception
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		RegistrationForm form = new RegistrationFormBuilder()
				.withName("f1")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedIdentityParam()
					.withIdentityType(EmailIdentity.ID)
					.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.build();
		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				false,
				EmailIdentity.ID, "demoTemplate", "demoChannel");

		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withAddedIdentity(new IdentityParam(EmailIdentity.ID, "example@example.com"))
				.build();

		registrationsMan.submitRegistrationRequest(request, new RegistrationContext( 
				false, TriggeringMode.manualAtLogin));
		Assert.assertEquals(1,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();

		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot proccess confirmation");
		}

		VerifiableElement vemail = getFirstEmailIdentityFromRegistration();
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());

		Assert.assertEquals(0,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
	}

	@Test
	public void shouldPreserveConfirmedStateOfIdentity() throws Exception
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		RegistrationForm form = new RegistrationFormBuilder()
				.withName("f1")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedIdentityParam()
					.withIdentityType(EmailIdentity.ID)
					.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.build();
		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				false,
				EmailIdentity.ID, "demoTemplate", "demoChannel");

		IdentityParam confirmedIdentity = new IdentityParam(EmailIdentity.ID, "example@example.com");
		confirmedIdentity.setConfirmationInfo(new ConfirmationInfo(true));
		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withAddedIdentity(confirmedIdentity)
				.build();

		registrationsMan.submitRegistrationRequest(request, new RegistrationContext( 
				false, TriggeringMode.manualAtLogin));
		Assert.assertEquals(0,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());

		VerifiableElement vemail = getFirstEmailIdentityFromRegistration();
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());
	}

	@Test
	public void shouldPreserveConfirmedStateOfAttribute() throws Exception
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));

		RegistrationForm form = new RegistrationFormBuilder()
				.withName("f1")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedIdentityParam()
					.withIdentityType(UsernameIdentity.ID).withLabel("label")
					.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedAttributeParam()
					.withAttributeType(InitializerCommon.EMAIL_ATTR)
					.withGroup("/")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.endAttributeParam()
				.build();
		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				true,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");
		VerifiableEmail confirmedEmail = new VerifiableEmail("test1@example.com", new ConfirmationInfo(true));
		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withAddedAttribute(
						VerifiableEmailAttribute.of(
								InitializerCommon.EMAIL_ATTR, "/",
								confirmedEmail))
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "username"))
				.build();

		registrationsMan.submitRegistrationRequest(request, new RegistrationContext( 
				false, TriggeringMode.manualAtLogin));
		Assert.assertEquals(0,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());

		VerifiableElement vemail = getFirstEmailAttributeValueFromRegistration();
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());	
	}
	
	@Test
	public void shouldAutoAcceptRegistrationRequestAfterConfirmingAttribute()
			throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				new String[] {AutomaticRequestAction.accept.toString()});
		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, 
				Lists.newArrayList(new TranslationRule("attr[\"email\"].confirmed ==  true", a1)));
		RegistrationForm form = getFormBuilder().withTranslationProfile(tp).build();
		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				true,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");

		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						VerifiableEmailAttribute.of(
								InitializerCommon.EMAIL_ATTR, "/",
								"test2@example.com"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abs").toJson()).endCredential()
				.withAddedGroupSelection().withGroup("/B").endGroupSelection()
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "username"))
				.build();

		registrationsMan.submitRegistrationRequest(request, new RegistrationContext( 
				false, TriggeringMode.manualAtLogin));

		Assert.assertEquals(RegistrationRequestStatus.pending, registrationsMan
				.getRegistrationRequests().get(0).getStatus());

		Assert.assertEquals(1,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();

		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot proccess confirmation");
		}
		Assert.assertEquals(RegistrationRequestStatus.accepted, registrationsMan
				.getRegistrationRequests().get(0).getStatus());

	}

	@Test
	public void shouldRewriteTokenAfterConfirmRequest() throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		RegistrationForm form = new RegistrationFormBuilder()
				.withName("f1")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedIdentityParam()
					.withIdentityType(EmailIdentity.ID)
					.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedAttributeParam()
					.withAttributeType(InitializerCommon.EMAIL_ATTR)
					.withGroup("/")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endAttributeParam()
				.build();

		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				true,
				InitializerCommon.EMAIL_ATTR, "demoTemplate1", "demoChannel1");

		addSimpleConfirmationConfiguration(
				false,
				EmailIdentity.ID, "demoTemplate2", "demoChannel2");

		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withAddedAttribute(
						VerifiableEmailAttribute.of(
								InitializerCommon.EMAIL_ATTR, "/",
								"test3@example.com"))
				.withAddedIdentity(new IdentityParam(EmailIdentity.ID, "test33@example.com"))
				.build();

		String requestId = registrationsMan.submitRegistrationRequest(request, new RegistrationContext(
				false, TriggeringMode.manualAtLogin));

		Assert.assertEquals(2,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		Assert.assertEquals(RegistrationRequestStatus.pending, registrationsMan
				.getRegistrationRequests().get(0).getStatus());

		RegistrationRequestState requestState = registrationsMan.getRegistrationRequests()
				.get(0);
		registrationsMan.processRegistrationRequest(requestId, requestState.getRequest(),
				RegistrationRequestAction.accept, "", "");

		Assert.assertEquals(RegistrationRequestStatus.accepted, registrationsMan
				.getRegistrationRequests().get(0).getStatus());

		Assert.assertEquals(2,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());

		for (Token tk : tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE))
		{
			byte[] tokenContents = tk.getContents();
			try
			{
				UserEmailConfirmationState state = new UserEmailConfirmationState(new String(
						tokenContents, StandardCharsets.UTF_8));
				if (!(state.getFacilityId().equals(
						EmailAttribiuteConfirmationState.FACILITY_ID) || state
						.getFacilityId()
						.equals(EmailIdentityConfirmationState.FACILITY_ID)))
					fail("Invalid facility id in confirmation state");

			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Tokens content cannot be parsed as UserConfirmationState");
			}
		}
	}
	
	@Test
	public void shouldSkipProcessingOfRejectedRequest() throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				new String[] {AutomaticRequestAction.accept.toString()});
		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, 
				Lists.newArrayList(new TranslationRule("attr[\"email\"].confirmed == true", a1)));
		RegistrationForm form = getFormBuilder().withTranslationProfile(tp).build();
		
		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				true,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");

		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						VerifiableEmailAttribute.of(
								InitializerCommon.EMAIL_ATTR, "/",
								"test5@example.com"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abs").toJson()).endCredential()
				.withAddedGroupSelection().withGroup("/B").endGroupSelection()
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "username"))
				.build();

		String requestId = registrationsMan.submitRegistrationRequest(request, new RegistrationContext( 
				false, TriggeringMode.manualAtLogin));
		
		Assert.assertEquals(RegistrationRequestStatus.pending, registrationsMan
				.getRegistrationRequests().get(0).getStatus());

		Assert.assertEquals(1,
				tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();
		RegistrationRequestState requestState = registrationsMan.getRegistrationRequests()
				.get(0);
		registrationsMan.processRegistrationRequest(requestId, requestState.getRequest(),
				RegistrationRequestAction.reject, "", "");
		
		WorkflowFinalizationConfiguration status = null; 
		try
		{
			 status = confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot proccess confirmation");
		}
		
		Assert.assertFalse(status.success);
	}
	
	@Test
	public void shouldNotSendConfirmationRequestIfLimitExceeded() throws Exception
	{
		setupPasswordAuthn();
		
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam entity = new EntityParam(id.getEntityId());
		AttributeType atT = new AttributeType(InitializerCommon.EMAIL_ATTR,
				VerifiableEmailAttributeSyntax.ID);
		atT.setMaxElements(5);
		atT.setSelfModificable(true);
		aTypeMan.addAttributeType(atT);
		groupsMan.addGroup(new Group("/test"));
		groupsMan.addMemberFromParent("/test", entity);
		
		Attribute at1 = VerifiableEmailAttribute.of(
				InitializerCommon.EMAIL_ATTR, "/test", "test6@ex.com");
		addSimpleConfirmationConfiguration(
				true,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");	
		setupAdmin();
		for (int i = 0; i < mainConfig.getIntValue(UnityServerConfiguration.EMAIL_CONFIRMATION_REQUEST_LIMIT); i++)
		{
			at1.setValues(new VerifiableEmail("test6@ex.com", new ConfirmationInfo(false)).toJsonString());
			attrsMan.setAttribute(entity, at1);
			String token = tokensMan
					.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE)
					.get(0).getValue();
			try
			{
				confirmationMan.processConfirmation(token);
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Cannot proccess confirmation");
			}
			
		}	
		attrsMan.setAttribute(entity, at1);
		Assert.assertTrue(tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE).isEmpty());	
		
		Collection<AttributeExt>  attrs = attrsMan.getAttributes(entity, "/test", InitializerCommon.EMAIL_ATTR);
		VerifiableElement vElement = VerifiableEmail.fromJsonString(attrs.iterator().next().getValues().get(0));
		Assert.assertEquals(0, vElement.getConfirmationInfo().getSentRequestAmount());	
	}
	
	private void addSimpleConfirmationConfiguration(boolean toAttr, String name,
			String templateName, String channelName) throws EngineException
	{
		I18nMessage message = new I18nMessage(new I18nString("test"), new I18nString(
				"test ${" + EmailConfirmationTemplateDef.CONFIRMATION_LINK + "}"));
		notMan.addNotificationChannel(NotificationChannelBuilder.notificationChannel()
				.withName(channelName)
				.withConfiguration("test")
				.withDescription("test")
				.withFacilityId(FacilityName.EMAIL.toString())
				.build());
		
		
		templateMan.addTemplate(new MessageTemplate(templateName, "demo", message,
				EmailConfirmationTemplateDef.NAME, MessageType.PLAIN, channelName));
		
		EmailConfirmationConfiguration config = new EmailConfirmationConfiguration();
		config.setMessageTemplate(templateName);
		
		if (toAttr)
		{
			AttributeType type = aTypeMan.getAttributeType(name);
			VerifiableEmailAttributeSyntax syntax = new VerifiableEmailAttributeSyntax();
			syntax.setEmailConfirmationConfiguration(config);
			type.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
			aTypeMan.updateAttributeType(type);
		}else
		{
			IdentityType identityType = idTypeMan.getIdentityTypes().stream().filter(id -> id.getName().equals(name)).findFirst().get();
			identityType.setEmailConfirmationConfiguration(config);
			idTypeMan.updateIdentityType(identityType);
			
		}
	}
	
	private VerifiableElement getFirstEmailIdentityFromEntity(EntityParam entity)
			throws EngineException
	{
		Entity e = idsMan.getEntityNoContext(entity, "/test");
		return getIdentitiesByType(e.getIdentities(), EmailIdentity.ID).iterator().next();
	}

	private VerifiableElement getFirstEmailAttributeValueFromEntity(EntityParam entity,
			String group) throws EngineException
	{
		Collection<AttributeExt> allAttributes = attrsMan.getAllAttributes(entity,
				false, group, InitializerCommon.EMAIL_ATTR, false);
		AttributeExt attribute = allAttributes.iterator().next();
		VerifiableElement vemail = VerifiableEmail.fromJsonString(attribute.getValues().get(0));
		return vemail;
	}

	private VerifiableElement getFirstEmailAttributeValueFromRegistration() throws EngineException
	{
		RegistrationRequestState state = registrationsMan.getRegistrationRequests().get(0);
		return VerifiableEmail.fromJsonString(state.getRequest().getAttributes().get(0).getValues()
				.get(0));
	}

	private VerifiableElement getFirstEmailIdentityFromRegistration() throws EngineException
	{
		RegistrationRequestState state = registrationsMan.getRegistrationRequests().get(0);
		return state.getRequest().getIdentities().get(0);
	}
	
	private RegistrationFormBuilder getFormBuilder()
	{
		return new RegistrationFormBuilder()
		.withName("f1")
		.withDescription("description")
		.withPubliclyAvailable(true)
		.withDefaultCredentialRequirement(
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
		.withRegistrationCode("123")
		.withCollectComments(true)
		.withFormInformation(new I18nString("formInformation"))
		.withAddedCredentialParam(new CredentialRegistrationParam(EngineInitialization.DEFAULT_CREDENTIAL,
						"label", "description"))
		.withAddedAgreement(new AgreementRegistrationParam(new I18nString("a"), false))
		.withAddedIdentityParam()
			.withDescription("description")
			.withIdentityType(UsernameIdentity.ID).withLabel("label")
			.withOptional(true)
			.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
		.endIdentityParam()
		.withAddedAttributeParam()
			.withAttributeType(InitializerCommon.EMAIL_ATTR)
			.withGroup("/")
			.withDescription("description")
			.withLabel("label")
			.withOptional(true)
			.withRetrievalSettings(ParameterRetrievalSettings.interactive)
			.withShowGroups(true)
		.endAttributeParam()
		.withAddedGroupParam()
			.withDescription("description")
			.withGroupPath("/B").withLabel("label")
			.withRetrievalSettings(ParameterRetrievalSettings.automatic)
		.endGroupParam();
	}
}
