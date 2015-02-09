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

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqIdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.UserConfirmationState;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
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
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
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
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

/**
 * 
 * @author P. Piernik
 * 
 */
public class TestConfirmation extends DBIntegrationTestBase
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

	@Test
	public void attributeConfirmationStatePreservedForAdmin() throws Exception
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
		
		VerifiableEmail e1 = new VerifiableEmail("a@example.com", new ConfirmationInfo(true));
		VerifiableEmail e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/",
				AttributeVisibility.full, e1, e2);
		attrsMan.setAttribute(entity, at1, false);
		
		AttributeExt<?> returned = attrsMan.getAttributes(entity, "/", 
				InitializerCommon.EMAIL_ATTR).iterator().next();		
		Assert.assertTrue(((VerifiableEmail)returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(((VerifiableEmail)returned.getValues().get(1)).isConfirmed());
		
		VerifiableEmail e1P = new VerifiableEmail("a@example.com", new ConfirmationInfo(false));
		VerifiableEmail e2P = new VerifiableEmail("b@example.com", new ConfirmationInfo(true));
		VerifiableEmail e3P = new VerifiableEmail("c@example.com", new ConfirmationInfo(true));
		VerifiableEmailAttribute at1P = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/",
				AttributeVisibility.full, e1P, e2P, e3P);
		attrsMan.setAttribute(entity, at1P, true);

		AttributeExt<?> returnedP = attrsMan.getAttributes(entity, "/", 
				InitializerCommon.EMAIL_ATTR).iterator().next();		
		Assert.assertFalse(((VerifiableEmail)returnedP.getValues().get(0)).isConfirmed()); 
		Assert.assertTrue(((VerifiableEmail)returnedP.getValues().get(1)).isConfirmed()); 
		Assert.assertTrue(((VerifiableEmail)returnedP.getValues().get(2)).isConfirmed()); 
	}

	/**
	 * Here we ensure that on added attribute state is forced to unconfirmed and that the state is preserved 
	 * on update.
	 *  
	 * @throws Exception
	 */
	@Test
	public void attributeConfirmationStatePreservedForUser() throws Exception
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
				InitializerCommon.EMAIL_ATTR, "/",
				AttributeVisibility.full, e1, e2);
		attrsMan.setAttribute(entity, at1, false);
		
		AttributeExt<?> returned = attrsMan.getAttributes(entity, "/", 
				InitializerCommon.EMAIL_ATTR).iterator().next();
		Assert.assertFalse(((VerifiableEmail)returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(((VerifiableEmail)returned.getValues().get(1)).isConfirmed());
		
		setupAdmin();
		e1 = new VerifiableEmail("a@example.com", new ConfirmationInfo(true));
		e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/",
				AttributeVisibility.full, e1, e2);
		attrsMan.setAttribute(entity, at1, true);

		returned = attrsMan.getAttributes(entity, "/", 
				InitializerCommon.EMAIL_ATTR).iterator().next();
		Assert.assertTrue(((VerifiableEmail)returned.getValues().get(0)).isConfirmed());
		Assert.assertFalse(((VerifiableEmail)returned.getValues().get(1)).isConfirmed());

		setupUserContext("user1", false);

		e1 = new VerifiableEmail("a@example.com", new ConfirmationInfo(false));
		e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(true));
		VerifiableEmail e3 = new VerifiableEmail("c@example.com", new ConfirmationInfo(true));
		at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/",
				AttributeVisibility.full, e3, e2, e1);
		attrsMan.setAttribute(entity, at1, true);

		returned = attrsMan.getAttributes(entity, "/", 
				InitializerCommon.EMAIL_ATTR).iterator().next();
		Assert.assertFalse(((VerifiableEmail)returned.getValues().get(0)).isConfirmed()); //reset
		Assert.assertFalse(((VerifiableEmail)returned.getValues().get(1)).isConfirmed()); //preserved old, reset
		Assert.assertTrue(((VerifiableEmail)returned.getValues().get(2)).isConfirmed()); //preserved old, set
		
		e1 = new VerifiableEmail("c@example.com", new ConfirmationInfo(false));
		e2 = new VerifiableEmail("b@example.com", new ConfirmationInfo(false));
		at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/",
				AttributeVisibility.full, e1, e2);
		try
		{
			attrsMan.setAttribute(entity, at1, true);
			fail("Ordinary user managed to remove the last confirmed attribute value");
		} catch (IllegalAttributeValueException e)
		{
			//OK
		}

	}
	
	@Test
	public void testConfirmationFromEntity() throws Exception
	{
		setupMockAuthn();
		groupsMan.addGroup(new Group("/test"));
		Identity id = idsMan.addEntity(new IdentityParam(EmailIdentity.ID,
				"example1@ex.com"), "crMock", EntityState.valid, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		groupsMan.addMemberFromParent("/test", entity);

		// Attribute
		attrsMan.addAttributeType(new AttributeType(InitializerCommon.EMAIL_ATTR,
				new VerifiableEmailAttributeSyntax()));
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute(
				InitializerCommon.EMAIL_ATTR, "/test",
				AttributeVisibility.full, "example2@ex.com");
		attrsMan.setAttribute(entity, at1, false);
		AttribiuteConfirmationState attrState = new AttribiuteConfirmationState(
				entity.getEntityId(), InitializerCommon.EMAIL_ATTR, 
				"example2@ex.com", "pl", "/test", "", "");
		try
		{
			confirmationMan.sendConfirmationRequest(attrState);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot send confirmation request");
		}
		VerifiableElement vemail = getAttributeValueFromEntity(entity, "/test");
		Assert.assertTrue(!vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		addTemplate();
		addNotificationChannel();
		try
		{
			configurationMan.addConfiguration(new ConfirmationConfiguration(
					ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
					InitializerCommon.EMAIL_ATTR, "demoChannel",
					"demoTemplate"));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot add configuration");
		}
		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		try
		{
			confirmationMan.sendConfirmationRequest(attrState);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot send confirmation request");
		}
		vemail = getAttributeValueFromEntity(entity, "/test");
		Assert.assertTrue(!vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(1, vemail.getConfirmationInfo().getSentRequestAmount());
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
			e.printStackTrace();
			fail("Cannot proccess confirmation");
		}
		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		vemail = getAttributeValueFromEntity(entity, "/test");
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());

		// Identity

		IdentityConfirmationState idState = new IdentityConfirmationState(
				entity.getEntityId(), EmailIdentity.ID, "example1@ex.com", "en", "", "");
		VerifiableElement identity = getIdentityFromEntity(entity);
		Assert.assertTrue(!identity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, identity.getConfirmationInfo().getSentRequestAmount());
		confirmationMan.sendConfirmationRequest(idState);
		identity = getIdentityFromEntity(entity);
		Assert.assertTrue(!identity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, identity.getConfirmationInfo().getSentRequestAmount());
		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		try
		{
			configurationMan.addConfiguration(new ConfirmationConfiguration(
					ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
					EmailIdentity.ID, "demoChannel", "demoTemplate"));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot add configuration");
		}
		try
		{
			confirmationMan.sendConfirmationRequest(idState);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot send confirmation request");
		}
		identity = getIdentityFromEntity(entity);
		Assert.assertTrue(!identity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(1, identity.getConfirmationInfo().getSentRequestAmount());
		Assert.assertEquals(1,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		token = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE).get(0)
				.getValue();
		try
		{
			confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot proccess confirmation");
		}
		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		identity = getIdentityFromEntity(entity);
		Assert.assertTrue(identity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, identity.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, identity.getConfirmationInfo().getConfirmationDate());
		try
		{
			configurationMan.removeConfiguration(
					ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
					InitializerCommon.EMAIL_ATTR);

			configurationMan.removeConfiguration(
					ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
					EmailIdentity.ID);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot add configurations");
		}
		Assert.assertEquals(0, configurationMan.getAllConfigurations().size());
	}

	private void addNotificationChannel() throws EngineException
	{
		notMan.addNotificationChannel(NotificationChannelBuilder.notificationChannel()
				.withName("demoChannel").withConfiguration("test")
				.withDescription("test").withFacilityId("test").build());
	}

	private VerifiableElement getAttributeValueFromEntity(EntityParam entity, String group)
			throws EngineException
	{
		Collection<AttributeExt<?>> allAttributes = attrsMan.getAllAttributes(entity,
				false, group, InitializerCommon.EMAIL_ATTR, false);
		AttributeExt<?> attribute = allAttributes.iterator().next();
		VerifiableElement vemail = (VerifiableElement) attribute.getValues().get(0);
		return vemail;

	}

	private VerifiableElement getIdentityFromEntity(EntityParam entity) throws EngineException
	{
		Entity e = idsMan.getEntityNoContext(entity, "/test");
		return e.getIdentities()[0];
	}

	private RegistrationForm getForm()
	{
		return RegistrationFormBuilder.registrationForm().withName("f1").withDescription("description")
				.withCredentialRequirementAssignment(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedGroupAssignment("/A")
				.withPubliclyAvailable(true)
				.withInitialEntityState(EntityState.valid)
				.withRegistrationCode("123")
				.withAutoAcceptCondition("false")
				.withCollectComments(true)
				.withFormInformation().withDefaultValue("formInformation").endFormInformation()
				.withAttributeAssignments(new ArrayList<Attribute<?>>())
				
				.withAddedCredentialParam()
				.withCredentialName(EngineInitialization.DEFAULT_CREDENTIAL)
				.withDescription("description")
				.withLabel("label")
				.endCredentialParam()
				
				.withAddedAgreement()
				.withManatory(false)
				.withText().withDefaultValue("a").endText()
				.endAgreement()

				.withAddedIdentityParam().withDescription("description")
				.withIdentityType(EmailIdentity.ID).withLabel("label")
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
				.withGroupPath("/B")
				.withLabel("label")
				.withRetrievalSettings(ParameterRetrievalSettings.automatic)
				.endGroupParam()
				.build();	
	}

	private RegistrationRequest getRequest()
	{
		return RegistrationRequestBuilder.registrationRequest().withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				
				.withAddedAttribute(
						new VerifiableEmailAttribute(
								InitializerCommon.EMAIL_ATTR, "/",
								AttributeVisibility.full, "foo@a.b"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abs").toJson())
				.endCredential()
				
				.withAddedGroupSelection()
				.withSelected(true)
				.endGroupSelection()
				
				.withAddedIdentity()
				.withTypeId(EmailIdentity.ID)
				.withValue("example@ex.com")
				.endIdentity()
					
				.build();
	}

	private void addTemplate() throws EngineException
	{
		I18nMessage message = new I18nMessage(new I18nString("test"), 
				new I18nString("test ${"+ ConfirmationTemplateDef.CONFIRMATION_LINK + "}"));		
		templateMan.addTemplate(new MessageTemplate("demoTemplate", "demo", message,
				ConfirmationTemplateDef.NAME));
	}

	private VerifiableElement getAttributeValueFromRegistration() throws EngineException
	{
		RegistrationRequestState state = registrationsMan.getRegistrationRequests().get(0);
		return (VerifiableElement) state.getRequest().getAttributes().get(0).getValues()
				.get(0);
	}

	private VerifiableElement getIdentityFromRegistration() throws EngineException
	{
		RegistrationRequestState state = registrationsMan.getRegistrationRequests().get(0);
		return (VerifiableElement) state.getRequest().getIdentities().get(0);
	}

	@Test
	public void testConfirmationFromRegistration() throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		registrationsMan.addForm(getForm());

		addTemplate();
		addNotificationChannel();
		try
		{
			configurationMan.addConfiguration(new ConfirmationConfiguration(
					ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
					InitializerCommon.EMAIL_ATTR, "demoChannel",
					"demoTemplate"));
			configurationMan.addConfiguration(new ConfirmationConfiguration(
					ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
					EmailIdentity.ID, "demoChannel", "demoTemplate"));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot add configurations");
		}

		RegistrationRequest request = getRequest();
		String requestId = registrationsMan.submitRegistrationRequest(request, true);

		Assert.assertEquals(2, tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE).size());

		try
		{
			for (Token tk : tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE))
			{
				confirmationMan.processConfirmation(tk.getValue());
			}

		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot proccess confirmation");
		}

		VerifiableElement vemail = getAttributeValueFromRegistration();
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());

		VerifiableElement videntity = getIdentityFromRegistration();
		Assert.assertTrue(videntity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, videntity.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, videntity.getConfirmationInfo().getConfirmationDate());

		RegistrationReqAttribiuteConfirmationState attrState = new RegistrationReqAttribiuteConfirmationState(
				requestId, InitializerCommon.EMAIL_ATTR, "foo@a.b", "pl", "/", "", "");

		RegistrationReqIdentityConfirmationState idState = new RegistrationReqIdentityConfirmationState(
				requestId, EmailIdentity.ID, "example@ex.com", "pl", "", "");

		try
		{
			confirmationMan.sendConfirmationRequest(attrState);

			confirmationMan.sendConfirmationRequest(idState);
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot add configurations");
		}

		vemail = getAttributeValueFromRegistration();
		Assert.assertFalse(vemail.toString(), vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(1, vemail.getConfirmationInfo().getSentRequestAmount());

		videntity = getIdentityFromRegistration();
		Assert.assertFalse(videntity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(1, videntity.getConfirmationInfo().getSentRequestAmount());

		Assert.assertEquals(2,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE).size());
		try
		{
			for (Token tk : tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE))
			{
				confirmationMan.processConfirmation(tk.getValue());
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot proccess confirmation");
		}

		vemail = getAttributeValueFromRegistration();
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());

		videntity = getIdentityFromRegistration();
		Assert.assertTrue(videntity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, videntity.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, videntity.getConfirmationInfo().getConfirmationDate());

		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE).size());

	}

	@Test
	public void testRewriteToken() throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		RegistrationForm form = getForm();
		form.setAutoAcceptCondition("attr[\"email\"].confirmed ==  true ");
		registrationsMan.addForm(form);

		addTemplate();
		addNotificationChannel();
		try
		{
			configurationMan.addConfiguration(new ConfirmationConfiguration(
					ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
					InitializerCommon.EMAIL_ATTR, "demoChannel",
					"demoTemplate"));
			configurationMan.addConfiguration(new ConfirmationConfiguration(
					ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
					EmailIdentity.ID, "demoChannel", "demoTemplate"));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot add configurations");
		}

		RegistrationRequest request = getRequest();
		registrationsMan.submitRegistrationRequest(request, true);

		Assert.assertEquals(RegistrationRequestStatus.pending, registrationsMan
				.getRegistrationRequests().get(0).getStatus());

		Assert.assertEquals(2, tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE).size());
		try
		{
			boolean proccess = false;
			for (Token tk : tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE))
			{
				BaseConfirmationState state = new BaseConfirmationState(tk.getContentsString());
				if (state.getFacilityId()
						.equals(RegistrationReqAttribiuteConfirmationState.FACILITY_ID))
				{
					confirmationMan.processConfirmation(tk.getValue());
					proccess = true;
				}
				if (!proccess)
					fail("Confirmation tokens not contain any "
							+ RegistrationReqAttribiuteConfirmationState.FACILITY_ID);
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail();

		}

		Assert.assertEquals(RegistrationRequestStatus.accepted, registrationsMan
				.getRegistrationRequests().get(0).getStatus());

		Assert.assertEquals(1, tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE).size());

		byte[] tokenContents = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE).get(0)
				.getContents();
		UserConfirmationState state = new UserConfirmationState(new String(tokenContents, 
				StandardCharsets.UTF_8));
		EntityParam entity = new EntityParam(state.getOwnerEntityId());
		VerifiableElement vattr = getAttributeValueFromEntity(entity, "/");
		Assert.assertTrue(vattr.getConfirmationInfo().isConfirmed());
		VerifiableElement id = getIdentityFromEntity(entity);
		Assert.assertFalse(id.getConfirmationInfo().isConfirmed());

		Assert.assertEquals(1, tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE).size());
		try
		{
			for (Token tk : tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE))
			{
				confirmationMan.processConfirmation(tk.getValue());
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail();

		}
		id = getIdentityFromEntity(entity);
		Assert.assertTrue(id.getConfirmationInfo().isConfirmed());

	}
}
