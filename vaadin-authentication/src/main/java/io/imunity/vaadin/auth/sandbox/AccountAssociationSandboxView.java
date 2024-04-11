/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.sandbox;

import static io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE;
import static io.imunity.vaadin.endpoint.common.Vaadin2XEndpoint.SANDBOX_PATH_ASSOCIATION;
import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppAuthenticationFlows;
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
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.LocaleChoiceComponent;
import io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter;
import io.imunity.vaadin.endpoint.common.Vaadin82XEndpointProperties;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.layout.WrappedLayout;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

@Route(value = SANDBOX_PATH_ASSOCIATION, layout=WrappedLayout.class)
@AnonymousAllowed
class AccountAssociationSandboxView extends UnityViewComponent implements BeforeEnterObserver
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, AccountAssociationSandboxView.class);
	private final MessageSource msg;
	private final Vaadin82XEndpointProperties config;
	private final ResolvedEndpoint endpointDescription;
	private final LocaleChoiceComponent localeChoice;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final ExecutorsService execService;
	private final EntityManagement idsMan;
	private final List<AuthenticationFlow> authnFlows;
	private final VaadinLogoImageLoader imageAccessService;
	private final NotificationPresenter notificationPresenter;
	private final UnityServerConfiguration unityServerConfiguration;

	@Autowired
	public AccountAssociationSandboxView(MessageSource msg, VaadinLogoImageLoader imageAccessService,
			InteractiveAuthenticationProcessor authnProcessor,
			UnityServerConfiguration cfg,
			ExecutorsService execService,
			@Qualifier("insecure") EntityManagement idsMan,
			NotificationPresenter notificationPresenter, UnityServerConfiguration unityServerConfiguration)
	{
		this.msg = msg;
		this.localeChoice = new LocaleChoiceComponent(cfg);
		this.authnProcessor = authnProcessor;
		this.execService = execService;
		this.idsMan = idsMan;
		this.imageAccessService = imageAccessService;
		this.notificationPresenter = notificationPresenter;
		this.endpointDescription = getCurrentWebAppResolvedEndpoint();
		this.config = getCurrentWebAppVaadinProperties();
		this.authnFlows = List.copyOf(getCurrentWebAppAuthenticationFlows());
		this.unityServerConfiguration = unityServerConfiguration;
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
		String title = msg.getMessage("SandboxUI.authenticateToAssociateAccounts");
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
				title,
				getCurrentWebAppSandboxAuthnRouter(),
				notificationPresenter,
				true, unityServerConfiguration);
		getContent().removeAll();
		getContent().add(components);
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
