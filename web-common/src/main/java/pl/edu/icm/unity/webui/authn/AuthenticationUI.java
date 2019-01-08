/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMNS_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_WIDTH;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.ColumnInstantAuthenticationScreen;
import pl.edu.icm.unity.webui.authn.outdated.CredentialChangeConfiguration;
import pl.edu.icm.unity.webui.authn.outdated.OutdatedCredentialController;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.forms.reg.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.forms.reg.StandaloneRegistrationView;

/**
 * Vaadin UI of the authentication application. Displays configured authentication UI and 
 * configures generic settings: registration, unknown user dialog and outdated credential dialog.
 *  
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component("AuthenticationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
@PreserveOnRefresh
public class AuthenticationUI extends UnityUIBase implements UnityWebUI
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, AuthenticationUI.class);
	private LocaleChoiceComponent localeChoice;
	private StandardWebAuthenticationProcessor authnProcessor;
	private RegistrationFormsLayoutController registrationFormController;
	private InsecureRegistrationFormLauncher formLauncher;
	private ExecutorsService execService;
	private EntityManagement idsMan;
	private InputTranslationEngine inputTranslationEngine;
	private ObjectFactory<OutdatedCredentialController> outdatedCredentialDialogFactory;
	private List<AuthenticationFlow> authnFlows;
	
	private AuthenticationScreen authenticationUI;
	private boolean resetScheduled;
	
	@Autowired
	public AuthenticationUI(UnityMessageSource msg, LocaleChoiceComponent localeChoice,
			StandardWebAuthenticationProcessor authnProcessor,
			RegistrationFormsLayoutController registrationFormController,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService, @Qualifier("insecure") EntityManagement idsMan,
			InputTranslationEngine inputTranslationEngine,
			ObjectFactory<OutdatedCredentialController> outdatedCredentialDialogFactory)
	{
		super(msg);
		this.localeChoice = localeChoice;
		this.authnProcessor = authnProcessor;
		this.registrationFormController = registrationFormController;
		this.formLauncher = formLauncher;
		this.execService = execService;
		this.idsMan = idsMan;
		this.inputTranslationEngine = inputTranslationEngine;
		this.outdatedCredentialDialogFactory = outdatedCredentialDialogFactory;
	}


	@Override
	public void configure(ResolvedEndpoint description,
			List<AuthenticationFlow> authnFlows,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties genericEndpointConfiguration)
	{
		super.configure(description, authnFlows, registrationConfiguration, genericEndpointConfiguration);
		this.authnFlows = new ArrayList<>(authnFlows);
		this.registrationFormController.configure(registrationConfiguration);
	}
	
	@Override
	protected void appInit(final VaadinRequest request)
	{
		Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider = 
				result -> new UnknownUserDialog(msg, result, 
				formLauncher, sandboxRouter, inputTranslationEngine, 
				getSandboxServletURLForAssociation());
		authenticationUI = new ColumnInstantAuthenticationScreen(msg, config, endpointDescription,
				this::showOutdatedCredentialDialog, 
				new CredentialResetLauncherImpl(),
				this::showRegistration, 
				cancelHandler, idsMan, execService, 
				isRegistrationEnabled(), 
				unknownUserDialogProvider, 
				authnProcessor, localeChoice, authnFlows);
		setContent(authenticationUI);
		setSizeFull();
	}
	
	/**
	 * We may end up in authentication UI also after being properly logged in,
	 * when the credential is outdated. The credential change dialog must be displayed then.
	 * @return
	 */
	private boolean showOutdatedCredentialDialog()
	{
		WrappedSession vss = VaadinSession.getCurrent().getSession();
		LoginSession ls = (LoginSession) vss.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY);
		if (ls != null && ls.isUsedOutdatedCredential())
		{
			CredentialChangeConfiguration uiConfig = new CredentialChangeConfiguration(
					config.getValue(VaadinEndpointProperties.AUTHN_LOGO), 
					getFirstColumnWidth(), 
					config.getBooleanValue(VaadinEndpointProperties.CRED_RESET_COMPACT));
			
			
			OutdatedCredentialController outdatedCredentialController = outdatedCredentialDialogFactory.getObject();
			outdatedCredentialController.init(uiConfig, authnProcessor, this::resetToFreshAuthenticationScreen);
			setContent(outdatedCredentialController.getComponent());
			return true;
		}
		return false;
	}
	
	private float getFirstColumnWidth()
	{
		Iterator<String> columnKeys = config.getStructuredListKeys(AUTHN_COLUMNS_PFX).iterator();
		return columnKeys.hasNext() ? 
				(float)(double)config.getDoubleValue(columnKeys.next()+AUTHN_COLUMN_WIDTH) 
				: VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH;
	}
	
	private void resetToFreshAuthenticationScreen()
	{
		setContent(authenticationUI);
		authenticationUI.reset();
		registrationFormController.resetSessionRegistraionAttribute();
	}

	private void scheduleResetToFreshState()
	{
		resetScheduled = true;
	}

	private void resetToFreshState()
	{
		scheduleResetToFreshState();
		refresh(VaadinRequest.getCurrent());
	}
	
	private boolean isRegistrationEnabled()
	{
		try
		{
			return registrationFormController.isRegistrationEnabled();
		} catch (EngineException e)
		{
			LOG.error("Failed to determine whether registration is enabled or not on "
					+ "authentication screen.", e);
			return false;
		}
	}
	
	private void showRegistration()
	{
		if (config.getRegistrationConfiguration().getExternalRegistrationURL().isPresent())
		{
			String redirectURL = config.getRegistrationConfiguration().getExternalRegistrationURL().get();
			Page.getCurrent().open(redirectURL, null);
		} else
		{
			showRegistrationLayout();
		}
	}
	
	private void showRegistrationLayout()
	{
		try
		{
			List<RegistrationForm> forms = registrationFormController.getDisplayedForms();
			if (forms.isEmpty())
			{
				NotificationPopup.showError(msg.getMessage("error"), 
						msg.getMessage("RegistrationFormsChooserComponent.noFormsInfo"));
			} else if (forms.size() == 1)
			{
				formSelected(forms.get(0));
			} else
			{
				RegistrationFormsChooserComponent chooser = new RegistrationFormsChooserComponent(
						forms, this::formSelected, this::resetToFreshAuthenticationScreen, msg);
				setContent(chooser);
			}
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg.getMessage("error"), 
					msg.getMessage("AuthenticationUI.registrationFormInitError"));
		}
	}
	
	private void formSelected(RegistrationForm form)
	{
		StandaloneRegistrationView view = registrationFormController.createRegistrationView(form);
		registrationFormController.setSessionRegistrationAttribute(view);
		view.enter(TriggeringMode.manualAtLogin, this::resetToFreshAuthenticationScreen, 
				this::scheduleResetToFreshState, this::resetToFreshState);
		setContent(view);
	}
	
	@Override
	protected void refresh(VaadinRequest request) 
	{
		if (resetScheduled)
		{
			resetScheduled = false;
			resetToFreshAuthenticationScreen();
			return;
		}
		
		StandaloneRegistrationView registrationFormView = registrationFormController.getSessionRegistraionAttribute();
		if (registrationFormView != null)
		{
			registrationFormView.refresh(request);
		} else
		{
			authenticationUI.refresh(request);
			showOutdatedCredentialDialog();
		}
	}
	
	
	private class CredentialResetLauncherImpl implements CredentialResetLauncher
	{
		@Override
		public void startCredentialReset(Component credentialResetUI)
		{
			setContent(credentialResetUI);
		}

		@Override
		public CredentialResetUIConfig getConfiguration()
		{
			return new CredentialResetUIConfig(getLogo(), 
					() -> resetToFreshAuthenticationScreen(), 
					getFirstColumnWidth() * 2, 
					getFirstColumnWidth(),
					config.getBooleanValue(VaadinEndpointProperties.CRED_RESET_COMPACT));
		}

		private Optional<Resource> getLogo()
		{
			String logoURL = config.getValue(VaadinEndpointProperties.AUTHN_LOGO);
			
			if (!logoURL.isEmpty())
			{
				Resource logoResource = ImageUtils.getConfiguredImageResource(logoURL);
				return Optional.of(logoResource);
			} else
			{
				return Optional.empty();
			}
				
		}
	}
}
