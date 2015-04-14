/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

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
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnContext;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthNTile;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.SelectedAuthNPanel;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.registration.InsecureRegistrationFormsChooserComponent;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.JavaScript;

/**
 * Vaadin UI of the sandbox application. This servlet is based on AuthenticationUI 
 * but authenticators are overwritten with all available {@link VaadinAuthentication.NAME}.
 *  
 * @author Roman Krysinski
 */
@org.springframework.stereotype.Component("SandboxUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
@PreserveOnRefresh
public class SandboxUI extends AuthenticationUI 
{
	private static final long serialVersionUID = 5093317898729462049L;
	private static final String DEBUG_ID = "sbox";
	public static final String PROFILE_VALIDATION = "validate";

	private List<AuthenticationOptionDescription> authnList;
	private boolean debug;
	private boolean validationMode;

	@Autowired
	public SandboxUI(UnityMessageSource msg,
			LocaleChoiceComponent localeChoice,
			WebAuthenticationProcessor authnProcessor,
			InsecureRegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService,
			AuthenticationManagement authnManagement,
			AuthenticatorsManagement authenticatorsManagement,
			AuthenticatorsRegistry authnRegistry,
			IdentitiesManagement idsMan)
	{
		super(msg, localeChoice, authnProcessor, formsChooser, formLauncher, execService, idsMan);
		
		authnList      = getAllVaadinAuthenticators(authnManagement, authnRegistry);
		authenticators = getAuthenticatorUIs(authnList, authenticatorsManagement);
	}

	@Override
	protected SelectedAuthNPanel createSelectedAuthNPanel()
	{
		return new SandboxSelectedAuthNPanel(msg, authnProcessor, idsMan, formLauncher, 
				execService, cancelHandler, endpointDescription.getRealm());
	}
	
	@Override
	public void configure(EndpointDescription description,
			List<AuthenticationOption> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties) 
	{
		registrationConfiguration = new EndpointRegistrationConfiguration(false);
		this.endpointDescription          = new EndpointDescription(description);
		this.endpointDescription.setAuthenticatorSets(authnList);
		config = new VaadinEndpointProperties(new Properties());
	}
	
	@Override
	protected void appInit(VaadinRequest request) 
	{
		super.appInit(request);
		
		setSandboxCallbackForAuthenticators();
		
		setHeaderTitle(msg.getMessage("SandboxUI.headerTitle"));
		
		VaadinRequest vaadinRequest = VaadinService.getCurrentRequest();
		validationMode = vaadinRequest.getParameter(PROFILE_VALIDATION) != null;
		debug = vaadinRequest.getParameter(DEBUG_ID) == null;
		
		AuthNTile firstTile = selectorPanel.getTiles().get(0);
		if (isProfileValidation())
		{
			firstTile.setCaption(msg.getMessage("SandboxUI.selectionTitle.profileValidation"));
		} else
		{
			firstTile.setCaption(msg.getMessage("SandboxUI.selectionTitle.profileCreation"));
		}
		
	}
	
	private class SandboxAuthnResultCallbackImpl implements SandboxAuthnResultCallback
	{
		@Override
		public void sandboxedAuthenticationDone(SandboxAuthnContext ctx)
		{
			sandboxRouter.fireEvent(new SandboxAuthnEvent(ctx));
			cancelAuthentication();
			if (isDebug()) 
			{
				JavaScript.getCurrent().execute("window.close();");
			}
		}
	}
	
	private void setSandboxCallbackForAuthenticators() 
	{
		if (authenticators == null)
			return;
		AuthNTile firstTile = selectorPanel.getTiles().get(0);
		Collection<VaadinAuthenticationUI> authnUIs = firstTile.getAuthenticators().values();
		for (VaadinAuthenticationUI authUI : authnUIs)
		{
			authUI.setSandboxAuthnResultCallback(new SandboxAuthnResultCallbackImpl());
		}
	}

	private void cancelAuthentication() 
	{
		AuthNTile firstTile = selectorPanel.getTiles().get(0);
		Collection<VaadinAuthenticationUI> authnUIs = firstTile.getAuthenticators().values();
		for (VaadinAuthenticationUI authUI : authnUIs)
		{
			authUI.cancelAuthentication();
		}
	}

	private List<AuthenticationOptionDescription> getAllVaadinAuthenticators(AuthenticationManagement authnManagement, 
			AuthenticatorsRegistry authnRegistry) 
	{
		ArrayList<AuthenticationOptionDescription> vaadinAuthenticators = new ArrayList<>();
		
		try 
		{
			Collection<AuthenticatorInstance> authnInstances = authnManagement.getAuthenticators(
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
	
	private List<AuthenticationOption> getAuthenticatorUIs(
			List<AuthenticationOptionDescription> authnList, AuthenticatorsManagement authenticatorsMan) 
	{
		try
		{
			return authenticatorsMan.getAuthenticatorUIs(authnList);
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not initialize sandbox UI", e);
		}
	}
	
	/**
	 * If sandbox URL contains {@value DEBUG_ID} then it's assumed that
	 * it works in debug mode - just for testing purposes when running sandbox
	 * "by hand" in browser.
	 * 
	 * @return false if debug mode assumed
	 */
	private boolean isDebug() 
	{
		return debug;
	}	
	
	private boolean isProfileValidation()
	{
		return validationMode;
	}
}
