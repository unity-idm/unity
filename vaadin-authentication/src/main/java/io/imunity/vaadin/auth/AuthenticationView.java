/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.server.Page;
import io.imunity.vaadin.auth.outdated.CredentialChangeConfiguration;
import io.imunity.vaadin.auth.outdated.OutdatedCredentialController;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.*;
import io.imunity.vaadin.endpoint.common.api.AssociationAccountWizardProvider;
import io.imunity.vaadin.endpoint.common.api.RegistrationFormDialogProvider;
import io.imunity.vaadin.endpoint.common.api.RegistrationFormsService;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.*;
import static pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMNS_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_WIDTH;

@Route("/authentication")
public class AuthenticationView extends Composite<Div> implements BeforeEnterObserver
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, AuthenticationView.class);
	private final MessageSource msg;
	private final VaadinLogoImageLoader imageAccessService;
	private final LocaleChoiceComponent localeChoice;
	private final VaddinWebLogoutHandler authnProcessor;
	private final ExecutorsService execService;
	private final EntityManagement idsMan;
	private final ObjectFactory<OutdatedCredentialController> outdatedCredentialDialogFactory;
	private final List<AuthenticationFlow> authnFlows;
	private final RegistrationFormsService registrationFormsService;
	private final NotificationPresenter notificationPresenter;
	private final AssociationAccountWizardProvider associationAccountWizardProvider;
	private final RegistrationFormDialogProvider formLauncher;

	private final VaadinEndpointProperties config;
	private final ResolvedEndpoint endpointDescription;
	
	private ColumnInstantAuthenticationScreen authenticationUI;
	private final InteractiveAuthenticationProcessor interactiveAuthnProcessor;
	
	@Autowired
	public AuthenticationView(MessageSource msg, VaadinLogoImageLoader imageAccessService, UnityServerConfiguration cfg,
	                          VaddinWebLogoutHandler authnProcessor,
	                          InteractiveAuthenticationProcessor interactiveProcessor,
	                          ExecutorsService execService, @Qualifier("insecure") EntityManagement idsMan,
	                          ObjectFactory<OutdatedCredentialController> outdatedCredentialDialogFactory,
	                          RegistrationFormsService registrationFormsService,
	                          RegistrationFormDialogProvider formLauncher,
	                          NotificationPresenter notificationPresenter,
	                          AssociationAccountWizardProvider associationAccountWizardProvider)
	{
		this.msg = msg;
		this.localeChoice = new LocaleChoiceComponent(cfg);
		this.authnProcessor = authnProcessor;
		this.interactiveAuthnProcessor = interactiveProcessor;
		this.execService = execService;
		this.idsMan = idsMan;
		this.outdatedCredentialDialogFactory = outdatedCredentialDialogFactory;
		this.imageAccessService = imageAccessService;
		this.registrationFormsService = registrationFormsService;
		this.notificationPresenter = notificationPresenter;
		this.formLauncher = formLauncher;
		this.associationAccountWizardProvider = associationAccountWizardProvider;
		this.endpointDescription = getCurrentWebAppResolvedEndpoint();
		this.config = getCurrentWebAppVaadinProperties();
		this.authnFlows = List.copyOf(getCurrentWebAppAuthenticationFlows());
		this.registrationFormsService.configure(config.getRegistrationConfiguration());
	}
	
	protected void init()
	{
		Function<UnknownRemotePrincipalResult, Dialog> unknownUserDialogProvider = result -> new UnknownUserDialog(
				msg, result, formLauncher, notificationPresenter, associationAccountWizardProvider
		);
		authenticationUI = ColumnInstantAuthenticationScreen.getInstance(msg, imageAccessService, config, endpointDescription,
				new CredentialResetLauncherImpl(),
				this::showRegistration,
				getCurrentWebAppCancelHandler(), idsMan, execService,
				isRegistrationEnabled(), 
				unknownUserDialogProvider,
				Optional.of(localeChoice), authnFlows,
				interactiveAuthnProcessor, notificationPresenter);
		loadInitialState();
		getContent().setSizeFull();
	}
	
	private void loadInitialState() 
	{
		LOG.debug("Loading initial state of authentication UI");
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteRedirectedAuthnResponseProcessingFilter.PostAuthenticationDecissionWithContext postAuthnStepDecision = (RemoteRedirectedAuthnResponseProcessingFilter.PostAuthenticationDecissionWithContext) session
				.getAttribute(RemoteRedirectedAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE);
		if (postAuthnStepDecision != null)
		{
			LOG.debug("Remote authentication result found in session, triggering its processing");
			if (postAuthnStepDecision.triggeringContext.isRegistrationTriggered())
			{
				//note that reg view will clean the session attribute on its own.
				formSelected(postAuthnStepDecision.triggeringContext.form);
			} else
			{
				session.removeAttribute(RemoteRedirectedAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE);
				authenticationUI.initializeAfterReturnFromExternalAuthn(postAuthnStepDecision.decision);
				getContent().removeAll();
				getContent().add(authenticationUI);
			}
		} else
		{
			if (isUserAuthenticatedWithOutdatedCredential())
				showOutdatedCredentialDialog();
			else
			{
				getContent().removeAll();
				getContent().add(authenticationUI);
			}
		}
	}
	
	/**
	 * We may end up in authentication UI also after being properly logged in,
	 * when the credential is outdated. The credential change dialog must be displayed then.
	 */
	private boolean isUserAuthenticatedWithOutdatedCredential()
	{
		WrappedSession vss = VaadinSession.getCurrent().getSession();
		LoginSession ls = (LoginSession) vss.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY);
		return ls != null && ls.isUsedOutdatedCredential();
	}
	
	private void showOutdatedCredentialDialog()
	{
		CredentialChangeConfiguration uiConfig = new CredentialChangeConfiguration(
				config.getValue(VaadinEndpointProperties.AUTHN_LOGO), 
				getFirstColumnWidth(), 
				config.getBooleanValue(VaadinEndpointProperties.CRED_RESET_COMPACT));

		OutdatedCredentialController outdatedCredentialController = outdatedCredentialDialogFactory.getObject();
		outdatedCredentialController.init(uiConfig, authnProcessor, this::resetToFreshAuthenticationScreen);
		getContent().removeAll();
		getContent().add(outdatedCredentialController.getComponent());
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
		getContent().removeAll();
		getContent().add(authenticationUI);
		authenticationUI.reset();
	}
	
	private boolean isRegistrationEnabled()
	{
		try
		{
			return registrationFormsService.isRegistrationEnabled();
		} catch (EngineException e)
		{
			LOG.error("Failed to determine whether registration is enabled or not on "
					+ "authentication screen.", e);
			return false;
		}	}
	
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
			List<RegistrationForm> forms = registrationFormsService.getDisplayedForms();
			if (forms.isEmpty())
			{
				notificationPresenter.showError(msg.getMessage("error"),
						msg.getMessage("RegistrationFormsChooserComponent.noFormsInfo"));
			} else if (forms.size() == 1)
			{
				formSelected(forms.get(0));
			} else
			{
				RegistrationFormsChooserComponent chooser = new RegistrationFormsChooserComponent(
						forms, this::formSelected, this::resetToFreshAuthenticationScreen, msg);
				getContent().removeAll();
				getContent().add(chooser);
			}
		} catch (EngineException e)
		{
			LOG.error("Failed to get displayed forms", e);
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("AuthenticationUI.registrationFormInitError"));
		}
	}

	private void formSelected(RegistrationForm form)
	{
		Component view = registrationFormsService.createRegistrationView(
				form, RegistrationContext.TriggeringMode.manualAtLogin, this::resetToFreshAuthenticationScreen,
				() -> UI.getCurrent().getPage().reload(), () -> UI.getCurrent().getPage().reload()
		);
		getContent().removeAll();
		getContent().add(view);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event)
	{
		if(VaadinService.getCurrentRequest().isUserInRole("USER"))
			UI.getCurrent().getPage().setLocation(VaadinServlet.getCurrent().getServletContext().getContextPath());
		else
			init();
	}

	private class CredentialResetLauncherImpl implements CredentialResetLauncher
	{
		@Override
		public void startCredentialReset(Component credentialResetUI)
		{
			getContent().removeAll();
			getContent().add(credentialResetUI);
		}

		@Override
		public CredentialResetUIConfig getConfiguration()
		{
			return new CredentialResetUIConfig(getLogo(),
					AuthenticationView.this::resetToFreshAuthenticationScreen,
					getFirstColumnWidth() * 2, 
					getFirstColumnWidth(),
					config.getBooleanValue(VaadinEndpointProperties.CRED_RESET_COMPACT));
		}

		private Optional<Image> getLogo()
		{
			String logoURL = config.getAuthnLogo();
			return imageAccessService.loadImageFromUri(logoURL);
		}
	}
}
