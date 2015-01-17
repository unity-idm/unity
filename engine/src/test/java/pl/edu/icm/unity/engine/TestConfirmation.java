/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.engine.confirmations.ConfirmationManagerImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.Message;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.VerifiableElement;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.NotificationChannel;
import pl.edu.icm.unity.types.registration.RegistrationForm;

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
		attrsMan.addAttributeType(new AttributeType("email",
				new VerifiableEmailAttributeSyntax()));
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute("email", "/test",
				AttributeVisibility.full, "example2@ex.com");
		attrsMan.setAttribute(entity, at1, false);
		AttribiuteConfirmationState attrState = new AttribiuteConfirmationState();
		attrState.setGroup("/test");
		attrState.setOwner(entity.getEntityId().toString());
		attrState.setType("email");
		attrState.setValue("example2@ex.com");
		try
		{
			confirmationMan.sendConfirmationRequest(attrState
					.getSerializedConfiguration());
		} catch (Exception e)
		{
			fail();
		}
		VerifiableElement vemail = getAttributeValueFromEntity(entity);
		Assert.assertTrue(!vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		HashMap<String, MessageTemplate.Message> params = new HashMap<String, MessageTemplate.Message>();
		params.put("", new Message("test", "test"));
		templateMan.addTemplate(new MessageTemplate("demoTemplate", "demo", params,
				ConfirmationTemplateDef.NAME));

		notificationsMan.addNotificationChannel(new NotificationChannel("demoChannel",
				"test", "test", "test"));
		try
		{
			configurationMan.addConfiguration(new ConfirmationConfiguration(
					ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
					"email", "demoChannel", "demoTemplate"));
		} catch (Exception e)
		{
			fail();
		}
		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		try
		{
			confirmationMan.sendConfirmationRequest(attrState
					.getSerializedConfiguration());
		} catch (Exception e)
		{
			fail();
		}
		vemail = getAttributeValueFromEntity(entity);
		Assert.assertTrue(!vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(1, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertEquals(1,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		String token = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
				.get(0).getValue();
		try
		{
			confirmationMan.proccessConfirmation(token);
		} catch (Exception e)
		{
			fail();
		}
		Assert.assertEquals(0,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		vemail = getAttributeValueFromEntity(entity);
		Assert.assertTrue(vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, vemail.getConfirmationInfo().getSentRequestAmount());
		Assert.assertNotEquals(0, vemail.getConfirmationInfo().getConfirmationDate());

		// Identity

		IdentityConfirmationState idState = new IdentityConfirmationState();
		idState.setOwner(entity.getEntityId().toString());
		idState.setType(EmailIdentity.ID);
		idState.setValue("example1@ex.com");
		VerifiableElement identity = getIdentityFromEntity(entity);
		Assert.assertTrue(!identity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(0, identity.getConfirmationInfo().getSentRequestAmount());
		confirmationMan.sendConfirmationRequest(idState.getSerializedConfiguration());
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
					"email", "demoChannel", "demoTemplate"));
		} catch (Exception e)
		{
			fail();
		}
		try
		{
			confirmationMan.sendConfirmationRequest(idState
					.getSerializedConfiguration());
		} catch (Exception e)
		{
			fail();
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
			confirmationMan.proccessConfirmation(token);
		} catch (Exception e)
		{
			fail();
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
					"email");

			configurationMan.removeConfiguration(
					ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
					"email");
		} catch (Exception e)
		{
			fail();
		}
		Assert.assertEquals(0, configurationMan.getAllConfigurations().size());
	}

	private VerifiableElement getAttributeValueFromEntity(EntityParam entity)
			throws EngineException
	{
		Collection<AttributeExt<?>> allAttributes = attrsMan.getAllAttributes(entity,
				false, "/test", "email", false);
		Assert.assertTrue(allAttributes.size() == 1);
		AttributeExt<?> attribute = allAttributes.iterator().next();
		VerifiableElement vemail = (VerifiableElement) attribute.getValues().get(0);
		return vemail;

	}

	private VerifiableElement getIdentityFromEntity(EntityParam entity) throws EngineException
	{
		Entity e = idsMan.getEntityNoContext(entity, "/test");
		return e.getIdentities()[0];
	}

	@Test
	public void testFromRegistration() throws EngineException
	{
		RegistrationForm form = new RegistrationForm();
		registrationsMan.addForm(form);
	}






}

