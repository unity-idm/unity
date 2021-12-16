/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.PREFIX;
import static pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

import io.imunity.attr.introspection.config.AttrIntrospectionAttributePoliciesConfiguration;
import io.imunity.attr.introspection.config.AttrIntrospectionEndpointProperties;
import io.imunity.attr.introspection.summary.PolicyProcessingSummaryComponent;
import io.imunity.attr.introspection.summary.PolicyProcessingSummaryComponent.PolicyProcessingSummaryComponentFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnNotifier.AuthnResultListener;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthenticationScreen;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter.PostAuthenticationDecissionWithContext;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthenticationScreen;

/**
 * The main entry point of the attribute introspection endpoint.
 * 
 * @author P.Piernik
 */
@Component("AttrIntrospectionUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
class AttrIntrospectionUI extends UnityUIBase implements UnityWebUI
{
	public static final String SANDBOX_CONTEXT_SESSION_ATTRIBUTE = "__sandbox_context";

	private final AuthenticatorSupportService authenticatorSupport;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final ExecutorsService execService;
	private final EntityManagement idsMan;
	private final ImageAccessService imageAccessService;
	private final PolicyProcessingSummaryComponentFactory summaryViewFactory;
	private AttrIntrospectionAttributePoliciesConfiguration config;

	AttrIntrospectionUI(MessageSource msg,
			InteractiveAuthenticationProcessor authnProcessor, ExecutorsService execService,
			@Qualifier("insecure") EntityManagement idsMan, AuthenticatorSupportService authenticatorSupport,
			ImageAccessService imageAccessService, PolicyProcessingSummaryComponentFactory summaryViewFactory)
	{
		super(msg);
		this.authnProcessor = authnProcessor;
		this.execService = execService;
		this.idsMan = idsMan;
		this.authenticatorSupport = authenticatorSupport;
		this.imageAccessService = imageAccessService;
		this.summaryViewFactory = summaryViewFactory;
	}

	@Override
	public void configure(ResolvedEndpoint description, List<AuthenticationFlow> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties)
	{
		super.configure(description, authenticators, regCfg, endpointProperties);
		config = new AttrIntrospectionAttributePoliciesConfiguration();
		config.fromProperties(new AttrIntrospectionEndpointProperties(endpointProperties), msg);
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		loadInitialState();
	}

	private void loadInitialState()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		PostAuthenticationDecissionWithContext postAuthnStepDecision = (PostAuthenticationDecissionWithContext) session
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
		AuthenticationScreen ui = new SandboxAuthenticationScreen(msg, imageAccessService,
				prepareConfigurationBasingOnEndpoint(super.config.getProperties()), endpointDescription, cancelHandler,
				idsMan, execService, authnProcessor, Optional.empty(), getAllRemoteVaadinAuthenticators(),
				"", sandboxRouter, true);
		setContent(ui);
		setSizeFull();
		addSandboxListener();
	}

	private static VaadinEndpointProperties prepareConfigurationBasingOnEndpoint(Properties endpointProperties)
	{
		Properties newConfig = new Properties();
		newConfig.putAll(endpointProperties);
		newConfig.setProperty(PREFIX + VaadinEndpointProperties.AUTHN_ADD_ALL, "false");
		newConfig.setProperty(PREFIX + VaadinEndpointProperties.AUTHN_LOGO, "");
		return new VaadinEndpointProperties(newConfig);
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
			throw new IllegalStateException("Unknown remote user");
		}

		PolicyProcessingSummaryComponent summary = summaryViewFactory.getInstance(config);
		summary.setPolicyProcessingResultForUser(ctx.getRemotePrincipal().get());
		VerticalLayout main = new VerticalLayout();
		main.addComponent(summary);
		main.setComponentAlignment(summary, Alignment.TOP_CENTER);
		setContent(main);
	}

	@Override
	public String getUiRootPath()
	{
		return endpointDescription.getEndpoint().getContextAddress();
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
}
