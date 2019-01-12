/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.forms.enquiry;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItems;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
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
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
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
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.GroupSelection;
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
		groupsMan.addGroup(new Group("/C"));
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

		Throwable exception = catchThrowable(() -> enquiryManagement.addEnquiry(form));
		assertExceptionType(exception , WrongArgumentException.class);
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
		
		Throwable exception = catchThrowable(() -> enquiryManagement.addEnquiry(form));
		assertExceptionType(exception , WrongArgumentException.class);
	}
	
	@Test
	public void byInvitationStickyEnquiryIsNotReturned() throws Exception
	{
		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid, false);
		EntityParam entityParam = new EntityParam(identity);
		groupsMan.addMemberFromParent("/A", entityParam);
		EnquiryForm form = new EnquiryFormBuilder()
			.withTargetGroups(new String[] {"/A"})
			.withType(EnquiryType.STICKY)
			.withName("s1enquiry")
			.withByInvitationOnly(true)
			.build();
		enquiryManagement.addEnquiry(form);
		
		EnquiryForm form2 = new EnquiryFormBuilder()
				.withTargetGroups(new String[] {"/A"})
				.withType(EnquiryType.STICKY)
				.withName("s2enquiry")
				.withByInvitationOnly(false)
				.build();
			enquiryManagement.addEnquiry(form2);
		
		List<EnquiryForm> pendingEnquires = enquiryManagement.getAvailableStickyEnquires(entityParam);
		
		assertThat(pendingEnquires.size(), is(1));
		assertThat(pendingEnquires.get(0), is(form2));
	}


	@Test
	public void shouldOverwriteSubmitedRequest() throws Exception
	{
		
		initAndCreateEnquiry("false");
		EnquiryResponse response = new EnquiryResponseBuilder()
			.withFormId("sticky")
			.withAddedGroupSelection()
			.withGroup("/")
			.withGroup("/A")
			.endGroupSelection()
			.withAddedGroupSelection()
			.withGroup("/B")
			.endGroupSelection()
			.withAddedAttribute(null)
			.build();
		
		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), 
				CRED_REQ_PASS, EntityState.valid, false);
		
		setupUserContext("tuser", null);

		enquiryManagement.submitEnquiryResponse(response,
				new RegistrationContext(false, TriggeringMode.manualStandalone));

		setupAdmin();

		Set<EnquiryResponseState> responses = enquiryManagement.getEnquiryResponses().stream()
				.filter(e -> e.getRequest().getFormId().equals("sticky")).collect(Collectors.toSet());
		assertThat(responses.size(), is(1));
		EnquiryResponseState res = responses.iterator().next();
		assertThat(res, notNullValue());
		assertThat(res.getRequest().getGroupSelections().size(), is(2));
		assertThat(res.getRequest().getGroupSelections().get(0).getSelectedGroups().get(0), is("/"));
		assertThat(res.getRequest().getGroupSelections().get(0).getSelectedGroups().get(1), is("/A"));
		assertThat(res.getRequest().getGroupSelections().get(1).getSelectedGroups().get(0), is("/B"));

		response.setGroupSelections(Arrays.asList(new GroupSelection(Arrays.asList("/")), new GroupSelection(Arrays.asList("/B"))));
		setupUserContext("tuser", null);

		enquiryManagement.submitEnquiryResponse(response,
				new RegistrationContext(false, TriggeringMode.manualStandalone));

		setupAdmin();
		responses = enquiryManagement.getEnquiryResponses().stream()
				.filter(e -> e.getRequest().getFormId().equals("sticky")).collect(Collectors.toSet());
		assertThat(responses.size(), is(1));

		res = responses.iterator().next();

		assertThat(res, notNullValue());
		assertThat(res.getRequest().getGroupSelections().size(), is(2));
		assertThat(res.getRequest().getGroupSelections().get(0).getSelectedGroups().size(), is(1));
		assertThat(res.getRequest().getGroupSelections().get(0).getSelectedGroups().get(0), is("/"));	
		assertThat(res.getRequest().getGroupSelections().get(1).getSelectedGroups().get(0), is("/B"));
	}
	
	@Test
	public void shouldBlockMultiSelectGroupInSingleSelectGroupParam() throws Exception
	{
		initAndCreateEnquiry("true");
		EnquiryResponse response = new EnquiryResponseBuilder()
				.withFormId("sticky")
				.withAddedGroupSelection()
				.withGroup("/")
				.endGroupSelection()
				.withAddedGroupSelection()
				.withGroup("/B")
				.withGroup("/C")
				.endGroupSelection()
				.withAddedAttribute(null)
				.build();

		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), CRED_REQ_PASS, EntityState.valid,
				false);
		setupUserContext("tuser", null);
		Throwable exception = catchThrowable(() -> enquiryManagement.submitEnquiryResponse(response,
				new RegistrationContext(false, TriggeringMode.manualStandalone)));
		assertExceptionType(exception, IllegalFormContentsException.class);
	}

	@Test
	public void shouldUpdateUsersGroup() throws Exception
	{
		initAndCreateEnquiry("true");
		EnquiryResponse response = new EnquiryResponseBuilder()
			.withFormId("sticky")
			.withAddedGroupSelection()
			.withGroup("/")
			.withGroup("/A")
			.withGroup("/A/AA")
			.withGroup("/C")
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
		assertThat(groups.size(), is(3));
		assertThat(groups.keySet().contains("/B/C"), is(true));
		assertThat(groups.keySet().contains("/A/AA"), is(false));
		
		setupUserContext("tuser", null);
		enquiryManagement.submitEnquiryResponse(response, new RegistrationContext(false, 
				TriggeringMode.manualStandalone));

		setupAdmin();
		groups = idsMan.getGroups(new EntityParam(identity));
		assertThat(groups.size(), is(5));
		assertThat(groups.keySet(), hasItems("/A/AA", "/C"));
		assertThat(groups.keySet(), not(hasItems("/B/C")));	
	}

	@Test
	public void shouldUpdateUsersAttribute() throws Exception
	{
		initAndCreateEnquiry("true");
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
	public void shouldRemoveGroups() throws Exception
	{
		initAndCreateEnquiry("true");
		EnquiryResponse response = new EnquiryResponseBuilder()
			.withFormId("sticky")
			.withAddedGroupSelection()
			.withGroup("/")
			.endGroupSelection()
			.withAddedGroupSelection()
			.withGroup("/B")
			.endGroupSelection()
			.withAddedAttribute(null)
			.build();

		Identity identity = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "tuser"), CRED_REQ_PASS,
				EntityState.valid, false);

		groupsMan.addMemberFromParent("/A", new EntityParam(identity));
		groupsMan.addMemberFromParent("/B", new EntityParam(identity));
		groupsMan.addMemberFromParent("/B/C", new EntityParam(identity));

		setupAdmin();
		Map<String, GroupMembership> groups = idsMan.getGroups(new EntityParam(identity));
		assertThat(groups.size(), is(4));
		assertThat(groups.keySet(), hasItems("/", "/A", "/B", "/B/C"));
		
		setupUserContext("tuser", null);
		enquiryManagement.submitEnquiryResponse(response,
				new RegistrationContext(false, TriggeringMode.manualStandalone));

		setupAdmin();
		groups = idsMan.getGroups(new EntityParam(identity));
		assertThat(groups.size(), is(2));
		assertThat(groups.keySet(), hasItems("/", "/B"));
	}
	
	
	
	@Test
	public void shouldNotAddUsersAttributeFromRemovedGroups() throws Exception
	{
		initAndCreateEnquiry("true");
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
		
		Throwable exception = catchThrowable(() -> attrsMan.getAllAttributes(new EntityParam(identity), false,
				"/A", InitializerCommon.EMAIL_ATTR, false));
		assertExceptionType(exception, IllegalGroupValueException.class);
	}
	
	private EnquiryFormBuilder getFormBuilder(String autoAcceptCondition)
	{
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				new String[] {AutomaticRequestAction.accept.toString()});
		
		String autoAcceptCnd = autoAcceptCondition == null ? "false" : autoAcceptCondition;
		
		List<TranslationRule> rules = Lists.newArrayList(new TranslationRule(autoAcceptCnd, a1));
		
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
				.withMultiselect(true)
				.withGroupPath("/**")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam()
				.withAddedGroupParam()
				.withGroupPath("/B/**")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam();
	}
	
	private EnquiryForm initAndCreateEnquiry(String autoAcceptCondition) throws EngineException
	{
		EnquiryForm form = getFormBuilder(autoAcceptCondition).build();
		enquiryManagement.addEnquiry(form);
		return form;
	}
	
	private void assertExceptionType(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
	}
}
