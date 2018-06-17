/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.google.common.collect.Sets;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;

import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.OutdatedCredentialDialog;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.tile.AuthNTile;
import pl.edu.icm.unity.webui.forms.reg.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormsChooserComponent;

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
public class TranslationProfileSandboxUI //extends SandboxUIBase 
{
	private AuthenticatorsRegistry authnRegistry;
	private AuthenticatorManagement authenticationManagement;
/*
	@Autowired
	public TranslationProfileSandboxUI(UnityMessageSource msg,
			LocaleChoiceComponent localeChoice,
			ExecutorsService execService,
			AuthenticatorSupportManagement authenticatorsManagement,
			AuthenticatorManagement authenticationManagement,
			AuthenticatorsRegistry authnRegistry, EntityManagement idsMan,
			ObjectFactory<OutdatedCredentialDialog> outdatedCredentialDialogFactory)
	{
		super(msg, localeChoice, authnProcessor, formsChooser, formLauncher, execService,
				authenticatorsManagement, idsMan, outdatedCredentialDialogFactory);
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
	protected List<AuthenticationFlowDefinition> getAllVaadinAuthenticationFlows(
			List<AuthenticationFlow> endpointAuthenticators) 
	{
		ArrayList<AuthenticationFlowDefinition> vaadinAuthenticators = new ArrayList<>();
		
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
					AuthenticationFlowDefinition authnFlow = new AuthenticationFlowDefinition(
							
							instance.getId(), Policy.NEVER, Sets.newHashSet(instance.getId()));

					vaadinAuthenticators.add(authnFlow);
				}
			}
		} catch (EngineException e) 
		{
			throw new IllegalStateException("Unable to initialize sandbox servlet: failed to get authenticators: " 
					+ e.getMessage(), e);
		}
		
		return vaadinAuthenticators;
	}
	*/
}
