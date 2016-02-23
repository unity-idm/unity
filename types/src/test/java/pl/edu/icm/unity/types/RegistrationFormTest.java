/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;

import org.junit.Test;

import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;


/**
 * Unit tests of {@link RegistrationForm}
 * @author Krzysztof Benedyczak
 */
public class RegistrationFormTest
{
	@Test
	public void mnimalFormSerializationIsIdempotent()
	{
		RegistrationForm minimal = new RegistrationFormBuilder()
			.withName("exForm")
			.withDefaultCredentialRequirement("credReq")
			.build();
		
		String jsonStr = UnityTypesFactory.toJsonString(minimal);
		RegistrationForm minimalParsed = UnityTypesFactory.parse(jsonStr, RegistrationForm.class);
		
		assertThat(minimalParsed, is(minimal));
	}

	@Test
	public void completeFormSerializationIsIdempotent()
	{
		TranslationProfile profile = new TranslationProfile(
				"regProf", "desc", ProfileType.REGISTRATION, new ArrayList<>());
		RegistrationForm complete = new RegistrationFormBuilder()
			.withName("exForm")
			.withDefaultCredentialRequirement("credReq")
			.withAddedAgreement(new AgreementRegistrationParam(new I18nString("agreement"), false))
			.withAddedAttributeParam()
				.withAttributeType("at")
				.withGroup("/A")
			.endAttributeParam()
			.withAddedCredentialParam(new CredentialRegistrationParam("cred", null, null))
			.withAddedGroupParam()
				.withGroupPath("/B")
				.withDescription("some desc")
				.withLabel("label")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
			.endGroupParam()
			.withAddedIdentityParam()
				.withIdentityType("idType")
			.endIdentityParam()
			.withCaptchaLength(4)
			.withDescription("desc")
			.withDisplayedName(new I18nString("dispName"))
			.withCollectComments(true)
			.withFormInformation(new I18nString("fi"))
			.withPubliclyAvailable(true)
			.withNotificationsConfiguration()
				.withAcceptedTemplate("tpl1")
				.withUpdatedTemplate("tpl2")
			.endNotificationsConfiguration()
			.withTranslationProfile(profile)
			.withRegistrationCode("code")
			.build();

		String jsonStr = UnityTypesFactory.toJsonString(complete);
		RegistrationForm completeParsed = UnityTypesFactory.parse(jsonStr, RegistrationForm.class);

		assertThat(completeParsed, is(complete));
	}
}
