/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;

public class TestRegistrations extends DBIntegrationTestBase
{
	@Autowired
	private InitializerCommon commonInitializer;
	
	@Test
	public void testRegistrationForms() throws Exception
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		
		RegistrationForm form = new RegistrationForm();
		
		AgreementRegistrationParam agreement = new AgreementRegistrationParam();
		agreement.setManatory(true);
		agreement.setText("a");
		form.setAgreements(Collections.singletonList(agreement));
		
		Attribute<?> attr = new StringAttribute("cn", "/", AttributeVisibility.full, "val");
		List<Attribute<?>> attrs = new ArrayList<>();
		attrs.add(attr);
		form.setAttributeAssignments(attrs);
		
		AttributeClassAssignment acA = new AttributeClassAssignment();
		acA.setAcName(InitializerCommon.NAMING_AC);
		acA.setGroup("/");
		form.setAttributeClassAssignments(Collections.singletonList(acA));
		
		AttributeRegistrationParam attrReg = new AttributeRegistrationParam();
		attrReg.setAttributeType("email");
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
		groupParam.setOptional(true);
		groupParam.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		form.setGroupParams(Collections.singletonList(groupParam));
		
		IdentityRegistrationParam idParam = new IdentityRegistrationParam();
		idParam.setDescription("description");
		idParam.setIdentityType(X500Identity.ID);
		idParam.setLabel("label");
		idParam.setOptional(true);
		idParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden);
		form.setIdentityParams(Collections.singletonList(idParam));
		form.setName("f1");
		form.setPubliclyAvailable(true);
		form.setRegistrationCode("123");
		registrationsMan.addForm(form);
		
		List<RegistrationForm> forms = registrationsMan.getForms();
		assertEquals(1, forms.size());
		assertEquals(form, forms.get(0));
		
		registrationsMan.removeForm("f1", false);
		assertEquals(0, registrationsMan.getForms().size());
		try
		{
			registrationsMan.removeForm("f1", true);
			fail("Removed the same form twice");
		} catch (WrongArgumentException e) {/*ok*/}
		
		registrationsMan.addForm(form);
		
		try
		{
			registrationsMan.addForm(form);
			fail("Added the same form twice");
		} catch (WrongArgumentException e) {/*ok*/}
		
		
		Attribute<?> attrB = new StringAttribute("missing", "/", AttributeVisibility.full, "val");
		List<Attribute<?>> attrsB = new ArrayList<>();
		attrsB.add(attrB);
		form.setAttributeAssignments(attrsB);
		checkUpdateOrAdd(form, "attr");
		form.setAttributeAssignments(null);
		
		AttributeClassAssignment acAB = new AttributeClassAssignment();
		acAB.setAcName("missing");
		acAB.setGroup("/");
		form.setAttributeClassAssignments(Collections.singletonList(acAB));
		checkUpdateOrAdd(form, "AC");
		form.setAttributeClassAssignments(null);
		
		attrReg.setAttributeType("missing");
		form.setAttributeParams(Collections.singletonList(attrReg));
		checkUpdateOrAdd(form, "attr(2)");
		form.setAttributeParams(null);
		
		credParam.setCredentialName("missing");
		form.setCredentialParams(Collections.singletonList(credParam));
		checkUpdateOrAdd(form, "cred");
		form.setCredentialParams(null);
		
		form.setCredentialRequirementAssignment("missing");
		checkUpdateOrAdd(form, "cred req");
		form.setCredentialRequirementAssignment(null);
		checkUpdateOrAdd(form, "credential req (2)");
		
		form.setCredentialRequirementAssignment(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
		
		groupParam.setGroupPath("/missing");
		form.setGroupParams(Collections.singletonList(groupParam));
		checkUpdateOrAdd(form, "group");
		form.setGroupParams(null);
		
		idParam.setIdentityType("missing");
		form.setIdentityParams(Collections.singletonList(idParam));
		checkUpdateOrAdd(form, "id");
		
		//TODO - test correct updates
		//TODO - test removal for forms with requests
	}
	
	private void checkUpdateOrAdd(RegistrationForm form, String msg) throws EngineException
	{
		try
		{
			registrationsMan.addForm(form);
			fail("Added the form with illegal " + msg);
		} catch (WrongArgumentException e) {/*ok*/}
		try
		{
			registrationsMan.updateForm(form, false);
			fail("Updated the form with illegal " + msg);
		} catch (WrongArgumentException e) {/*ok*/}
	}
}
