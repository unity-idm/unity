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
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.ColumnInstantAuthenticationScreen;
import pl.edu.icm.unity.webui.authn.column.RegistrationInfoProvider;
import pl.edu.icm.unity.webui.authn.outdated.CredentialChangeConfiguration;
import pl.edu.icm.unity.webui.authn.outdated.OutdatedCredentialController;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.forms.reg.InsecureRegistrationFormLauncher;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormChooserDialog;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormsChooserComponent;

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
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationUI.class);
	private LocaleChoiceComponent localeChoice;
	private StandardWebAuthenticationProcessor authnProcessor;
	private RegistrationFormsChooserComponent formsChooser;
	private InsecureRegistrationFormLauncher formLauncher;
	private ExecutorsService execService;
	private EndpointRegistrationConfiguration registrationConfiguration;
	private EntityManagement idsMan;
	private InputTranslationEngine inputTranslationEngine;
	private ObjectFactory<OutdatedCredentialController> outdatedCredentialDialogFactory;
	private List<AuthenticationFlow> authnFlows;
	
	private AuthenticationScreen authenticationUI;
	private RegistrationInfoProvider registrationInfoProvider;
	
	@Autowired
	public AuthenticationUI(UnityMessageSource msg, LocaleChoiceComponent localeChoice,
			StandardWebAuthenticationProcessor authnProcessor,
			RegistrationFormsChooserComponent formsChooser,
			InsecureRegistrationFormLauncher formLauncher,
			ExecutorsService execService, @Qualifier("insecure") EntityManagement idsMan,
			InputTranslationEngine inputTranslationEngine,
			ObjectFactory<OutdatedCredentialController> outdatedCredentialDialogFactory,
			RegistrationInfoProvider registrationInfoProvider)
	{
		super(msg);
		this.localeChoice = localeChoice;
		this.authnProcessor = authnProcessor;
		this.formsChooser = formsChooser;
		this.formLauncher = formLauncher;
		this.execService = execService;
		this.idsMan = idsMan;
		this.inputTranslationEngine = inputTranslationEngine;
		this.outdatedCredentialDialogFactory = outdatedCredentialDialogFactory;
		this.registrationInfoProvider = registrationInfoProvider;
	}


	@Override
	public void configure(ResolvedEndpoint description,
			List<AuthenticationFlow> authnFlows,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties genericEndpointConfiguration)
	{
		super.configure(description, authnFlows, registrationConfiguration, genericEndpointConfiguration);
		this.authnFlows = new ArrayList<>(authnFlows);
		this.registrationConfiguration = registrationConfiguration;
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
				this::showRegistrationDialog, 
				cancelHandler, idsMan, execService, 
				isRegistrationEnabled(), 
				unknownUserDialogProvider, 
				authnProcessor, localeChoice, authnFlows, registrationInfoProvider);
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
	}
	
	private boolean isRegistrationEnabled()
	{
		if (!registrationConfiguration.isShowRegistrationOption())
			return false;
		if (registrationConfiguration.getEnabledForms().size() > 0)
			formsChooser.setAllowedForms(registrationConfiguration.getEnabledForms());
		formsChooser.initUI(TriggeringMode.manualAtLogin);
		if (formsChooser.getDisplayedForms().size() == 0)
			return false;
		return true;
	}
	
	private void showRegistrationDialog()
	{
		if (formsChooser.getDisplayedForms().size() == 1)
		{
			RegistrationForm form = formsChooser.getDisplayedForms().get(0);
			formLauncher.showRegistrationDialog(form, 
					RemotelyAuthenticatedContext.getLocalContext(),
					TriggeringMode.manualAtLogin, 
					error -> handleRegistrationError(error, form.getName()));
		} else
		{
			RegistrationFormChooserDialog chooser = new RegistrationFormChooserDialog(
				msg, msg.getMessage("RegistrationFormChooserDialog.selectForm"), formsChooser);
			chooser.show();
		}
	}
	
	private void handleRegistrationError(Exception e, String formName)
	{
		log.info("Can't initialize registration form '" + formName + "' UI. "
				+ "It can be fine in some cases, but often means "
				+ "that the form should not be marked "
				+ "as public or its configuration is invalid: " + e.toString());
		if (log.isDebugEnabled())
			log.debug("Deatils: ", e);
		NotificationPopup.showError(msg.getMessage("error"), 
				msg.getMessage("AuthenticationUI.registrationFormInitError"));
	}
	
	@Override
	protected void refresh(VaadinRequest request) 
	{
		authenticationUI.refresh(request);
		showOutdatedCredentialDialog();
	}
}
