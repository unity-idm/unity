/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection;

import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.imunity.attr.introspection.config.AttrIntrospectionAttributePoliciesConfiguration;
import io.imunity.attr.introspection.config.AttrIntrospectionEndpointProperties;
import io.imunity.attr.introspection.summary.PolicyProcessingSummaryComponent;
import io.imunity.attr.introspection.summary.PolicyProcessingSummaryComponent.PolicyProcessingSummaryComponentFactory;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.auth.sandbox.SandboxAuthenticationScreen;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.CancelHandler;
import io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.Vaadin82XEndpointProperties;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.layout.WrappedLayout;

import org.springframework.beans.factory.annotation.Qualifier;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnNotifier.AuthnResultListener;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE;
import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.*;
import static io.imunity.vaadin.endpoint.common.VaadinEndpointProperties.PREFIX;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_CSS_FILE_NAME;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH;

@Route(value = "/", layout=WrappedLayout.class)
@AnonymousAllowed
class AttrIntrospectionView extends UnityViewComponent
{
	public static final String SANDBOX_CONTEXT_SESSION_ATTRIBUTE = "__sandbox_context";

	private final MessageSource msg;
	private final AuthenticatorSupportService authenticatorSupport;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final ExecutorsService execService;
	private final EntityManagement idsMan;
	private final VaadinLogoImageLoader imageAccessService;
	private final PolicyProcessingSummaryComponentFactory summaryViewFactory;
	private final NotificationPresenter notificationPresenter;
	private final Properties properties;
	private final ResolvedEndpoint endpointDescription;
	private final CancelHandler cancelHandler;
	private final SandboxAuthnRouter sandboxRouter;
	private final AttrIntrospectionAttributePoliciesConfiguration config;
	private final UnityServerConfiguration serverConfiguration;

	AttrIntrospectionView(MessageSource msg,
			InteractiveAuthenticationProcessor authnProcessor, ExecutorsService execService,
			@Qualifier("insecure") EntityManagement idsMan, AuthenticatorSupportService authenticatorSupport,
			VaadinLogoImageLoader imageAccessService, PolicyProcessingSummaryComponentFactory summaryViewFactory,
			NotificationPresenter notificationPresenter, UnityServerConfiguration unityServerConfiguration)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.execService = execService;
		this.idsMan = idsMan;
		this.authenticatorSupport = authenticatorSupport;
		this.imageAccessService = imageAccessService;
		this.summaryViewFactory = summaryViewFactory;
		this.notificationPresenter = notificationPresenter;
		this.properties = getCurrentWebAppContextProperties();
		this.endpointDescription = getCurrentWebAppResolvedEndpoint();
		this.cancelHandler = getCurrentWebAppCancelHandler();
		this.sandboxRouter = getCurrentWebAppSandboxAuthnRouter();
		this.serverConfiguration = unityServerConfiguration;

		config = new AttrIntrospectionAttributePoliciesConfiguration();
		config.fromProperties(new AttrIntrospectionEndpointProperties(properties), msg);

		loadInitialState();
	}

	private void loadInitialState()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteRedirectedAuthnResponseProcessingFilter.PostAuthenticationDecissionWithContext postAuthnStepDecision =
				(RemoteRedirectedAuthnResponseProcessingFilter.PostAuthenticationDecissionWithContext) session
					.getAttribute(DECISION_SESSION_ATTRIBUTE);
		if (postAuthnStepDecision != null)
		{
			showResult((SandboxAuthnContext) session.getAttribute(SANDBOX_CONTEXT_SESSION_ATTRIBUTE));
			session.removeAttribute(DECISION_SESSION_ATTRIBUTE);
			session.removeAttribute(SANDBOX_CONTEXT_SESSION_ATTRIBUTE);
		} else
		{
			createAuthnUI();
		}
	}

	private void createAuthnUI()
	{
		SandboxAuthenticationScreen ui = new SandboxAuthenticationScreen(msg, imageAccessService,
				prepareConfigurationBasingOnEndpoint(properties), endpointDescription, cancelHandler,
				idsMan, execService, authnProcessor, Optional.empty(), getAllRemoteVaadinAuthenticators(),
				"", sandboxRouter, notificationPresenter, true, serverConfiguration);
		getContent().add(ui);
		addSandboxListener();
	}

	private Vaadin82XEndpointProperties prepareConfigurationBasingOnEndpoint(Properties endpointProperties)
	{
		Properties newConfig = new Properties();
		newConfig.putAll(endpointProperties);
		newConfig.setProperty(PREFIX + VaadinEndpointProperties.AUTHN_ADD_ALL, "false");
		return new Vaadin82XEndpointProperties(newConfig, serverConfiguration.getValue(DEFAULT_WEB_CONTENT_PATH),
				serverConfiguration.getValue(DEFAULT_CSS_FILE_NAME));
	}

	protected void addSandboxListener()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		AuthnResultListener listener = event -> session.setAttribute(SANDBOX_CONTEXT_SESSION_ATTRIBUTE, event.ctx);
		sandboxRouter.addListener(listener);
	}

	private void showResult(SandboxAuthnContext ctx)
	{
		if (ctx == null || ctx.getRemotePrincipal().isEmpty())
		{
			createAuthnUI();
			notificationPresenter.showError(msg.getMessage("AttrIntrospection.errorAuthentication"), "");
			return;
		}

		PolicyProcessingSummaryComponent summary = summaryViewFactory.getInstance(config, this::loadInitialState);
		summary.setPolicyProcessingResultForUser(ctx.getRemotePrincipal().get(), getContent());
		VerticalLayout main = new VerticalLayout();
		main.setAlignItems(FlexComponent.Alignment.CENTER);
		main.add(summary);
		getContent().add(main);
	}

	private List<AuthenticationFlow> getAllRemoteVaadinAuthenticators()
	{
		try
		{
			return authenticatorSupport.getRemoteAuthenticatorsAsFlows(VaadinAuthentication.NAME);
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not initialize authn sandbox UI", e);
		}
	}

	@Override
	public String getPageTitle()
	{
		return Vaadin2XWebAppContext.getCurrentWebAppDisplayedName();
	}
}
