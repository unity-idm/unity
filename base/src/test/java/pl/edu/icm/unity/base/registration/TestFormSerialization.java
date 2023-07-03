/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;

public class TestFormSerialization
{
	@Test
	public void deserializationIsIdempotentWithoutURLQueryPrefillConfig()
	{
		RegistrationForm form = new RegistrationFormBuilder()
			.withName("form")
			.withDefaultCredentialRequirement("cr")
			.withAddedAttributeParam()
				.withAttributeType("attr")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
			.endAttributeParam()
			.withAddedIdentityParam()
				.withIdentityType("idType")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
			.endIdentityParam()
			.build();
		
		String jsonString = JsonUtil.toJsonString(form);
		RegistrationForm deserialized = JsonUtil.parse(jsonString, RegistrationForm.class);
		
		assertThat(deserialized).isEqualTo(form);
	}

	@Test
	public void shouldDeserizlizeWithoutURLPrefillElementInJson()
	{
		String jsonString = "{\"Agreements\":[],\"AttributeParams\":[{\"retrievalSettings\":\"interactive\",\"optional\":false,\"attributeType\":\"attr\",\"showGroups\":false,\"useDescription\":false,\"confirmationMode\":\"ON_SUBMIT\"}],\"CollectComments\":false,\"CredentialParams\":[],\"i18nFormInformation\":{\"DefaultValue\":null,\"Map\":{}},\"GroupParams\":[],\"IdentityParams\":[{\"retrievalSettings\":\"interactive\",\"optional\":false,\"identityType\":\"idType\",\"confirmationMode\":\"ON_SUBMIT\"}],\"Name\":\"form\",\"DisplayedName\":{\"DefaultValue\":null,\"Map\":{}},\"TranslationProfile\":{\"ver\":\"2\",\"name\":\"registrationProfile\",\"description\":\"\",\"type\":\"REGISTRATION\",\"mode\":\"DEFAULT\",\"rules\":[]},\"FormLayoutSettings\":{\"compactInputs\":false,\"showCancel\":false,\"columnWidth\":21.0,\"columnWidthUnit\":\"em\",\"logoURL\":null},\"PageTitle\":{\"DefaultValue\":null,\"Map\":{}},\"WrapUpConfig\":[],\"ByInvitationOnly\":false,\"DefaultCredentialRequirement\":\"cr\",\"NotificationsConfiguration\":{\"rejectedTemplate\":null,\"acceptedTemplate\":null,\"updatedTemplate\":null,\"invitationTemplate\":null,\"adminsNotificationGroup\":null,\"sendUserNotificationCopyToAdmin\":false,\"submittedTemplate\":null},\"PubliclyAvailable\":false,\"RegistrationCode\":null,\"CaptchaLength\":0,\"ExternalSignupSpec\":{\"specs\":[]},\"ExternalSignupGridSpec\":{\"specs\":[],\"gridSettings\":null},\"RegistrationFormLayouts\":{\"primaryLayout\":null,\"secondaryLayout\":null,\"localSignupEmbeddedAsButton\":false},\"Title2ndStage\":{\"DefaultValue\":null,\"Map\":{}},\"ShowSignInLink\":false,\"SignInLink\":null,\"AutoLoginToRealm\":null}\n";

		RegistrationForm deserialized = JsonUtil.parse(jsonString, RegistrationForm.class);
		
		assertThat(deserialized.getAttributeParams().get(0).getUrlQueryPrefill()).isNull();
		assertThat(deserialized.getIdentityParams().get(0).getUrlQueryPrefill()).isNull();
	}

	
	@Test
	public void deserializationIsIdempotentWithURLQueryPrefillConfig()
	{
		RegistrationForm form = new RegistrationFormBuilder()
				.withName("form")
				.withDefaultCredentialRequirement("cr")
				.withAddedAttributeParam()
					.withAttributeType("attr")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withURLQueryPrefill(new URLQueryPrefillConfig("attr_p", PrefilledEntryMode.READ_ONLY))
				.endAttributeParam()
				.withAddedIdentityParam()
					.withIdentityType("idType")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withURLQueryPrefill(new URLQueryPrefillConfig("id_p", PrefilledEntryMode.HIDDEN))
				.endIdentityParam()
				.build();
			
		String jsonString = JsonUtil.toJsonString(form);
		RegistrationForm deserialized = JsonUtil.parse(jsonString, RegistrationForm.class);
			
		assertThat(deserialized).isEqualTo(form);
	}
}
