/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
import pl.edu.icm.unity.engine.confirmations.ConfirmationManagerImpl;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.Message;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.NotificationChannel;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.Selection;

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
	public void testConfirmationFromEntity() throws Exception
	{
		setupMockAuthn();
		groupsMan.addGroup(new Group("/test"));
		Identity id = idsMan.addEntity(new IdentityParam(EmailIdentity.ID,
				"example1@ex.com"), "crMock", EntityState.valid, false);
		EntityParam entity = new EntityParam(id.getEntityId());
		groupsMan.addMemberFromParent("/test", entity);

		// Attribute
		attrsMan.addAttributeType(new AttributeType(VerifiableEmailAttributeSyntax.ID,
				new VerifiableEmailAttributeSyntax()));
		VerifiableEmailAttribute at1 = new VerifiableEmailAttribute(
				VerifiableEmailAttributeSyntax.ID, "/test",
				AttributeVisibility.full, "example2@ex.com");
		attrsMan.setAttribute(entity, at1, false);
		AttribiuteConfirmationState attrState = new AttribiuteConfirmationState();
		attrState.setGroup("/test");
		attrState.setOwner(entity.getEntityId().toString());
		attrState.setType(VerifiableEmailAttributeSyntax.ID);
		attrState.setValue("example2@ex.com");
		try
		{
			confirmationMan.sendConfirmationRequest(attrState
					.getSerializedConfiguration());
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
					VerifiableEmailAttributeSyntax.ID, "demoChannel",
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
			confirmationMan.sendConfirmationRequest(attrState
					.getSerializedConfiguration());
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
					EmailIdentity.ID, "demoChannel", "demoTemplate"));
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot add configuration");
		}
		try
		{
			confirmationMan.sendConfirmationRequest(idState
					.getSerializedConfiguration());
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
					VerifiableEmailAttributeSyntax.ID);

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
		notificationsMan.addNotificationChannel(new NotificationChannel("demoChannel",
				"test", "test", "test"));

	}

	private VerifiableElement getAttributeValueFromEntity(EntityParam entity, String group)
			throws EngineException
	{
		Collection<AttributeExt<?>> allAttributes = attrsMan.getAllAttributes(entity,
				false, group, VerifiableEmailAttributeSyntax.ID, false);
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
		RegistrationForm form = new RegistrationForm();

		AgreementRegistrationParam agreement = new AgreementRegistrationParam();
		agreement.setManatory(false);
		agreement.setText("a");
		form.setAgreements(Collections.singletonList(agreement));

		Attribute<?> attr1 = new StringAttribute("cn", "/", AttributeVisibility.full, "val");
		Attribute<?> attr2 = new StringAttribute("email", "/", AttributeVisibility.full,
				"val@ex.com");
		List<Attribute<?>> attrs = new ArrayList<>();
		attrs.add(attr1);
		attrs.add(attr2);
		form.setAttributeAssignments(attrs);

		AttributeClassAssignment acA = new AttributeClassAssignment();
		acA.setAcName(InitializerCommon.NAMING_AC);
		acA.setGroup("/");
		form.setAttributeClassAssignments(Collections.singletonList(acA));

		AttributeRegistrationParam attrReg = new AttributeRegistrationParam();
		attrReg.setAttributeType(VerifiableEmailAttributeSyntax.ID);
		attrReg.setDescription("description");
		attrReg.setGroup("/");
		attrReg.setLabel("label");
		attrReg.setOptional(true);
		attrReg.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		attrReg.setShowGroups(true);
		attrReg.setUseDescription(true);
		form.setAttributeParams(Collections.singletonList(attrReg));

		form.setCollectComments(true);

		CredentialRegistrationParam credParam = new CredentialRegistrationParam();
		credParam.setCredentialName(EngineInitialization.DEFAULT_CREDENTIAL);
		credParam.setDescription("description");
		credParam.setLabel("label");
		form.setCredentialParams(Collections.singletonList(credParam));

		form.setCredentialRequirementAssignment(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
		form.setDescription("description");
		form.setFormInformation("formInformation");
		form.setGroupAssignments(Collections.singletonList("/A"));

		GroupRegistrationParam groupParam = new GroupRegistrationParam();
		groupParam.setDescription("description");
		groupParam.setGroupPath("/B");
		groupParam.setLabel("label");
		groupParam.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		form.setGroupParams(Collections.singletonList(groupParam));

		IdentityRegistrationParam idParam = new IdentityRegistrationParam();
		idParam.setDescription("description");
		idParam.setIdentityType(EmailIdentity.ID);
		idParam.setLabel("label");
		idParam.setOptional(true);
		idParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden);
		form.setIdentityParams(Collections.singletonList(idParam));
		form.setName("f1");
		form.setPubliclyAvailable(true);
		form.setInitialEntityState(EntityState.valid);
		form.setRegistrationCode("123");
		form.setAutoAcceptCondition("false");
		return form;
	}

	private RegistrationRequest getRequest()
	{
		RegistrationRequest request = new RegistrationRequest();

		request.setAgreements(Collections.singletonList(new Selection(true)));
		List<Attribute<?>> attrs = new ArrayList<Attribute<?>>();
		attrs.add(new VerifiableEmailAttribute(VerifiableEmailAttributeSyntax.ID, "/",
				AttributeVisibility.full, "foo@a.b"));
		request.setAttributes(attrs);
		request.setComments("comments");
		CredentialParamValue cp = new CredentialParamValue();
		cp.setCredentialId(EngineInitialization.DEFAULT_CREDENTIAL);
		cp.setSecrets(new PasswordToken("abc").toJson());
		request.setCredentials(Collections.singletonList(cp));
		request.setFormId("f1");
		request.setGroupSelections(Collections.singletonList(new Selection(true)));
		IdentityParam ip = new IdentityParam(EmailIdentity.ID, "example@ex.com");
		request.setIdentities(Collections.singletonList(ip));
		request.setRegistrationCode("123");
		return request;
	}

	private void addTemplate() throws EngineException
	{
		HashMap<String, MessageTemplate.Message> params = new HashMap<String, MessageTemplate.Message>();
		params.put("", new Message("test", "test ${"
				+ ConfirmationTemplateDef.CONFIRMATION_LINK + "}"));
		templateMan.addTemplate(new MessageTemplate("demoTemplate", "demo", params,
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
					VerifiableEmailAttributeSyntax.ID, "demoChannel",
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

		Assert.assertEquals(2,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());

		try
		{
			for (Token tk : tokensMan
					.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE))
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

		RegistrationReqAttribiuteConfirmationState attrState = new RegistrationReqAttribiuteConfirmationState();
		attrState.setOwner(requestId);
		attrState.setGroup("/");
		attrState.setType(VerifiableEmailAttributeSyntax.ID);
		attrState.setValue("foo@a.b");

		RegistrationReqIdentityConfirmationState idState = new RegistrationReqIdentityConfirmationState();
		idState.setOwner(requestId);
		idState.setType(EmailIdentity.ID);
		idState.setValue("example@ex.com");

		try
		{
			confirmationMan.sendConfirmationRequest(attrState
					.getSerializedConfiguration());

			confirmationMan.sendConfirmationRequest(idState
					.getSerializedConfiguration());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("Cannot add configurations");
		}

		vemail = getAttributeValueFromRegistration();
		Assert.assertTrue(!vemail.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(1, vemail.getConfirmationInfo().getSentRequestAmount());

		videntity = getIdentityFromRegistration();
		Assert.assertTrue(!videntity.getConfirmationInfo().isConfirmed());
		Assert.assertEquals(1, videntity.getConfirmationInfo().getSentRequestAmount());

		Assert.assertEquals(2,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		try
		{
			for (Token tk : tokensMan
					.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE))
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
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());

	}

	@Test
	public void testRewriteToken() throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		RegistrationForm form = getForm();
		form.setAutoAcceptCondition("attr[\"verifiableEmail\"].confirmed ==  true ");
		registrationsMan.addForm(form);

		addTemplate();
		addNotificationChannel();
		try
		{
			configurationMan.addConfiguration(new ConfirmationConfiguration(
					ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
					VerifiableEmailAttributeSyntax.ID, "demoChannel",
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

		Assert.assertEquals(2,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		try
		{
			boolean proccess = false;
			for (Token tk : tokensMan
					.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE))
			{
				BaseConfirmationState state = new BaseConfirmationState();
				state.setSerializedConfiguration(new String(tk.getContents()));
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

		Assert.assertEquals(1,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());

		BaseConfirmationState state = new BaseConfirmationState();
		state.setSerializedConfiguration(new String(tokensMan
				.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE).get(0)
				.getContents()));
		String entity = state.getOwner();
		VerifiableElement vattr = getAttributeValueFromEntity(
				new EntityParam(Long.parseLong(entity)), "/");
		Assert.assertTrue(vattr.getConfirmationInfo().isConfirmed());
		VerifiableElement id = getIdentityFromEntity(new EntityParam(Long.parseLong(entity)));
		Assert.assertFalse(id.getConfirmationInfo().isConfirmed());

		Assert.assertEquals(1,
				tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE)
						.size());
		try
		{
			for (Token tk : tokensMan
					.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE))
			{

				confirmationMan.processConfirmation(tk.getValue());

			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail();

		}
		id = getIdentityFromEntity(new EntityParam(Long.parseLong(entity)));
		Assert.assertTrue(id.getConfirmationInfo().isConfirmed());

	}
}
