/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.UserConfirmationState;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.builders.ConfirmationConfigurationBuilder;
import pl.edu.icm.unity.engine.builders.NotificationChannelBuilder;
import pl.edu.icm.unity.engine.builders.RegistrationFormBuilder;
import pl.edu.icm.unity.engine.builders.RegistrationRequestBuilder;
import pl.edu.icm.unity.engine.confirmations.ConfirmationManagerImpl;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.I18nMessage;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

/**
 * 
 * @author P. Piernik
 * 
 */
public class TestConfirmations extends DBIntegrationTestBase
{
	@Autowired
	MessageTemplateManagement templateMan;
	@Autowired
	NotificationsManagement notificationsMan;
	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	private ConfirmationConfigurationManagement configurationMan;
	@Autowired
	private ConfirmationManagerImpl confirmationMan;
	@Autowired
	private InitializerCommon commonInitializer;
	@Autowired
	private UnityServerConfiguration mainConfig;

	@Test
	public void shouldNotPreservedConfirmationStateIfChangedByAdmin() throws Exception
	{
		setupMockAuthn();
		setupAdmin();

		Identity id = idsMan.addEntity(new IdentityParam(EmailIdentity.ID,
				"example1@ex.com"), "crMock", EntityState.valid, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		AttributeType atT = new AttributeType(InitializerCommon.EMAIL_ATTR,
				new VerifiableEmailAttributeSyntax());
		atT.setMaxElements(5);
		atT.setSelfModificable(true);
		attrsMan.addAttributeType(atT);

		VerifiableEmail e1 = new VerifiableEmail("a@example.com",
				new ConfirmationInfo(true));
		VerifiableEmail e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(
				false));
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/", AttributeVisibility.full, e1, e2);
		attrsMan.setAttribute(entity, at1, false);

		AttributeExt<?> returned = attrsMan
				.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertTrue(((VerifiableEmail) returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(((VerifiableEmail) returned.getValues().get(1)).isConfirmed());

		
		VerifiableEmail e1P = new VerifiableEmail("a@example.com", new ConfirmationInfo(false));
		VerifiableEmail e2P = new VerifiableEmail("b@example.com", new ConfirmationInfo(true));
		VerifiableEmail e3P = new VerifiableEmail("c@example.com", new ConfirmationInfo(true));
		
		VerifiableEmailAttribute at1P = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/", AttributeVisibility.full, e1P,
				e2P, e3P);
		attrsMan.setAttribute(entity, at1P, true);

		AttributeExt<?> returnedP = attrsMan
				.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertFalse(((VerifiableEmail) returnedP.getValues().get(0)).isConfirmed());
		Assert.assertTrue(((VerifiableEmail) returnedP.getValues().get(1)).isConfirmed());
		Assert.assertTrue(((VerifiableEmail) returnedP.getValues().get(2)).isConfirmed());
	}

	@Test
	public void shouldNotAddConfirmedAttributeIfAddedByUser() throws Exception
	{
		setupPasswordAuthn();
		Identity id = createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		EntityParam entity = new EntityParam(id.getEntityId());
		AttributeType atT = new AttributeType(InitializerCommon.EMAIL_ATTR,
				new VerifiableEmailAttributeSyntax());
		atT.setMaxElements(5);
		atT.setSelfModificable(true);
		attrsMan.addAttributeType(atT);

		setupUserContext("user1", false);

		VerifiableEmail e1 = new VerifiableEmail("a@example.com", new ConfirmationInfo(true));
		VerifiableEmail e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/", AttributeVisibility.full, e1, e2);
		attrsMan.setAttribute(entity, at1, false);

		AttributeExt<?> returned = attrsMan
				.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertFalse(((VerifiableEmail) returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(((VerifiableEmail) returned.getValues().get(1)).isConfirmed());
	}

	@Test
	public void shouldPreservedOneConfirmationStateIfChangedByUser() throws Exception
	{
		setupPasswordAuthn();
		Identity id = createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		EntityParam entity = new EntityParam(id.getEntityId());
		AttributeType atT = new AttributeType(InitializerCommon.EMAIL_ATTR,
				new VerifiableEmailAttributeSyntax());
		atT.setMaxElements(5);
		atT.setSelfModificable(true);
		attrsMan.addAttributeType(atT);

		setupAdmin();
		VerifiableEmail e1 = new VerifiableEmail("a@example.com", new ConfirmationInfo(true));
		VerifiableEmail e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/", AttributeVisibility.full, e1, e2);
		attrsMan.setAttribute(entity, at1, true);

		AttributeExt<?> returned = attrsMan
				.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertTrue(((VerifiableEmail) returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(((VerifiableEmail) returned.getValues().get(1)).isConfirmed());

		setupUserContext("user1", false);

		e1 = new VerifiableEmail("a@example.com", new ConfirmationInfo(false));
		e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(true));
		VerifiableEmail e3 = new VerifiableEmail("c@example.com",
				new ConfirmationInfo(true));
		at1 = new VerifiableEmailAttribute(InitializerCommon.EMAIL_ATTR, "/",
				AttributeVisibility.full, e3, e2, e1);
		attrsMan.setAttribute(entity, at1, true);

		returned = attrsMan.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertFalse(((VerifiableEmail) returned.getValues().get(0)).isConfirmed()); // reset
		Assert.assertFalse(((VerifiableEmail) returned.getValues().get(1)).isConfirmed()); // preserved old, reset
		Assert.assertTrue(((VerifiableEmail) returned.getValues().get(2)).isConfirmed()); // preserved old, set

	}

	@Test
	public void shouldThrowExceptionIfUserRemoveLastConfirmedValue() throws Exception
	{
		setupPasswordAuthn();
		Identity id = createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		EntityParam entity = new EntityParam(id.getEntityId());
		AttributeType atT = new AttributeType(InitializerCommon.EMAIL_ATTR,
				new VerifiableEmailAttributeSyntax());
		atT.setMaxElements(5);
		atT.setSelfModificable(true);
		attrsMan.addAttributeType(atT);

		setupAdmin();
		VerifiableEmail e1 = new VerifiableEmail("a@example.com",new ConfirmationInfo(true));
		VerifiableEmail e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/", AttributeVisibility.full, e1, e2);
		attrsMan.setAttribute(entity, at1, true);

		AttributeExt<?> returned = attrsMan
				.getAttributes(entity, "/", InitializerCommon.EMAIL_ATTR)
				.iterator().next();
		Assert.assertTrue(((VerifiableEmail) returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(((VerifiableEmail) returned.getValues().get(1)).isConfirmed());

		setupUserContext("user1", false);
		e1 = new VerifiableEmail("c@example.com", new ConfirmationInfo(false));
		e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		at1 = new VerifiableEmailAttribute(InitializerCommon.EMAIL_ATTR, "/",
				AttributeVisibility.full, e1, e2);
		try
		{
			attrsMan.setAttribute(entity, at1, true);
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

		attrsMan.addAttributeType(new AttributeType(InitializerCommon.EMAIL_ATTR,
				new VerifiableEmailAttributeSyntax()));
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/test", AttributeVisibility.full,
				"example2@ex.com");
		attrsMan.setAttribute(entity, at1, false);
		AttribiuteConfirmationState attrState = new AttribiuteConfirmationState(
				entity.getEntityId(), InitializerCommon.EMAIL_ATTR,
				"example2@ex.com", "pl", "/test", "", "");

		confirmationMan.sendConfirmationRequest(attrState);

		VerifiableElement vemail = getFirstEmailAttributeValueFromEntity(entity, "/test");
		Assert.assertFalse(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
	}
	
	@Test
	public void shouldThrowExceptionIfTheSameConfigurationExists() throws Exception
	{
		addSimpleConfirmationConfiguration(
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");
		try
		{
			configurationMan.addConfiguration(ConfirmationConfigurationBuilder
					.confirmationConfiguration()
					.withNameToConfirm(InitializerCommon.EMAIL_ATTR)
					.withTypeToConfirm(ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE)
					.withMsgTemplate("demoTemplate")
					.withNotificationChannel("demoChannel").build());
			
			fail("Added duplicate of confirmation configuration");
		} catch (Exception e)
		{
			// ok
		}
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
		attrsMan.addAttributeType(new AttributeType(InitializerCommon.EMAIL_ATTR,
				new VerifiableEmailAttributeSyntax()));
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/test", AttributeVisibility.full,
				"example2@ex.com");
		attrsMan.setAttribute(entity, at1, false);
		addSimpleConfirmationConfiguration(
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");

		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		
		AttribiuteConfirmationState attrState = new AttribiuteConfirmationState(
				entity.getEntityId(), InitializerCommon.EMAIL_ATTR,
				"example2@ex.com", "pl", "/test", "", "");
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
		Assert.assertEquals(1, tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();
		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			fail("Cannot proccess confirmation");
		}
		
		Assert.assertEquals(0, tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
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
				ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
				EmailIdentity.ID, "demoTemplate", "demoChannel");
	
		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());

		IdentityConfirmationState idState = new IdentityConfirmationState(
				entity.getEntityId(), EmailIdentity.ID, "example1@ex.com", "en",
				"", "");
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
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();
		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			fail("Cannot proccess confirmation");
		}
		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		identity = getFirstEmailIdentityFromEntity(entity);
		Assert.assertTrue(identity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, identity.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, identity.getConfirmationInfo().getConfirmationDate());

	}

	@Test
	public void checkAttributeFromRegistrationConfirmationProcess() throws Exception
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));

		RegistrationForm form = RegistrationFormBuilder
				.registrationForm()
				.withName("f1")
				.withDescription("description")
				.withCredentialRequirementAssignment(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedGroupAssignment("/A").withPubliclyAvailable(true)
				.withInitialEntityState(EntityState.valid)
				.withRegistrationCode("123").withAutoAcceptCondition("false")
				.withCollectComments(true)
				.withFormInformation().withDefaultValue("formInformation").endFormInformation()
				.withAttributeAssignments(new ArrayList<Attribute<?>>())
				.withAddedCredentialParam()
				.withCredentialName(EngineInitialization.DEFAULT_CREDENTIAL)
				.withDescription("description").withLabel("label")
				.endCredentialParam()
				.withAddedAgreement().withManatory(false).withText()
				.withDefaultValue("a").endText().endAgreement()
				.withAddedIdentityParam().withDescription("description")
				.withIdentityType(UsernameIdentity.ID).withLabel("label")
				.withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedAttributeParam()
				.withAttributeType(InitializerCommon.EMAIL_ATTR).withGroup("/")
				.withDescription("description").withLabel("label")
				.withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.withShowGroups(true).endAttributeParam()
				.withAddedGroupParam().withDescription("description")
				.withGroupPath("/B").withLabel("label")
				.withRetrievalSettings(ParameterRetrievalSettings.automatic)
				.endGroupParam().build();

		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");

		RegistrationRequest request = RegistrationRequestBuilder
				.registrationRequest()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						new VerifiableEmailAttribute(
								InitializerCommon.EMAIL_ATTR, "/",
								AttributeVisibility.full, "test1@a.b"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abs").toJson()).endCredential()
				.withAddedGroupSelection().withSelected(true).endGroupSelection()
				.withAddedIdentity().withTypeId(UsernameIdentity.ID)
				.withValue("username").endIdentity()

				.build();

		registrationsMan.submitRegistrationRequest(request, true);
		Assert.assertEquals(1,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();

		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			fail("Cannot proccess confirmation");
		}

		VerifiableElement vemail = getFirstEmailAttributeValueFromRegistration();
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());

	}

	@Test
	public void checkIdentityFromRegistrationConfirmationProcess() throws Exception
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));

		RegistrationForm form = RegistrationFormBuilder
				.registrationForm()
				.withName("f1")
				.withCredentialRequirementAssignment(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedGroupAssignment("/A").withPubliclyAvailable(true)
				.withInitialEntityState(EntityState.valid)
				.withRegistrationCode("123").withAutoAcceptCondition("false")
				.withCollectComments(true).withFormInformation()
				.withDefaultValue("formInformation").endFormInformation()
				.withAttributeAssignments(new ArrayList<Attribute<?>>())
				.withAddedCredentialParam().withCredentialName(EngineInitialization.DEFAULT_CREDENTIAL)
				.endCredentialParam()
				.withAddedAgreement().withManatory(false).withText()
				.withDefaultValue("a").endText().endAgreement()
				.withAddedIdentityParam()
				.withIdentityType(EmailIdentity.ID)
				.withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedAttributeParam()
				.withAttributeType(InitializerCommon.CN_ATTR).withGroup("/")
				.withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.withShowGroups(true).endAttributeParam()
				.withAddedGroupParam()
				.withGroupPath("/B")
				.withRetrievalSettings(ParameterRetrievalSettings.automatic)
				.endGroupParam().build();

		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
				EmailIdentity.ID, "demoTemplate", "demoChannel");

		RegistrationRequest request = RegistrationRequestBuilder
				.registrationRequest()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						new StringAttribute(InitializerCommon.CN_ATTR, "/",
								AttributeVisibility.full, "cn"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abs").toJson()).endCredential()
				.withAddedGroupSelection().withSelected(true).endGroupSelection()
				.withAddedIdentity().withTypeId(EmailIdentity.ID)
				.withValue("example@example.com").endIdentity()
				.build();

		registrationsMan.submitRegistrationRequest(request, true);
		Assert.assertEquals(1,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();

		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			fail("Cannot proccess confirmation");
		}

		VerifiableElement vemail = getFirstEmailIdentityFromRegistration();
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());

		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
	}

	@Test
	public void shouldAutoAcceptRegistrationRequestAfterConfirmAttribute()
			throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));

		RegistrationForm form = RegistrationFormBuilder
				.registrationForm()
				.withName("f1")
				.withCredentialRequirementAssignment(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedGroupAssignment("/A").withPubliclyAvailable(true)
				.withInitialEntityState(EntityState.valid)
				.withRegistrationCode("123")
				.withAutoAcceptCondition("attr[\"email\"].confirmed ==  true ")
				.withCollectComments(true).withFormInformation()
				.withDefaultValue("formInformation").endFormInformation()
				.withAttributeAssignments(new ArrayList<Attribute<?>>())
				.withAddedCredentialParam()
				.withCredentialName(EngineInitialization.DEFAULT_CREDENTIAL)
				.endCredentialParam()
				.withAddedAgreement().withManatory(false).withText()
				.withDefaultValue("a").endText().endAgreement()
				.withAddedIdentityParam()
				.withIdentityType(UsernameIdentity.ID)
				.withOptional(true)
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
				.endGroupParam().build();

		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");

		RegistrationRequest request = RegistrationRequestBuilder
				.registrationRequest()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						new VerifiableEmailAttribute(
								InitializerCommon.EMAIL_ATTR, "/",
								AttributeVisibility.full, "test2@a.b"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abs").toJson()).endCredential()
				.withAddedGroupSelection().withSelected(true).endGroupSelection()
				.withAddedIdentity().withTypeId(UsernameIdentity.ID)
				.withValue("username").endIdentity()
				.build();

		registrationsMan.submitRegistrationRequest(request, true);

		Assert.assertEquals(RegistrationRequestStatus.pending, registrationsMan
				.getRegistrationRequests().get(0).getStatus());

		Assert.assertEquals(1,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();

		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
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

		RegistrationForm form = RegistrationFormBuilder
				.registrationForm()
				.withName("f1")
				.withCredentialRequirementAssignment(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedGroupAssignment("/A").withPubliclyAvailable(true)
				.withInitialEntityState(EntityState.valid)
				.withRegistrationCode("123").withAutoAcceptCondition("false")
				.withCollectComments(true).withFormInformation()
				.withDefaultValue("formInformation").endFormInformation()
				.withAttributeAssignments(new ArrayList<Attribute<?>>())
				.withAddedCredentialParam()
				.withCredentialName(EngineInitialization.DEFAULT_CREDENTIAL)
				.endCredentialParam()
				.withAddedAgreement().withManatory(false).withText()
				.withDefaultValue("a").endText().endAgreement()
				.withAddedIdentityParam()
				.withIdentityType(EmailIdentity.ID)
				.withOptional(true)
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
				.endGroupParam()
				.build();

		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
				InitializerCommon.EMAIL_ATTR, "demoTemplate1", "demoChannel1");

		addSimpleConfirmationConfiguration(
				ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
				EmailIdentity.ID, "demoTemplate2", "demoChannel2");

		RegistrationRequest request = RegistrationRequestBuilder
				.registrationRequest()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						new VerifiableEmailAttribute(
								InitializerCommon.EMAIL_ATTR, "/",
								AttributeVisibility.full, "test3@a.b"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abs").toJson()).endCredential()
				.withAddedGroupSelection().withSelected(true).endGroupSelection()
				.withAddedIdentity().withTypeId(EmailIdentity.ID)
				.withValue("test33@c.d").endIdentity()
				.build();

		String requestId = registrationsMan.submitRegistrationRequest(request, true);

		Assert.assertEquals(2,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
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
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());

		for (Token tk : tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE))
		{
			byte[] tokenContents = tk.getContents();
			try
			{
				UserConfirmationState state = new UserConfirmationState(new String(
						tokenContents, StandardCharsets.UTF_8));
				if (!(state.getFacilityId().equals(
						AttribiuteConfirmationState.FACILITY_ID) || state
						.getFacilityId()
						.equals(IdentityConfirmationState.FACILITY_ID)))
					fail("Invalid facility id in confirmation state");

			} catch (Exception e)
			{
				fail("Tokens content cannot be parsed as UserConfirmationState");
			}
		}
	}
	
	@Test
	public void shouldSkipProcessRejectedRequest() throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));

		RegistrationForm form = RegistrationFormBuilder
				.registrationForm()
				.withName("f1")
				.withCredentialRequirementAssignment(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedGroupAssignment("/A").withPubliclyAvailable(true)
				.withInitialEntityState(EntityState.valid)
				.withRegistrationCode("123")
				.withAutoAcceptCondition("attr[\"email\"].confirmed ==  true ")
				.withCollectComments(true).withFormInformation()
				.withDefaultValue("formInformation").endFormInformation()
				.withAttributeAssignments(new ArrayList<Attribute<?>>())
				.withAddedCredentialParam()
				.withCredentialName(EngineInitialization.DEFAULT_CREDENTIAL)
				.endCredentialParam()
				.withAddedAgreement().withManatory(false).withText()
				.withDefaultValue("a").endText().endAgreement()
				.withAddedIdentityParam()
				.withIdentityType(UsernameIdentity.ID)
				.withOptional(true)
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
				.endGroupParam()
				.build();

		registrationsMan.addForm(form);

		addSimpleConfirmationConfiguration(
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");

		RegistrationRequest request = RegistrationRequestBuilder
				.registrationRequest()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						new VerifiableEmailAttribute(
								InitializerCommon.EMAIL_ATTR, "/",
								AttributeVisibility.full, "test5@a.b"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abs").toJson()).endCredential()
				.withAddedGroupSelection().withSelected(true).endGroupSelection()
				.withAddedIdentity().withTypeId(UsernameIdentity.ID)
				.withValue("username").endIdentity()
				.build();

		String requestId = registrationsMan.submitRegistrationRequest(request, true);
		
		Assert.assertEquals(RegistrationRequestStatus.pending, registrationsMan
				.getRegistrationRequests().get(0).getStatus());

		Assert.assertEquals(1,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();
		RegistrationRequestState requestState = registrationsMan.getRegistrationRequests()
				.get(0);
		registrationsMan.processRegistrationRequest(requestId, requestState.getRequest(),
				RegistrationRequestAction.reject, "", "");
		
		ConfirmationStatus status = null; 
		try
		{
			 status = confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			fail("Cannot proccess confirmation");
		}
		
		Assert.assertFalse(status.isSuccess());
	}
	
	@Test
	public void shouldNotSendConfirmationRequestIfLimitExceeded() throws Exception
	{
		setupPasswordAuthn();
		
		Identity id = createUsernameUser(AuthorizationManagerImpl.USER_ROLE);
		EntityParam entity = new EntityParam(id.getEntityId());
		AttributeType atT = new AttributeType(InitializerCommon.EMAIL_ATTR,
				new VerifiableEmailAttributeSyntax());
		atT.setMaxElements(5);
		atT.setSelfModificable(true);
		attrsMan.addAttributeType(atT);
		groupsMan.addGroup(new Group("/test"));
		groupsMan.addMemberFromParent("/test", entity);
		
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/test", AttributeVisibility.full,"test6@ex.com");
		addSimpleConfirmationConfiguration(
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
				InitializerCommon.EMAIL_ATTR, "demoTemplate", "demoChannel");	
		setupAdmin();
		for (int i = 0; i < mainConfig.getIntValue(UnityServerConfiguration.CONFIRMATION_REQUEST_LIMIT); i++)
		{
			at1.getValues().get(0).setConfirmationInfo(new ConfirmationInfo(false));
			attrsMan.setAttribute(entity, at1, true);
			String token = tokensMan
					.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
					.get(0).getValue();
			try
			{
				confirmationMan.processConfirmation(token);
			} catch (Exception e)
			{
				fail("Cannot proccess confirmation");
			}
			
		}	
		attrsMan.setAttribute(entity, at1, true);
		Assert.assertTrue(tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE).isEmpty());	
		
		Collection<AttributeExt<?>>  attrs = attrsMan.getAttributes(entity, "/test", InitializerCommon.EMAIL_ATTR);
		VerifiableElement vElement = (VerifiableElement) attrs.iterator().next().getValues().get(0);
		Assert.assertEquals(0, vElement.getConfirmationInfo().getSentRequestAmount());	
	}
	
	private void addSimpleConfirmationConfiguration(String type, String name,
			String templateName, String channelName) throws EngineException
	{
		I18nMessage message = new I18nMessage(new I18nString("test"), new I18nString(
				"test ${" + ConfirmationTemplateDef.CONFIRMATION_LINK + "}"));
		templateMan.addTemplate(new MessageTemplate(templateName, "demo", message,
				ConfirmationTemplateDef.NAME));
		
		notMan.addNotificationChannel(NotificationChannelBuilder.notificationChannel()
				.withName(channelName)
				.withConfiguration("test")
				.withDescription("test")
				.withFacilityId("test")
				.build());
		
		configurationMan.addConfiguration(ConfirmationConfigurationBuilder
				.confirmationConfiguration()
				.withNameToConfirm(name)
				.withTypeToConfirm(type)
				.withMsgTemplate(templateName)
				.withNotificationChannel(channelName).build());
	}
	
	private VerifiableElement getFirstEmailIdentityFromEntity(EntityParam entity)
			throws EngineException
	{
		Entity e = idsMan.getEntityNoContext(entity, "/test");
		return e.getIdentities()[0];
	}

	private VerifiableElement getFirstEmailAttributeValueFromEntity(EntityParam entity,
			String group) throws EngineException
	{
		Collection<AttributeExt<?>> allAttributes = attrsMan.getAllAttributes(entity,
				false, group, InitializerCommon.EMAIL_ATTR, false);
		AttributeExt<?> attribute = allAttributes.iterator().next();
		VerifiableElement vemail = (VerifiableElement) attribute.getValues().get(0);
		return vemail;
	}

	private VerifiableElement getFirstEmailAttributeValueFromRegistration() throws EngineException
	{
		RegistrationRequestState state = registrationsMan.getRegistrationRequests().get(0);
		return (VerifiableElement) state.getRequest().getAttributes().get(0).getValues()
				.get(0);
	}

	private VerifiableElement getFirstEmailIdentityFromRegistration() throws EngineException
	{
		RegistrationRequestState state = registrationsMan.getRegistrationRequests().get(0);
		return (VerifiableElement) state.getRequest().getIdentities().get(0);
	}
}
