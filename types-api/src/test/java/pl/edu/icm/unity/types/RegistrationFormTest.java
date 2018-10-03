/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mockito.Mockito;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.ExternalSignupSpec;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationFormLayouts;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;
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
		
		String jsonStr = JsonUtil.toJsonString(minimal);
		RegistrationForm minimalParsed = JsonUtil.parse(jsonStr, RegistrationForm.class);
		
		assertThat(minimalParsed, is(minimal));
	}

	@Test
	public void completeFormSerializationIsIdempotent()
	{
		RegistrationForm complete = getRegistrationForm();
		MessageSource msg = Mockito.mock(MessageSource.class);
		
		RegistrationFormLayouts layouts = new RegistrationFormLayouts();
		complete.setFormLayouts(layouts);
		layouts.setPrimaryLayout(complete.getEffectivePrimaryFormLayout(msg));
		layouts.setSecondaryLayout(complete.getEffectiveSecondaryFormLayout(msg));
		
		String jsonStr = JsonUtil.toJsonString(complete);
		System.err.println(jsonStr);
		RegistrationForm completeParsed = JsonUtil.parse(jsonStr, RegistrationForm.class);

		assertThat(completeParsed, is(complete));
	}
	
	@Test
	public void shouldValidateLayoutWhenLocalSignupEmbeddedAsButton()
	{
		RegistrationForm complete = getRegistrationForm();
		MessageSource msg = Mockito.mock(MessageSource.class);
		
		RegistrationFormLayouts layouts = new RegistrationFormLayouts();
		layouts.setLocalSignupEmbeddedAsButton(true);
		complete.setFormLayouts(layouts);
		
		layouts.setPrimaryLayout(complete.getEffectivePrimaryFormLayout(msg));
		layouts.setSecondaryLayout(complete.getEffectiveSecondaryFormLayout(msg));
		
		layouts.validate(complete);
		
		Throwable exception = catchThrowable(() -> layouts.validate(complete));
		assertThat(exception).isNull();
	}
	
	private RegistrationForm getRegistrationForm()
	{
		TranslationProfile profile = new TranslationProfile(
				"regProf", "desc", ProfileType.REGISTRATION, new ArrayList<>());
		return new RegistrationFormBuilder()
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
			.withExternalSignupSpec(new ExternalSignupSpec(Lists.newArrayList(
					AuthenticationOptionKey.valueOf("asdf.asdf"), 
					AuthenticationOptionKey.valueOf("asdf1.asdf2"))))
			.withFormLayoutSettings(FormLayoutSettings.builder()
					.withColumnWidth(200)
					.withColumnWidthUnit("EM")
					.withCompactInputs(true)
					.build())
			.build();
	}
}
