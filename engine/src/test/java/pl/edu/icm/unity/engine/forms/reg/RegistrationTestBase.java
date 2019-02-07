/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddAttributeClassActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * Utility methods for registration tests
 * @author K. Benedyczak
 */
public abstract class RegistrationTestBase extends DBIntegrationTestBase
{
	@Autowired
	private InitializerCommon commonInitializer;
	
	protected RegistrationFormBuilder getFormBuilder(boolean nullCode, String autoAcceptCondition, boolean addEmailAC)
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

		if (addEmailAC)
		{
			TranslationAction a4 = new TranslationAction(AddAttributeClassActionFactory.NAME, 
					new String[] {"/", "'" + InitializerCommon.NAMING_AC + "'"});
			rules.add(new TranslationRule("true", a4));
			
		}
		
		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);

		return new RegistrationFormBuilder()
				.withName("f1")
				.withDescription("desc")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withTranslationProfile(tp)
				.withCollectComments(true)
				.withFormInformation(new I18nString("formInformation"))
				.withAddedCredentialParam(
						new CredentialRegistrationParam(EngineInitialization.DEFAULT_CREDENTIAL, null, null))
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
				.endGroupParam()
				.withRegistrationCode(nullCode ? null : "123");

	}
	
	protected RegistrationRequest getRequest()
	{
		//note - we could use more easily VerifiableEmailAttribute, but then strict equals wouldn't work
		//when testing returned request.
		Attribute emailA = new Attribute(InitializerCommon.EMAIL_ATTR, VerifiableEmailAttributeSyntax.ID, "/",
				Lists.newArrayList(EmailUtils.convertFromString("foo@example.com").toJsonString()));
		return new RegistrationRequestBuilder()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(emailA)
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abc").toJson()).endCredential()
				.withAddedGroupSelection().withGroup("/B").endGroupSelection()
				.withAddedIdentity(new IdentityParam(X500Identity.ID, "CN=registration test"))
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "test-user"))
				.build();
	}

	protected RegistrationRequest getRequestWithoutOptionalElements()
	{
		return new RegistrationRequestBuilder()
				.withFormId("f1")
				.withComments("comments")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abc").toJson()).endCredential()
				.withAddedAttribute(null)
				.withAddedIdentity(null)
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "test-user"))
				.withAddedGroupSelection(null)
				.build();
	}
	
	protected void checkUpdateOrAdd(RegistrationForm form, String msg, Class<?> exception) throws EngineException
	{
		try
		{
			registrationsMan.addForm(form);
			fail("Added the form with illegal " + msg);
		} catch (Exception e) 
		{
			assertTrue(e.toString(), e.getClass().isAssignableFrom(exception));
		}
		try
		{
			registrationsMan.updateForm(form, false);
			fail("Updated the form with illegal " + msg);
		} catch (Exception e) 
		{
			assertTrue(e.toString(), e.getClass().isAssignableFrom(exception));
		}
	}
	
	protected RegistrationForm initAndCreateForm(boolean nullCode, String autoAcceptCondition) throws EngineException
	{
		return initAndCreateForm(nullCode, autoAcceptCondition, true);
	}
	
	protected void initContents() throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		groupsMan.addGroup(new Group("/A/B"));
		groupsMan.addGroup(new Group("/A/B/C"));
	}
	
	protected RegistrationForm initAndCreateForm(boolean nullCode, String autoAcceptCondition, 
			boolean addEmailAC) throws EngineException
	{
		initContents();
		
		RegistrationForm form = getFormBuilder(nullCode, autoAcceptCondition, addEmailAC).build();

		registrationsMan.addForm(form);
		return form;
	}
}
