/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.AuthenticatorsManagement;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificatorFactory;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.webui.authn.AuthNTile;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormsChooserComponent;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;

/**
 * Vaadin UI of the sandbox application using all remote authenticators. Suitable for sandbox authn used in 
 * input translation profile creation wizard and dry run.
 *  
 * @author Roman Krysinski
 */
@org.springframework.stereotype.Component("TranslationProfileSandboxUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
@PreserveOnRefresh
public class TranslationProfileSandboxUI extends SandboxUIBase 
{
	private AuthenticatorsRegistry authnRegistry;
	private AuthenticationManagement authenticationManagement;

	@Autowired
	public TranslationProfileSandboxUI(UnityMessageSource msg,
			LocaleChoiceComponent localeChoice,
			WebAuthenticationProcessor authnProcessor,
			InsecureRegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService,
			AuthenticatorsManagement authenticatorsManagement,
			AuthenticationManagement authenticationManagement,
			AuthenticatorsRegistry authnRegistry, IdentitiesManagement idsMan)
	{
		super(msg, localeChoice, authnProcessor, formsChooser, formLauncher, execService,
				authenticatorsManagement, idsMan);
		this.authenticationManagement = authenticationManagement;
		this.authnRegistry = authnRegistry;
	}

	@Override
	protected void customizeUI()
	{
		AuthNTile firstTile = selectorPanel.getTiles().get(0);
		if (isProfileValidation())
		{
			firstTile.setCaption(msg.getMessage("SandboxUI.selectionTitle.profileValidation"));
		} else
		{
			firstTile.setCaption(msg.getMessage("SandboxUI.selectionTitle.profileCreation"));
		}
	}

	@Override
	protected List<AuthenticationOptionDescription> getAllVaadinAuthenticators(
			List<AuthenticationOption> endpointAuthenticators) 
	{
		ArrayList<AuthenticationOptionDescription> vaadinAuthenticators = new ArrayList<>();
		
		try 
		{
			Collection<AuthenticatorInstance> authnInstances = authenticationManagement.getAuthenticators(
					VaadinAuthentication.NAME);
			for (AuthenticatorInstance instance : authnInstances)
			{
				CredentialVerificatorFactory factory = authnRegistry.getCredentialVerificatorFactory(
						instance.getTypeDescription().getVerificationMethod());
				if (!(factory instanceof LocalCredentialVerificatorFactory)) 
				{
					AuthenticationOptionDescription authnSet = new AuthenticationOptionDescription(
							instance.getId(), null);
					vaadinAuthenticators.add(authnSet);
				}
			}
		} catch (EngineException e) 
		{
			throw new IllegalStateException("Unable to initialize sandbox servlet: failed to get authenticators: " 
					+ e.getMessage(), e);
		}
		
		return vaadinAuthenticators;
	}
}
