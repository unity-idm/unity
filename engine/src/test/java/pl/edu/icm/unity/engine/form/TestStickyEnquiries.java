/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.form;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseBuilder;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TestStickyEnquiries extends DBIntegrationTestBase
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
		groupsMan.addGroup(new Group("/A/AA"));
		groupsMan.addGroup(new Group("/B"));
		groupsMan.addGroup(new Group("/B/C"));
		groupsMan.addGroup(new Group("/B/C/D"));
	}

	@Test
	public void addStickyEnquiryWithIdentityParamsShouldBeBlocked() throws Exception
	{
		EnquiryForm form = new EnquiryFormBuilder().withName("f1").withTargetGroups(new String[] { "/" })
				.withType(EnquiryType.STICKY)
				.withAddedAttributeParam()
				.withAttributeType(InitializerCommon.CN_ATTR)
				.withGroup("/")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endAttributeParam()
				.withAddedIdentityParam()
				.withIdentityType(UsernameIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.build();

		catchException(enquiryManagement).addEnquiry(form);
		assertThat(caughtException(), isA(WrongArgumentException.class));
	}
	
	@Test
	public void addStickyEnquiryWithCredentialParamsShouldBeBlocked() throws Exception
	{
		EnquiryForm form = new EnquiryFormBuilder().withName("f1").withTargetGroups(new String[] { "/" })
				.withType(EnquiryType.STICKY)
				.withAddedAttributeParam()
				.withAttributeType(InitializerCommon.CN_ATTR)
				.withGroup("/")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endAttributeParam()
				.withAddedCredentialParam(new CredentialRegistrationParam(EngineInitialization.DEFAULT_CREDENTIAL))
				.build();

		catchException(enquiryManagement).addEnquiry(form);
		assertThat(caughtException(), isA(WrongArgumentException.class));
	}

	@Test
	public void shouldUpdateUsersGroup() throws Exception
	{
		initAndCreateEnquiry();
		EnquiryResponse response = new EnquiryResponseBuilder()
			.withFormId("sticky")
			.withAddedGroupSelection()
			.withGroup("/")
			.withGroup("/A")
			.withGroup("/A/AA")
			.endGroupSelection()
			.withAddedGroupSelection()
			.withGroup("/B")
			.endGroupSelection()
			.withAddedAttribute(null)
			.build();
		
		
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid, false);

		groupsMan.addMemberFromParent("/B", new EntityParam(identity));
		groupsMan.addMemberFromParent("/B/C", new EntityParam(identity));
		
		Map<String, GroupMembership> groups = idsMan.getGroups(new EntityParam(identity));
		System.out.println("FFFFF" + groups.keySet());
		assertThat(groups.size(), is(3));
		assertThat(groups.keySet().contains("/B/C"), is(true));
		assertThat(groups.keySet().contains("/A/AA"), is(false));
		
		setupUserContext("tuser", null);
		enquiryManagement.submitEnquiryResponse(response, new RegistrationContext(false, 
				TriggeringMode.manualStandalone));

		setupAdmin();
		groups = idsMan.getGroups(new EntityParam(identity));
		assertThat(groups.size(), is(4));
		assertThat(groups.keySet().contains("/A/AA"), is(true));
		assertThat(groups.keySet().contains("/B/C"), is(false));	
	}

	@Test
	public void shouldUpdateUsersAttribute() throws Exception
	{
		initAndCreateEnquiry();
		EnquiryResponse response = new EnquiryResponseBuilder()
			.withFormId("sticky")
			.withAddedGroupSelection()
			.withGroup("/")
			.withGroup("/A")
			.endGroupSelection()
			.withAddedGroupSelection()
			.withGroup("/B")
			.withGroup("/B/C")
			.endGroupSelection()
			.withAddedAttribute(new Attribute(InitializerCommon.EMAIL_ATTR, VerifiableEmailAttributeSyntax.ID, "/A", Arrays.asList(new VerifiableEmail("email@demo.com").toJsonString())))
			.build();
		
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), CRED_REQ_PASS,
				EntityState.valid, false);

		groupsMan.addMemberFromParent("/B", new EntityParam(identity));
		groupsMan.addMemberFromParent("/B/C", new EntityParam(identity));

		setupUserContext("tuser", null);
		enquiryManagement.submitEnquiryResponse(response,
				new RegistrationContext(false, TriggeringMode.manualStandalone));

		setupAdmin();
		Collection<AttributeExt> allAttributes = attrsMan.getAllAttributes(new EntityParam(identity), false,
				"/A", InitializerCommon.EMAIL_ATTR, false);
		assertThat(allAttributes.size(), is(1));
		VerifiableEmail email = VerifiableEmail
				.fromJsonString(allAttributes.iterator().next().getValues().iterator().next());
		assertThat(email.getValue(), is("email@demo.com"));

	}
	
	@Test
	public void shouldNotAddUsersAttributeFromRemovedGroups() throws Exception
	{
		initAndCreateEnquiry();
		EnquiryResponse response = new EnquiryResponseBuilder()
			.withFormId("sticky")
			.withAddedGroupSelection()
			.withGroup("/")
			.endGroupSelection()
			.withAddedGroupSelection()
			.withGroup("/B")
			.withGroup("/B/C")
			.endGroupSelection()
			.withAddedAttribute(new Attribute(InitializerCommon.EMAIL_ATTR,
						VerifiableEmailAttributeSyntax.ID, "/A",
						Arrays.asList(new VerifiableEmail("email@demo.com").toJsonString())))
			.build();

		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), CRED_REQ_PASS,
				EntityState.valid, false);

		groupsMan.addMemberFromParent("/A", new EntityParam(identity));

		setupUserContext("tuser", null);
		enquiryManagement.submitEnquiryResponse(response,
				new RegistrationContext(false, TriggeringMode.manualStandalone));

		setupAdmin();
		catchException(attrsMan).getAllAttributes(new EntityParam(identity), false, "/A",
				InitializerCommon.EMAIL_ATTR, false);
		assertThat(caughtException(), isA(IllegalGroupValueException.class));

	}
	
	private EnquiryFormBuilder getFormBuilder()
	{
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				new String[] {AutomaticRequestAction.accept.toString()});
	
		
		List<TranslationRule> rules = Lists.newArrayList(new TranslationRule("true", a1));
		
		TranslationProfile translationProfile = new TranslationProfile("form", "", 
				ProfileType.REGISTRATION, rules);
		
		return new EnquiryFormBuilder()
				.withName("sticky")
				.withDescription("desc")
				.withTranslationProfile(translationProfile)
				.withTargetGroups(new String[] {"/"})
				.withType(EnquiryType.STICKY)
				.withAddedAttributeParam()
				.withAttributeType(InitializerCommon.EMAIL_ATTR).withGroup("/A")
				.withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.withShowGroups(true).endAttributeParam()
				.withAddedGroupParam()
				.withGroupPath("/**")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam()
				.withAddedGroupParam()
				.withGroupPath("/B/**")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam();
	}
	
	private EnquiryForm initAndCreateEnquiry() throws EngineException
	{
		EnquiryForm form = getFormBuilder().build();
		enquiryManagement.addEnquiry(form);
		return form;
	}
}
