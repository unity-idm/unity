/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.OutdatedCredentialDialog;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.forms.reg.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormsChooserComponent;

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
public class AccountAssociationSandboxUI //extends SandboxUIBase 
{
	/*
	@Autowired
	public AccountAssociationSandboxUI(UnityMessageSource msg,
			LocaleChoiceComponent localeChoice,
			WebAuthenticationProcessor authnProcessor,
			RegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService, 
			AuthenticatorSupportManagement authenticatorsManagement,
			AuthenticatorsRegistry authnRegistry, EntityManagement idsMan,
			ObjectFactory<OutdatedCredentialDialog> outdatedCredentialDialogFactory)
	{
		super(msg, localeChoice, authnProcessor, formsChooser, formLauncher, execService, 
				authenticatorsManagement, idsMan, outdatedCredentialDialogFactory);
	}

	@Override
	protected List<AuthenticationFlowDefinition> getAllVaadinAuthenticationFlows(
			List<AuthenticationFlow> endpointAuthenticationFlows)
	{
		ArrayList<AuthenticationFlowDefinition> vaadinAuthenticationFlows = new ArrayList<>();
		for (AuthenticationFlow flow : endpointAuthenticationFlows)
		{

			flow.getFirstFactorAuthenticators().stream()
					.map(b -> b.getRetrieval().getAuthenticatorId())
					.collect(Collectors.toSet());
			AuthenticationFlowDefinition def = new AuthenticationFlowDefinition(
					flow.getId(), flow.getPolicy(),
					flow.getFirstFactorAuthenticators().stream()
							.map(b -> b.getRetrieval().getAuthenticatorId())
							.collect(Collectors.toSet()),
					flow.getSecondFactorAuthenticators().stream()
							.map(b -> b.getRetrieval().getAuthenticatorId())
							.collect(Collectors.toList()));

			vaadinAuthenticationFlows.add(def);
		}
		return vaadinAuthenticationFlows;
	}
	*/
}
