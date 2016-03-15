/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.AuthenticatorsManagement;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.forms.reg.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormsChooserComponent;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;

/**
 * Vaadin UI of the sandbox application. This UI is using the same authenticators as those configured for
 * the wrapping endpoint. Suitable for account association.
 *  
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component("AccountAssociationSandboxUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
@PreserveOnRefresh
public class AccountAssociationSandboxUI extends SandboxUIBase 
{
	@Autowired
	public AccountAssociationSandboxUI(UnityMessageSource msg,
			LocaleChoiceComponent localeChoice,
			WebAuthenticationProcessor authnProcessor,
			RegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService, AuthenticationManagement authnManagement,
			AuthenticatorsManagement authenticatorsManagement,
			AuthenticatorsRegistry authnRegistry, IdentitiesManagement idsMan)
	{
		super(msg, localeChoice, authnProcessor, formsChooser, formLauncher, execService, 
				authenticatorsManagement, idsMan);
	}

	@Override
	protected List<AuthenticationOptionDescription> getAllVaadinAuthenticators(
			List<AuthenticationOption> endpointAuthenticators)
	{
		ArrayList<AuthenticationOptionDescription> vaadinAuthenticators = new ArrayList<>();
		for (AuthenticationOption ao: endpointAuthenticators)
			vaadinAuthenticators.add(new AuthenticationOptionDescription(
					ao.getPrimaryAuthenticator().getAuthenticatorId(), 
					ao.getMandatory2ndAuthenticator() == null ? null :
						ao.getMandatory2ndAuthenticator().getAuthenticatorId()));
		return vaadinAuthenticators;
	}
}
