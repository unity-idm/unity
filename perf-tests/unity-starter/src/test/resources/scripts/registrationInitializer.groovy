import com.google.common.collect.Lists

import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction
import pl.edu.icm.unity.engine.server.EngineInitialization
import pl.edu.icm.unity.engine.translation.form.action.AddAttributeActionFactory
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory
import pl.edu.icm.unity.exceptions.EngineException
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.types.I18nString
import pl.edu.icm.unity.types.basic.EntityParam
import pl.edu.icm.unity.types.basic.EntityState
import pl.edu.icm.unity.types.basic.Identity
import pl.edu.icm.unity.types.basic.IdentityParam
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings
import pl.edu.icm.unity.types.registration.RegistrationForm
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder
import pl.edu.icm.unity.types.translation.ProfileType
import pl.edu.icm.unity.types.translation.TranslationAction
import pl.edu.icm.unity.types.translation.TranslationProfile
import pl.edu.icm.unity.types.translation.TranslationRule

if (!isColdStart)
{
	log.debug("Database already initialized with content, skipping...");
	return;
}
	
initExtraUser();
initRegistrationForm();

void initExtraUser()
{
	try
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "demo-user2");
		Identity base = entityManagement.addEntity(toAdd, EntityState.valid, false);
		groupsManagement.addMemberFromParent("/A", new EntityParam(base.getEntityId()));
		PasswordToken pToken = new PasswordToken("the!test2");
		entityCredentialManagement.setEntityCredential(new EntityParam(base.getEntityId()), EngineInitialization.DEFAULT_CREDENTIAL,
				pToken.toJson());
	}  catch (EngineException e)
	{
		throw new IllegalStateException("Can add additional user");
	}
}

void initRegistrationForm()
{
	try
	{
		AttributeRegistrationParam attrReg = new AttributeRegistrationParam();
		attrReg.setAttributeType("email");
		attrReg.setDescription("description");
		attrReg.setGroup("/");
		attrReg.setLabel("Email");
		attrReg.setOptional(true);
		attrReg.setRetrievalSettings(ParameterRetrievalSettings.automaticAndInteractive);
		attrReg.setShowGroups(true);

		IdentityRegistrationParam idParam = new IdentityRegistrationParam();
		idParam.setDescription("description");
		idParam.setIdentityType(UsernameIdentity.ID);
		idParam.setLabel("Username");
		idParam.setOptional(true);
		idParam.setRetrievalSettings(ParameterRetrievalSettings.automatic);

		AgreementRegistrationParam agreement = new AgreementRegistrationParam();
		agreement.setManatory(false);
		agreement.setText(new I18nString("a"));

		TranslationProfile translationProfile = new TranslationProfile("regProfile", "",
				ProfileType.REGISTRATION, Lists.newArrayList(
				new TranslationRule("true", new TranslationAction(
						AddAttributeActionFactory.NAME, "name", "/", "'val'")),
				new TranslationRule("true", new TranslationAction(
						AutoProcessActionFactory.NAME,
						AutomaticRequestAction.accept.toString()))));
		
		RegistrationForm form = new RegistrationFormBuilder()
			.withName("Test")
			.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
			.withPubliclyAvailable(true)
			.withAttributeParams(Lists.newArrayList(attrReg))
			.withIdentityParams(Lists.newArrayList(idParam))
			.withAgreements(Lists.newArrayList(agreement))
			.withTranslationProfile(translationProfile)
			.build();

		registrationsManagement.addForm(form);
	} catch (EngineException e)
	{
		throw new IllegalStateException("Registration form was not set up");
	}
}