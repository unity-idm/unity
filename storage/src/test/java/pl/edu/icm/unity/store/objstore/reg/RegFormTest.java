/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;

public class RegFormTest extends BaseFormTest<RegistrationForm>
{
	@Autowired
	private RegistrationFormDB dao;
	
	@Autowired
	private CredentialRequirementDB crDB;
	
	@Test
	public void usedCredReqRemovalIsRestricted()
	{
		tx.runInTransaction(() -> {
			crDB.create(new CredentialRequirements("credreq", "", Sets.newHashSet("cred")));
			
			RegistrationForm obj = getObject("name1");
			getDAO().create(obj);

			Throwable error = catchThrowable(() -> crDB.delete("credreq"));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void usedCredReqRenameIsPropagated()
	{
		RegistrationForm afterDependencyRename = renameTest(
				new CredentialRequirements("credreq", "", Sets.newHashSet("cred")), 
				new CredentialRequirements("changed", "", Sets.newHashSet("cred")), 
				crDB);
		
		assertThat(afterDependencyRename.getDefaultCredentialRequirement(),
					is("changed"));
	}
	
	@Override
	protected NamedCRUDDAOWithTS<RegistrationForm> getDAO()
	{
		return dao;
	}

	@Override
	protected RegistrationForm getObject(String id)
	{
		List<TranslationRule> rules = Lists.newArrayList(new TranslationRule("condition", 
				new TranslationAction("action", new String[] {"p1"})));
		TranslationProfile translationProfile = new TranslationProfile("tp", "desc", 
				ProfileType.REGISTRATION, rules);
		RegistrationFormBuilder builder = new RegistrationFormBuilder()
				.withName(id)
				.withNotificationsConfiguration()
					.withAdminsNotificationGroup("/notifyGrp")
					.withAcceptedTemplate("template")
					.endNotificationsConfiguration()
				.withDescription("desc")
				.withDefaultCredentialRequirement(
						"credreq")
				.withPubliclyAvailable(true)
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
				.endGroupParam()
				.withRegistrationCode("123");
		return builder.build();
	}

	@Override
	protected RegistrationForm mutateObject(RegistrationForm src)
	{
		RegistrationFormBuilder builder = new RegistrationFormBuilder()
				.withName("name-changed")
				.withDescription("desc2")
				.withDefaultCredentialRequirement(
						"credreq2")
				.withPubliclyAvailable(false)
				.withCollectComments(false)
				.withFormInformation(new I18nString("formInformation2"))
				.withAddedCredentialParam(
						new CredentialRegistrationParam("cred2", "label2", "desc2"))
				.withAddedAgreement(new AgreementRegistrationParam(new I18nString("a2"), true))
				.withAddedIdentityParam()
					.withIdentityType("x500-2")
					.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withRegistrationCode("1");
		return builder.build();
	}
}
