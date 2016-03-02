/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.builders.EnquiryResponseBuilder;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfileBuilder;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.translation.TranslationProfile;

public class TestEnquiries extends DBIntegrationTestBase
{
	@Autowired
	private InitializerCommon commonInitializer;
	
	@Autowired
	private RegistrationActionsRegistry registry;
	
	@Autowired
	private EnquiryManagement enquiryManagement;

	@Test 
	public void addedEnquiryIsReturned() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		List<EnquiryForm> forms = enquiryManagement.getEnquires();
		assertEquals(1, forms.size());
		assertEquals(form, forms.get(0));
	}
	
	@Test 
	public void removedFormIsNotReturned() throws Exception
	{
		initAndCreateEnquiry(null);
		
		enquiryManagement.removeEnquiry("f1");
		
		assertEquals(0, enquiryManagement.getEnquires().size());
	}

	@Test 
	public void missingFormCantBeRemoved() throws Exception
	{
		try
		{
			enquiryManagement.removeEnquiry("missing");
			fail("Removed non existing enquiry");
		} catch (WrongArgumentException e) {/*ok*/}
	}
	
	@Test 
	public void formWithDuplicateNameCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		try
		{
			enquiryManagement.addEnquiry(form);
			fail("Added the same enquiry twice");
		} catch (WrongArgumentException e) {/*ok*/}
	}
	
	@Test 
	public void formWithMissingAttributeCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);

		AttributeRegistrationParam attrReg = form.getAttributeParams().get(0);
		attrReg.setAttributeType("missing");
		testFormBuilder.withAttributeParams(Collections.singletonList(attrReg));
		
		checkUpdateOrAdd(testFormBuilder.build(), "attr(2)", WrongArgumentException.class);
	}
	
	@Test 
	public void formWithMissingCredentialCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);

		CredentialRegistrationParam credParam = form.getCredentialParams().get(0);
		credParam.setCredentialName("missing");
		testFormBuilder.withCredentialParams(Collections.singletonList(credParam));
		
		checkUpdateOrAdd(testFormBuilder.build(), "cred", WrongArgumentException.class);
	}

	
	@Test 
	public void formWithMissingGroupCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);

		GroupRegistrationParam groupParam = form.getGroupParams().get(0);
		groupParam.setGroupPath("/missing");
		testFormBuilder.withGroupParams(Collections.singletonList(groupParam));
		checkUpdateOrAdd(testFormBuilder.build(), "group", IllegalGroupValueException.class);
	}

	@Test 
	public void formWithMissingIdentityCantBeAdded() throws Exception
	{
		EnquiryForm form = initAndCreateEnquiry(null);
		EnquiryFormBuilder testFormBuilder = getFormBuilder(null);
		IdentityRegistrationParam idParam = form.getIdentityParams().get(0);
		idParam.setIdentityType("missing");
		testFormBuilder.withIdentityParams(Collections.singletonList(idParam));
		checkUpdateOrAdd(testFormBuilder.build(), "id", IllegalTypeException.class);
	}

	@Test
	public void artefactsPresentInFormCantBeRemoved() throws Exception
	{
		initAndCreateEnquiry(null);
		
		try
		{
			attrsMan.removeAttributeClass(InitializerCommon.NAMING_AC);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		
		try
		{
			attrsMan.removeAttributeType("cn", true);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		try
		{
			attrsMan.removeAttributeType("email", true);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		
		try
		{
			groupsMan.removeGroup("/B", true);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
	}
	
	
	@Test
	public void requestWithoutOptionalFieldsIsAccepted() throws EngineException
	{
		initAndCreateEnquiry("true");
		EnquiryResponse response = getRequestWithoutOptionalElements();
		enquiryManagement.submitEnquiryResponse(response, new RegistrationContext(true, false, 
				TriggeringMode.manualStandalone));
		
		//TODO check if applied
	}
	
	private EnquiryFormBuilder getFormBuilder(String autoAcceptCondition)
	{
		RegistrationTranslationProfileBuilder profileBuilder = new RegistrationTranslationProfileBuilder(
				registry, "form").
				withAutoProcess(autoAcceptCondition == null ? "false"
						: autoAcceptCondition, 
						AutomaticRequestAction.accept).
				withAddAttribute("true", "cn", "/", "'val'", AttributeVisibility.full).
				withGroupMembership("true", "'/A'");
		
		TranslationProfile translationProfile = profileBuilder.build();
		
		return new EnquiryFormBuilder()
				.withName("f1")
				.withDescription("desc")
				.withTranslationProfile(translationProfile)
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
/*	
	private EnquiryResponse getRequest()
	{
		return new EnquiryResponseBuilder()
				.withFormId("f1")
				.withComments("comments")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						new VerifiableEmailAttribute(
								InitializerCommon.EMAIL_ATTR, "/",
								AttributeVisibility.full, "foo@example.com"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abc").toJson()).endCredential()
				.withAddedGroupSelection().withSelected(true).endGroupSelection()
				.withAddedIdentity()
					.withTypeId(X500Identity.ID)
					.withValue("CN=registration test")
				.endIdentity()
				.withAddedIdentity()
					.withTypeId(UsernameIdentity.ID)
					.withValue("test-user")
				.endIdentity()
				.build();
	}
*/
	private EnquiryResponse getRequestWithoutOptionalElements()
	{
		return new EnquiryResponseBuilder()
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
				.withAddedIdentity()
					.withTypeId(UsernameIdentity.ID)
					.withValue("test-user")
				.endIdentity()
				.withAddedGroupSelection(null)
				.build();
	}
	
	private void checkUpdateOrAdd(EnquiryForm form, String msg, Class<?> exception) throws EngineException
	{
		try
		{
			enquiryManagement.addEnquiry(form);
			fail("Added the form with illegal " + msg);
		} catch (EngineException e) 
		{
			assertTrue(e.toString(), e.getClass().isAssignableFrom(exception));
		}
		try
		{
			enquiryManagement.updateEnquiry(form);
			fail("Updated the form with illegal " + msg);
		} catch (EngineException e) 
		{
			assertTrue(e.toString(), e.getClass().isAssignableFrom(exception));
		}
	}
	
	private EnquiryForm initAndCreateEnquiry(String autoAcceptCondition) throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		
		EnquiryForm form = getFormBuilder(autoAcceptCondition).build();

		enquiryManagement.addEnquiry(form);
		return form;
	}
}
