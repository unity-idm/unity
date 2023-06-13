/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;

public class EnquiryFormTest extends BaseFormTest<EnquiryForm>
{
	@Autowired
	private EnquiryFormDB dao;
	
	@Override
	protected NamedCRUDDAOWithTS<EnquiryForm> getDAO()
	{
		return dao;
	}

	@Override
	protected EnquiryForm getObject(String id)
	{
		List<TranslationRule> rules = Lists.newArrayList(new TranslationRule("condition", 
				new TranslationAction("action", new String[] {"p1"})));
		TranslationProfile translationProfile = new TranslationProfile("tp", "desc", 
				ProfileType.REGISTRATION, rules);
		EnquiryFormBuilder builder = new EnquiryFormBuilder()
				.withName(id)
				.withNotificationsConfiguration()
					.withAdminsNotificationGroup("/notifyGrp")
					.withAcceptedTemplate("template")
					.endNotificationsConfiguration()
				.withType(EnquiryType.REQUESTED_MANDATORY)
				.withTargetGroups(new String[] {"/"})
				.withDescription("desc")
				.withTranslationProfile(translationProfile)
				.withCollectComments(true)
				.withFormInformation(new I18nString("formInformation"))
				.withAddedCredentialParam(
						new CredentialRegistrationParam("cred", "label", "desc"))
				.withAddedAgreement(new AgreementRegistrationParam(new I18nString("a"), false))
				.withAddedIdentityParam()
					.withIdentityType("x500")
					.withOptional(true)
					.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedAttributeParam()
					.withAttributeType("email")
					.withGroup("/C")
					.withOptional(true)
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withShowGroups(true).endAttributeParam()
				.withAddedGroupParam()
					.withGroupPath("/B")
					.withRetrievalSettings(ParameterRetrievalSettings.automatic)
				.endGroupParam();
		return builder.build();
	}

	@Override
	protected EnquiryForm mutateObject(EnquiryForm src)
	{
		EnquiryFormBuilder builder = new EnquiryFormBuilder()
				.withName("name-changed")
				.withType(EnquiryType.REQUESTED_OPTIONAL)
				.withTargetGroups(new String[] {})
				.withDescription("desc2")
				.withCollectComments(false)
				.withFormInformation(new I18nString("formInformation2"))
				.withAddedCredentialParam(
						new CredentialRegistrationParam("cred2", "label2", "desc2"))
				.withAddedAgreement(new AgreementRegistrationParam(new I18nString("a2"), true))
				.withAddedIdentityParam()
					.withIdentityType("x500-2")
					.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam();
		return builder.build();
	}
}
