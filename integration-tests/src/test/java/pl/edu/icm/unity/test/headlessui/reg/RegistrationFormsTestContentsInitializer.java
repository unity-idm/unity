package pl.edu.icm.unity.test.headlessui.reg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfileBuilder;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.utils.ServerInitializer;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.translation.TranslationProfile;

import com.google.gwt.thirdparty.guava.common.collect.Lists;

@Component
public class RegistrationFormsTestContentsInitializer implements ServerInitializer
{
	public static final String NAME = "registrationInitializer";
	private RegistrationsManagement regMan;
	private IdentitiesManagement idsMan;
	private GroupsManagement groupsMan;
	private RegistrationActionsRegistry registry;


	@Autowired
	public RegistrationFormsTestContentsInitializer(@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure")RegistrationsManagement regMan,
			@Qualifier("insecure") IdentitiesManagement idsMan,
			RegistrationActionsRegistry registry)
	{
		this.groupsMan = groupsMan;
		this.regMan = regMan;
		this.idsMan = idsMan;
		this.registry = registry;
	}

	@Override
	public void run()
	{
		initExtraUser();
		initRegistrationForm();
	}
	
	private void initExtraUser()
	{
		try
		{
			IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "demo-user2");
			Identity base = idsMan.addEntity(toAdd, "Password requirement", EntityState.valid, false);
			groupsMan.addMemberFromParent("/A", new EntityParam(base.getEntityId()));
			PasswordToken pToken = new PasswordToken("the!test2");
			idsMan.setEntityCredential(new EntityParam(base.getEntityId()), "Password credential", 
					pToken.toJson());
		}  catch (EngineException e)
		{
			throw new IllegalStateException("Can add additional user");
		}
	}
	
	private void initRegistrationForm()
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

			TranslationProfile translationProfile = new RegistrationTranslationProfileBuilder(
					registry, "form").
					withAddAttribute("true", "cn", "/", "'val'", AttributeVisibility.full).
					withAutoProcess("true", AutomaticRequestAction.accept).
					build();
			
			RegistrationForm form = new RegistrationFormBuilder()
				.withName("Test")
				.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withAttributeParams(Lists.newArrayList(attrReg))
				.withIdentityParams(Lists.newArrayList(idParam))
				.withAgreements(Lists.newArrayList(agreement))
				.withTranslationProfile(translationProfile)
				.build();

			regMan.addForm(form);
		} catch (EngineException e)
		{
			throw new IllegalStateException("Registration form was not set up");
		}
	}

	@Override
	public String getName()
	{
		return NAME;
	}

}
