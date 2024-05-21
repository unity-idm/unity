/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.sandbox;

import static io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE;
import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppCancelHandler;
import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppDisplayedName;
import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppResolvedEndpoint;
import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppSandboxAuthnRouter;
import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppVaadinProperties;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.auth.sandbox.SandboxAuthenticationScreen;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.LocaleChoiceComponent;
import io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.layout.WrappedLayout;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

@Route(value = SandboxView.SANDBOX_PATH, layout = WrappedLayout.class)
@AnonymousAllowed
public class SandboxView extends UnityViewComponent implements BeforeEnterObserver
{
	public static final String SANDBOX_PATH = "/sandbox";
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, SandboxView.class);
	public static final String PROFILE_VALIDATION = "validate";

	private final MessageSource msg;
	private final AuthenticatorSupportService authenticatorSupport;
	private final VaadinEndpointProperties config;
	private final ResolvedEndpoint endpointDescription;
	private final LocaleChoiceComponent localeChoice;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final ExecutorsService execService;
	private final EntityManagement idsMan;
	private final List<AuthenticationFlow> authnFlows;
	private final VaadinLogoImageLoader imageAccessService;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	SandboxView(MessageSource msg, VaadinLogoImageLoader imageAccessService,
			InteractiveAuthenticationProcessor authnProcessor,
			UnityServerConfiguration cfg,
			ExecutorsService execService,
			@Qualifier("insecure") EntityManagement idsMan,
			AuthenticatorSupportService authenticatorSupport,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.authenticatorSupport = authenticatorSupport;
		this.localeChoice = new LocaleChoiceComponent(cfg);
		this.authnProcessor = authnProcessor;
		this.execService = execService;
		this.idsMan = idsMan;
		this.imageAccessService = imageAccessService;
		this.notificationPresenter = notificationPresenter;
		this.endpointDescription = getCurrentWebAppResolvedEndpoint();
		this.config = getCurrentWebAppVaadinProperties();
		this.authnFlows = getAllRemoteVaadinAuthenticators();
	}


	private void loadInitialState()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteRedirectedAuthnResponseProcessingFilter.PostAuthenticationDecissionWithContext postAuthnStepDecision =
				(RemoteRedirectedAuthnResponseProcessingFilter.PostAuthenticationDecissionWithContext) session
						.getAttribute(DECISION_SESSION_ATTRIBUTE);
		if (postAuthnStepDecision != null)
		{
			log.debug("Remote authentication result found in session, closing");
			session.removeAttribute(DECISION_SESSION_ATTRIBUTE);
			UI.getCurrent().getPage().executeJs("window.close();");
		} else
		{
			createAuthnUI();
		}
	}

	private void createAuthnUI()
	{
		boolean validationMode = isInProfileValidationMode();
		SandboxAuthenticationScreen components = new SandboxAuthenticationScreen(msg,
				imageAccessService,
				config,
				endpointDescription,
				getCurrentWebAppCancelHandler(),
				idsMan,
				execService,
				authnProcessor,
				Optional.of(localeChoice),
				authnFlows,
				getTitle(validationMode),
				getCurrentWebAppSandboxAuthnRouter(),
				notificationPresenter,
				false);
		getContent().removeAll();
		getContent().addClassName(CssClassNames.AUTHN_SCREEN.getName());
		getContent().add(components);
	}

	private boolean isInProfileValidationMode()
	{
		VaadinRequest vaadinRequest = VaadinService.getCurrentRequest();
		return vaadinRequest.getParameter(PROFILE_VALIDATION) != null;
	}

	private String getTitle(boolean validationMode)
	{
		return validationMode ? msg.getMessage("SandboxUI.selectionTitle.profileValidation") :
				msg.getMessage("SandboxUI.selectionTitle.profileCreation");
	}

	private List<AuthenticationFlow> getAllRemoteVaadinAuthenticators()
	{
		try
		{
			return authenticatorSupport.getRemoteAuthenticatorsAsFlows(VaadinAuthentication.NAME);
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not initialize sandbox UI", e);
		}
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event)
	{
		loadInitialState();
	}

	@Override
	public String getPageTitle()
	{
		return getCurrentWebAppDisplayedName();
	}
}
