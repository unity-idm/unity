/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.internal.AuthenticatorsManagement;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnContext;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.authn.AuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.AuthenticatorSetComponent;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
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
@Theme("unityTheme")
@PreserveOnRefresh
public class SandboxUI extends AuthenticationUI 
{
	private static final long serialVersionUID = 5093317898729462049L;
	private static final String DEBUG_ID = "sbox";
	public static final String PROFILE_VALIDATION = "validate";

	private List<AuthenticatorSet> authnList;
	private boolean debug;
	private boolean validationMode;

	@Autowired
	public SandboxUI(UnityMessageSource msg,
			LocaleChoiceComponent localeChoice,
			AuthenticationProcessor authnProcessor,
			InsecureRegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService,
			AuthenticationManagement authnManagement,
			AuthenticatorsManagement authenticatorsManagement,
			AuthenticatorsRegistry authnRegistry)
	{
		super(msg, localeChoice, authnProcessor, formsChooser, formLauncher, execService);
		
		authnList      = getAllVaadinAuthenticators(authnManagement, authnRegistry);
		authenticators = getAuthenticatorUIs(authnList, authenticatorsManagement);
	}

	@Override
	protected AuthenticatorSetComponent buildAuthenticatorSetComponent(EndpointDescription description,
			Map<String, VaadinAuthenticationUI> authenticator, AuthenticatorSet authenticatorSet)
	{
		return new SandboxAuthenticatorSetComponent(authenticator, 
				authenticatorSet, msg, authnProcessor, 
				formLauncher, execService, cancelHandler, description.getRealm());
	}
	
	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties) 
	{
		registrationConfiguration = new EndpointRegistrationConfiguration(false);
		this.description          = new EndpointDescription(description);
		this.description.setAuthenticatorSets(authnList);
	}
	
	@Override
	protected void appInit(VaadinRequest request) 
	{
		setSandboxCallbackForAuthenticators();
		
		super.appInit(request);
		
		setHeaderTitle(msg.getMessage("SandboxUI.headerTitle"));
		
		VaadinRequest vaadinRequest = VaadinService.getCurrentRequest();
		validationMode = vaadinRequest.getParameter(PROFILE_VALIDATION) != null;
		debug = vaadinRequest.getParameter(DEBUG_ID) == null;
		
		if (isProfileValidation())
		{
			setSelectionTitle(msg.getMessage("SandboxUI.selectionTitle.profileValidation"));
		} else
		{
			setSelectionTitle(msg.getMessage("SandboxUI.selectionTitle.profileCreation"));
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

		for (Map<String, VaadinAuthenticationUI> auth : authenticators)
		{
			for (VaadinAuthenticationUI authUI : auth.values())
			{
				authUI.setSandboxAuthnResultCallback(new SandboxAuthnResultCallbackImpl());
			}
		}
	}

	private void cancelAuthentication() 
	{
		if (authenticators != null) 
		{
			for (Map<String, VaadinAuthenticationUI> auth : authenticators)
			{
				for (VaadinAuthenticationUI authUI : auth.values())
				{
					authUI.cancelAuthentication();
				}
			}
		}			
	}

	private List<AuthenticatorSet> getAllVaadinAuthenticators(AuthenticationManagement authnManagement, 
			AuthenticatorsRegistry authnRegistry) 
	{
		ArrayList<AuthenticatorSet> vaadinAuthenticators = new ArrayList<AuthenticatorSet>();
		
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
					AuthenticatorSet authnSet = new AuthenticatorSet(
							Collections.singleton(instance.getId()));
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
	
	private List<Map<String, VaadinAuthenticationUI>> getAuthenticatorUIs(
			List<AuthenticatorSet> authnList, AuthenticatorsManagement authenticatorsMan) 
	{
		List<Map<String, BindingAuthn>> authenticators;
		try
		{
			authenticators = authenticatorsMan.getAuthenticatorUIs(authnList);
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not initialize sandbox UI", e);
		}

		List<Map<String, VaadinAuthenticationUI>> authenticatorUIs = 
				new ArrayList<Map<String, VaadinAuthenticationUI>>();
		for (int i=0; i<authenticators.size(); i++)
		{
			Map<String, VaadinAuthenticationUI> map = new HashMap<String, VaadinAuthenticationUI>();
			Map<String, BindingAuthn> origMap = authenticators.get(i);
			for (Map.Entry<String, BindingAuthn> el: origMap.entrySet())
				map.put(el.getKey(), ((VaadinAuthentication)el.getValue()).createUIInstance());
			authenticatorUIs.add(map);
		}

		return authenticatorUIs;
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
