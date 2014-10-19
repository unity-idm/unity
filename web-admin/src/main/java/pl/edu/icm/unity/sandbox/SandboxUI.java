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

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.callbacks.SandboxAuthnResultCallback;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.engine.authn.AuthenticatorLoader;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
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
import com.vaadin.ui.Notification;

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

	@Autowired
	public SandboxUI(UnityMessageSource msg,
			LocaleChoiceComponent localeChoice,
			AuthenticationProcessor authnProcessor,
			InsecureRegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService,
			AuthenticationManagement authnManagement,
			AuthenticatorLoader authnLoader,
			DBSessionManager db,
			AuthenticatorsRegistry authnRegistry)
	{
		super(msg, localeChoice, authnProcessor, formsChooser, formLauncher, execService);
		
		authnList      = getAllVaadinAuthenticators(authnManagement, authnRegistry);
		authenticators = getAuthenticatorUIs(authnList, authnLoader, db);
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration regCfg) 
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
		
		if (isProfileValidation())
		{
			setSelectionTitle(msg.getMessage("SandboxUI.selectionTitle.profileValidation"));
		} else
		{
			setSelectionTitle(msg.getMessage("SandboxUI.selectionTitle.profileCreation"));
		}
		
	}
	
	private void setSandboxCallbackForAuthenticators() 
	{
		if (authenticators != null) 
		{
			for (Map<String, VaadinAuthenticationUI> auth : authenticators)
			{
				for (VaadinAuthenticationUI authUI : auth.values())
				{
					authUI.setSandboxAuthnResultCallback(new SandboxAuthnResultCallback() 
					{
						@Override
						public void handleAuthnInput(RemotelyAuthenticatedInput input) 
						{
							if (isPopup())
							{
								fireAuthnEvent(input);
							}
							cancelAuthentication();
							if (isPopup())
							{
								JavaScript.getCurrent().execute("window.close();");
							} else
							{
								Notification.show(input.getTextDump(), Notification.Type.HUMANIZED_MESSAGE);
							}
						}
						
						@Override
						public void handleAuthnError(AuthenticationException e) 
						{
							cancelAuthentication();
							Notification.show(msg.getMessage("very.important.message", e.getMessage()), Notification.Type.ERROR_MESSAGE);
						}

						@Override
						public boolean validateProfile() 
						{
							return isProfileValidation();
						}

						@Override
						public void handleProfileValidation(AuthenticationResult authnResult, StringBuffer capturedLogs) 
						{
							fireAuthnEvent(authnResult, capturedLogs);
							cancelAuthentication();
							if (isPopup()) 
							{
								JavaScript.getCurrent().execute("window.close();");
							}
						}
					});
				}
			}
		}
	}

	private void fireAuthnEvent(RemotelyAuthenticatedInput input) 
	{
		sandboxRouter.fireEvent(new SandboxRemoteAuthnInputEvent(input));
	}

	private void fireAuthnEvent(AuthenticationResult authnResult, StringBuffer capturedLogs)
	{
		sandboxRouter.fireEvent(new SandboxAuthnResultEvent(authnResult, capturedLogs));
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
			Collection<AuthenticatorInstance> authnInstances = authnManagement.getAuthenticators(VaadinAuthentication.NAME);
			for (AuthenticatorInstance instance : authnInstances)
			{
				CredentialVerificatorFactory factory = authnRegistry.getCredentialVerificatorFactory(
						instance.getTypeDescription().getVerificationMethod());
				if (!(factory instanceof LocalCredentialVerificatorFactory)) 
				{
					AuthenticatorSet authnSet = new AuthenticatorSet(Collections.singleton(instance.getId()));
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
			List<AuthenticatorSet> authnList, AuthenticatorLoader authnLoader, DBSessionManager db) 
	{
		SqlSession sql = db.getSqlSession(true);
		List<Map<String, BindingAuthn>> authenticators = null;
		try 
		{
			authenticators = authnLoader.getAuthenticators(authnList, sql);
			sql.rollback();
		} catch (Exception e)
		{
			throw new IllegalStateException("Unable to initialize sandbox servlet: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
		
		ArrayList<Map<String, VaadinAuthenticationUI>> authenticatorUIs = new ArrayList<Map<String, VaadinAuthenticationUI>>();
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
	private boolean isPopup() 
	{
		boolean isPopup = true;
		if (VaadinService.getCurrentRequest().getParameter(DEBUG_ID) != null)
		{
			isPopup = false;
		}
		return isPopup;
	}	
	
	private boolean isProfileValidation()
	{
		boolean isProfileValidation = false;
		if (VaadinService.getCurrentRequest().getParameter(PROFILE_VALIDATION) != null)
		{
			isProfileValidation = true;
		}		
		return isProfileValidation;
	}
}
