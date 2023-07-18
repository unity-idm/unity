/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to4_0;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;

public class TestInDBUpdateFromSchema4_0
{
	private static final String FORM_WITH_FIDO = """
		{
			"Agreements":[],
			"AttributeParams":[
				{"label":null,"description":null,"retrievalSettings":"automaticOrInteractive","optional":true,"attributeType":"name","group":"/","showGroups":false,"useDescription":false,"confirmationMode":"ON_SUBMIT","urlQueryPrefill":null},
				{"label":null,"description":null,"retrievalSettings":"automaticOrInteractive","optional":true,"attributeType":"avatar","group":"/projects/FBI","showGroups":false,"useDescription":false,"confirmationMode":"ON_SUBMIT","urlQueryPrefill":null},
				{"label":null,"description":null,"retrievalSettings":"interactive","optional":false,"attributeType":"avatarURL","group":"/","showGroups":false,"useDescription":false,"confirmationMode":"ON_SUBMIT","urlQueryPrefill":null},
				{"label":null,"description":null,"retrievalSettings":"interactive","optional":false,"attributeType":"linkable","group":"/","showGroups":false,"useDescription":false,"confirmationMode":"ON_SUBMIT","urlQueryPrefill":null}
			],
			"CollectComments":false,
			"CredentialParams":[
				{"credentialName":"sys:password","label":null,"description":null},
				{"credentialName":"fido","label":null,"description":null}
			],
			"Description":"",
			"i18nFormInformation":{"DefaultValue":null,"Map":{}},
			"GroupParams":[{"label":"Your groups:","description":null,"retrievalSettings":"interactive","groupPath":"/projects/FBI/?*/**","multiSelect":true,"includeGroupsMode":"all"}],
			"IdentityParams":[{"label":null,"description":null,"retrievalSettings":"automaticHidden","optional":true,"identityType":"identifier","confirmationMode":"ON_SUBMIT","urlQueryPrefill":null}],
			"Name":"FBIRegistration",
			"DisplayedName":{"DefaultValue":"FBIRegistration","Map":{"en":"Join FBI"}},
			"TranslationProfile":{"ver":"2","name":"autoProfile","description":"","type":"REGISTRATION","mode":"DEFAULT","rules":[{"condition":{"conditionValue":"validCode == true"},"action":{"name":"autoProcess","parameters":["accept"]}},{"condition":{"conditionValue":"true"},"action":{"name":"addToGroup","parameters":["\\"/projects/FBI\\""]}}]},
			"FormLayoutSettings":{"compactInputs":false,"showCancel":false,"columnWidth":21.0,"columnWidthUnit":"em","logoURL":"https://upload.wikimedia.org/wikipedia/commons/9/9f/FBISeal.png"},
			"PageTitle":{"DefaultValue":null,"Map":{}},
			"WrapUpConfig":[],
			"ByInvitationOnly":false,
			"PolicyAgreements":[],
			"CheckIdentityOnSubmit":false,
			"DefaultCredentialRequirement":"sys:all",
			"NotificationsConfiguration":{"rejectedTemplate":"registrationRequestRejected","acceptedTemplate":"registrationRequestAccepted","updatedTemplate":"registrationRequestUpdated","invitationTemplate":"invitationWithCode","invitationProcessedTemplate":null,"adminsNotificationGroup":null,"sendUserNotificationCopyToAdmin":false,"submittedTemplate":null},
			"PubliclyAvailable":true,
			"RegistrationCode":null,
			"CaptchaLength":0,
			"ExternalSignupSpec":{"specs":[]},
			"ExternalSignupGridSpec":{"specs":[],"gridSettings":{"searchable":true,"height":8}},
			"RegistrationFormLayouts":{"primaryLayout":null,"secondaryLayout":null,"localSignupEmbeddedAsButton":false},
			"Title2ndStage":{"DefaultValue":null,"Map":{"en":"Provide your details"}},
			"ShowSignInLink":false,
			"SignInLink":"",
			"AutoLoginToRealm":"",
			"SwitchToEnquiryInfo":{"DefaultValue":null,"Map":{"en":"You are registering a new account for ${invitationEmail}. If you already have an account here, you can ${switch_start}switch to use it${switch_end}."}}
		}
	""";
	private static final String FORM_WITHOUT_FIDO = """
		{
			"Agreements":[],
			"AttributeParams":[
				{"label":null,"description":null,"retrievalSettings":"automaticOrInteractive","optional":false,"attributeType":"name","group":"/","showGroups":false,"useDescription":false,"confirmationMode":"ON_SUBMIT","urlQueryPrefill":null},
				{"label":null,"description":null,"retrievalSettings":"automaticOrInteractive","optional":true,"attributeType":"firstname","group":"/projects/univ","showGroups":false,"useDescription":false,"confirmationMode":"ON_SUBMIT","urlQueryPrefill":null},
				{"label":null,"description":null,"retrievalSettings":"automaticOrInteractive","optional":true,"attributeType":"surname","group":"/projects/univ","showGroups":false,"useDescription":false,"confirmationMode":"ON_SUBMIT","urlQueryPrefill":null}],
				"CollectComments":false,"CredentialParams":[{"credentialName":"sys:password","label":null,"description":null}],
				"Description":"","i18nFormInformation":{"DefaultValue":null,"Map":{}},
				"GroupParams":[{"label":"Your groups:","description":null,"retrievalSettings":"interactive","groupPath":"/projects/univ/?*/**","multiSelect":true,"includeGroupsMode":"all"}],
				"IdentityParams":[
					{"label":null,"description":null,"retrievalSettings":"automaticOrInteractive","optional":false,"identityType":"email","confirmationMode":"ON_SUBMIT","urlQueryPrefill":null},
					{"label":null,"description":null,"retrievalSettings":"automaticHidden","optional":true,"identityType":"identifier","confirmationMode":"ON_SUBMIT","urlQueryPrefill":null},
					{"label":null,"description":null,"retrievalSettings":"interactive","optional":false,"identityType":"fidoUserHandle","confirmationMode":"ON_SUBMIT","urlQueryPrefill":null}
				],
				"Name":"UniversityRegistration",
				"DisplayedName":{"DefaultValue":"UniversityRegistration","Map":{"en":"Join University"}},
				"TranslationProfile":{"ver":"2","name":"autoProfile","description":"","type":"REGISTRATION","mode":"DEFAULT","rules":[{"condition":{"conditionValue":"validCode == true"},"action":{"name":"autoProcess","parameters":["accept"]}},{"condition":{"conditionValue":"true"},"action":{"name":"addToGroup","parameters":["\\"/projects/univ\\""]}}]},
				"FormLayoutSettings":{"compactInputs":false,"showCancel":false,"columnWidth":21.0,"columnWidthUnit":"em","logoURL":"https://previews.123rf.com/images/captainvector/captainvector1703/captainvector170309945/74377645-university-logo-element.jpg"},
				"PageTitle":{"DefaultValue":null,"Map":{}},
				"WrapUpConfig":[],
				"ByInvitationOnly":false,
				"PolicyAgreements":[{"documentsIdsToAccept":[1],"presentationType":"CHECKBOX_SELECTED","text":{"DefaultValue":null,"Map":{}}}],
				"CheckIdentityOnSubmit":false,
				"DefaultCredentialRequirement":"sys:all",
				"NotificationsConfiguration":{"rejectedTemplate":"registrationRequestRejected","acceptedTemplate":"registrationRequestAccepted","updatedTemplate":"registrationRequestUpdated","invitationTemplate":"invitationWithCode","invitationProcessedTemplate":null,"adminsNotificationGroup":null,"sendUserNotificationCopyToAdmin":false,"submittedTemplate":null},
				"PubliclyAvailable":true,
				"RegistrationCode":null,
				"CaptchaLength":0,
				"ExternalSignupSpec":{"specs":[]},
				"ExternalSignupGridSpec":{"specs":[],"gridSettings":{"searchable":true,"height":8}},
				"RegistrationFormLayouts":{"primaryLayout":null,"secondaryLayout":null,"localSignupEmbeddedAsButton":false},
				"Title2ndStage":{"DefaultValue":null,"Map":{"en":"Provide your details"}},
				"ShowSignInLink":true,
				"SignInLink":"",
				"AutoLoginToRealm":"",
				"SwitchToEnquiryInfo":{"DefaultValue":null,"Map":{"en":"You are registering a new account for ${invitationEmail}. If you already have an account here, you can ${switch_start}switch to use it${switch_end}."}}
			}
		""";
	private static final String FIXED_FORM = """
		{
			"Agreements":[],
			"AttributeParams":[
				{"label":null,"description":null,"retrievalSettings":"automaticOrInteractive","optional":false,"attributeType":"name","group":"/","showGroups":false,"useDescription":false,"confirmationMode":"ON_SUBMIT","urlQueryPrefill":null},
				{"label":null,"description":null,"retrievalSettings":"automaticOrInteractive","optional":true,"attributeType":"firstname","group":"/projects/univ","showGroups":false,"useDescription":false,"confirmationMode":"ON_SUBMIT","urlQueryPrefill":null},
				{"label":null,"description":null,"retrievalSettings":"automaticOrInteractive","optional":true,"attributeType":"surname","group":"/projects/univ","showGroups":false,"useDescription":false,"confirmationMode":"ON_SUBMIT","urlQueryPrefill":null}
			],
			"CollectComments":false,
			"CredentialParams":[{"credentialName":"sys:password","label":null,"description":null}],
			"Description":"",
			"i18nFormInformation":{"DefaultValue":null,"Map":{}},
			"GroupParams":[
				{"label":"Your groups:","description":null,"retrievalSettings":"interactive","groupPath":"/projects/univ/?*/**","multiSelect":true,"includeGroupsMode":"all"}
			],
			"IdentityParams":[
				{"label":null,"description":null,"retrievalSettings":"automaticOrInteractive","optional":false,"identityType":"email","confirmationMode":"ON_SUBMIT","urlQueryPrefill":null},
				{"label":null,"description":null,"retrievalSettings":"automaticHidden","optional":true,"identityType":"identifier","confirmationMode":"ON_SUBMIT","urlQueryPrefill":null}
			],
			"Name":"UniversityRegistration",
			"DisplayedName":{"DefaultValue":"UniversityRegistration","Map":{"en":"Join University"}},
			"TranslationProfile":{"ver":"2","name":"autoProfile","description":"","type":"REGISTRATION","mode":"DEFAULT","rules":[{"condition":{"conditionValue":"validCode == true"},"action":{"name":"autoProcess","parameters":["accept"]}},{"condition":{"conditionValue":"true"},"action":{"name":"addToGroup","parameters":["\\"/projects/univ\\""]}}]},
			"FormLayoutSettings":{"compactInputs":false,"showCancel":false,"columnWidth":21.0,"columnWidthUnit":"em","logoURL":"https://previews.123rf.com/images/captainvector/captainvector1703/captainvector170309945/74377645-university-logo-element.jpg"},
			"PageTitle":{"DefaultValue":null,"Map":{}},
			"WrapUpConfig":[],
			"ByInvitationOnly":false,
			"PolicyAgreements":[{"documentsIdsToAccept":[1],"presentationType":"CHECKBOX_SELECTED","text":{"DefaultValue":null,"Map":{}}}],
			"CheckIdentityOnSubmit":false,
			"DefaultCredentialRequirement":"sys:all",
			"NotificationsConfiguration":{"rejectedTemplate":"registrationRequestRejected","acceptedTemplate":"registrationRequestAccepted","updatedTemplate":"registrationRequestUpdated","invitationTemplate":"invitationWithCode","invitationProcessedTemplate":null,"adminsNotificationGroup":null,"sendUserNotificationCopyToAdmin":false,"submittedTemplate":null},
			"PubliclyAvailable":true,
			"RegistrationCode":null,
			"CaptchaLength":0,
			"ExternalSignupSpec":{"specs":[]},
			"ExternalSignupGridSpec":{"specs":[],"gridSettings":{"searchable":true,"height":8}},
			"RegistrationFormLayouts":{"primaryLayout":null,"secondaryLayout":null,"localSignupEmbeddedAsButton":false},
			"Title2ndStage":{"DefaultValue":null,"Map":{"en":"Provide your details"}},
			"ShowSignInLink":true,
			"SignInLink":"",
			"AutoLoginToRealm":"",
			"SwitchToEnquiryInfo":{"DefaultValue":null,"Map":{"en":"You are registering a new account for ${invitationEmail}. If you already have an account here, you can ${switch_start}switch to use it${switch_end}."}}
		}
	""";

	@Test
	public void shouldRemoveUnwantedIdentityTypes() throws IOException
	{
		RegistrationFormDB registrationFormDB = mock(RegistrationFormDB.class);
		RegistrationForm first = new RegistrationForm((ObjectNode) Constants.MAPPER.readTree(FORM_WITH_FIDO));
		RegistrationForm second = new RegistrationForm((ObjectNode) Constants.MAPPER.readTree(FORM_WITHOUT_FIDO));

		when(registrationFormDB.getAll()).thenReturn(List.of(first, second));
		InDBUpdateFromSchema18 hotfix = new InDBUpdateFromSchema18(registrationFormDB);

		hotfix.update();

		RegistrationForm toUpdate = new RegistrationForm((ObjectNode) Constants.MAPPER.readTree(FIXED_FORM));
		verify(registrationFormDB).update(toUpdate);
	}
}
