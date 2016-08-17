/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.RegistrationMVELContext.RequestSubmitStatus;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.ConfirmationRedirectActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TestRegistrationFormProfile extends DBIntegrationTestBase
{
	@Autowired
	private RegistrationActionsRegistry registry;
	@Autowired
	private AttributeTypeHelper atHelper;
	@Autowired
	private TransactionalRunner tx;
	
	@Test
	public void confirmedAttributeIsPresentInMVELContext() throws EngineException
	{
		aTypeMan.addAttributeType(new AttributeType("email", VerifiableEmailAttributeSyntax.ID));
		
		TranslationAction a1 = new TranslationAction(ConfirmationRedirectActionFactory.NAME, 
				new String[] {"'URL'"});
		
		List<TranslationRule> rules = Lists.newArrayList(
				new TranslationRule("confirmedElementType == 'attribute' "
						+ "&& confirmedElementValue == 'email@example.com' "
						+ "&& confirmedElementName == 'email'", a1));

		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);
		
		RegistrationForm form = new RegistrationFormBuilder()
			.withName("f1")
			.withDefaultCredentialRequirement(
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
			.withTranslationProfile(tp)
			.withAddedAttributeParam()
				.withAttributeType("email")
				.withGroup("/")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
			.endAttributeParam()
			.build();
		
		Attribute confirmed = new VerifiableEmailAttribute("email", "/", "email@example.com");
		
		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withAddedAttribute(confirmed)
				.build();
		
		RegistrationTranslationProfile rprofile = new RegistrationTranslationProfile(tp, 
				registry, atHelper);
		
		RegistrationRequestState requestFull = new RegistrationRequestState();
		requestFull.setStatus(RegistrationRequestStatus.pending);
		requestFull.setRequest(request);
		requestFull.setRequestId(UUID.randomUUID().toString());
		requestFull.setTimestamp(new Date());
		requestFull.setRegistrationContext(new RegistrationContext(true, true, 
				TriggeringMode.manualStandalone));
		
		String postConfirmationRedirectURL = tx.runInTransactionRet(() -> {
			return rprofile.getPostConfirmationRedirectURL(form, requestFull, confirmed, "requestId");
		});
		
		assertThat(postConfirmationRedirectURL, is("URL"));
	}

	@Test
	public void confirmedIdentityIsPresentInMVELContext()
	{
		TranslationAction a1 = new TranslationAction(ConfirmationRedirectActionFactory.NAME, 
				new String[] {"'URL'"});
		
		List<TranslationRule> rules = Lists.newArrayList(
				new TranslationRule("confirmedElementType == 'identity' "
						+ "&& confirmedElementValue == 'email@example.com' "
						+ "&& confirmedElementName == 'email'", a1));

		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);
		
		RegistrationForm form = new RegistrationFormBuilder()
			.withName("f1")
			.withDefaultCredentialRequirement(
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
			.withTranslationProfile(tp)
			.withAddedIdentityParam()
				.withIdentityType(EmailIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
			.endIdentityParam()
			.build();
		
		IdentityParam confirmed = new IdentityParam(EmailIdentity.ID, "email@example.com");
		confirmed.setConfirmationInfo(new ConfirmationInfo(true));
		
		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withAddedIdentity(confirmed)
				.build();
		
		RegistrationTranslationProfile rprofile = new RegistrationTranslationProfile(tp, 
				registry, atHelper);
		
		RegistrationRequestState requestFull = new RegistrationRequestState();
		requestFull.setStatus(RegistrationRequestStatus.pending);
		requestFull.setRequest(request);
		requestFull.setRequestId(UUID.randomUUID().toString());
		requestFull.setTimestamp(new Date());
		requestFull.setRegistrationContext(new RegistrationContext(true, true, 
				TriggeringMode.manualStandalone));
		
		String postConfirmationRedirectURL = tx.runInTransactionRet(() -> {
			return rprofile.getPostConfirmationRedirectURL(form, requestFull, confirmed, "requestId");
		});
		
		assertThat(postConfirmationRedirectURL, is("URL"));
	}

	@Test
	public void objectVersionOfIdentityIsPresentInMVELContext()
	{
		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				new String[] {AutomaticRequestAction.accept.toString()});
		
		List<TranslationRule> rules = Lists.newArrayList(
				new TranslationRule("idsByTypeObj['email'][0].confirmed == true", a1));

		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);
		
		RegistrationForm form = new RegistrationFormBuilder()
			.withName("f1")
			.withDefaultCredentialRequirement(
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
			.withTranslationProfile(tp)
			.withAddedIdentityParam()
				.withIdentityType(EmailIdentity.ID)
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
			.endIdentityParam()
			.build();
		
		IdentityParam confirmed = new IdentityParam(EmailIdentity.ID, "email@example.com");
		confirmed.setConfirmationInfo(new ConfirmationInfo(true));
		
		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withAddedIdentity(confirmed)
				.build();
		
		RegistrationTranslationProfile rprofile = new RegistrationTranslationProfile(tp, 
				registry, atHelper);
		
		RegistrationRequestState requestFull = new RegistrationRequestState();
		requestFull.setStatus(RegistrationRequestStatus.pending);
		requestFull.setRequest(request);
		requestFull.setRequestId(UUID.randomUUID().toString());
		requestFull.setTimestamp(new Date());
		requestFull.setRegistrationContext(new RegistrationContext(true, true, 
				TriggeringMode.manualStandalone));
		
		AutomaticRequestAction action = tx.runInTransactionRet(() -> {
			return rprofile.getAutoProcessAction(form, requestFull, RequestSubmitStatus.submitted);
		});
		
		assertThat(action, is(AutomaticRequestAction.accept));
	}
}
