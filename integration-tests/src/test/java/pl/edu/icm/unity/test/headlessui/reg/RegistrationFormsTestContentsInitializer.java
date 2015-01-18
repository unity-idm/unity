package pl.edu.icm.unity.test.headlessui.reg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.utils.ServerInitializer;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;

@Component
public class RegistrationFormsTestContentsInitializer implements ServerInitializer
{
	public static final String NAME = "registrationInitializer";
	private RegistrationsManagement regMan;

	@Autowired
	public RegistrationFormsTestContentsInitializer(@Qualifier("insecure")RegistrationsManagement regMan)
	{
		this.regMan = regMan;
	}

	@Override
	public void run()
	{
		try
		{
			RegistrationForm form = new RegistrationForm();
			form.setName("Test");
			form.setCredentialRequirementAssignment(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
			form.setInitialEntityState(EntityState.valid);
			form.setPubliclyAvailable(true);
			
			AttributeRegistrationParam attrReg = new AttributeRegistrationParam();
			attrReg.setAttributeType("email");
			attrReg.setDescription("description");
			attrReg.setGroup("/");
			attrReg.setLabel("Email");
			attrReg.setOptional(true);
			attrReg.setRetrievalSettings(ParameterRetrievalSettings.automaticAndInteractive);
			attrReg.setShowGroups(true);
			attrReg.setUseDescription(true);
			form.setAttributeParams(Collections.singletonList(attrReg));
			
			IdentityRegistrationParam idParam = new IdentityRegistrationParam();
			idParam.setDescription("description");
			idParam.setIdentityType(UsernameIdentity.ID);
			idParam.setLabel("Username");
			idParam.setOptional(true);
			idParam.setRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive);
			form.setIdentityParams(Collections.singletonList(idParam));
			
			Attribute<?> attr = new StringAttribute("cn", "/", AttributeVisibility.full, "val");
			List<Attribute<?>> attrs = new ArrayList<>();
			attrs.add(attr);
			form.setAttributeAssignments(attrs);
			
			AttributeClassAssignment acA = new AttributeClassAssignment();
			acA.setAcName(InitializerCommon.NAMING_AC);
			acA.setGroup("/");
			form.setAttributeClassAssignments(Collections.singletonList(acA));
			
			AgreementRegistrationParam agreement = new AgreementRegistrationParam();
			agreement.setManatory(false);
			agreement.setText(new I18nString("a"));
			form.setAgreements(Collections.singletonList(agreement));
			
			
			form.setCredentialParams(null);
			
			
			
			
			form.setAutoAcceptCondition("true");
			
			regMan.addForm(form);
		} catch (EngineException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public String getName()
	{
		return NAME;
	}

}
