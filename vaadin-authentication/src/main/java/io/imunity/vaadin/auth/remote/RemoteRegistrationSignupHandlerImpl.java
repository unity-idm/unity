/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.remote;

import com.google.common.collect.Lists;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import io.imunity.vaadin.auth.*;
import io.imunity.vaadin.auth.server.ProxyAuthenticationFilter;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationGrid;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationOption;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationSignupHandler;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.base.registration.ExternalSignupSpec;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.layout.FormParameterElement;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

import java.util.*;
import java.util.stream.Collectors;

class RemoteRegistrationSignupHandlerImpl implements RemoteRegistrationSignupHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RemoteRegistrationSignupHandlerImpl.class);

	private final AuthenticatorSupportService authnSupport;
	private final MessageSource msg;
	private final RegistrationForm form;
	private final ResolvedInvitationParam invitation;
	private final String regCodeProvided;
	private final Map<AuthenticationOptionKey, AuthNOption> remoteSignupOptions;

	RemoteRegistrationSignupHandlerImpl(AuthenticatorSupportService authnSupport, MessageSource msg,
										RegistrationForm form, ResolvedInvitationParam invitation, String regCodeProvided)
	{
		this.authnSupport = authnSupport;
		this.msg = msg;
		this.form = form;
		this.invitation = invitation;
		this.regCodeProvided = regCodeProvided;
		this.remoteSignupOptions = resolveRemoteSignupOptions();
	}

	@Override
	public List<RemoteRegistrationOption> getOptions(FormParameterElement element, boolean enabled)
	{
		ExternalSignupSpec externalSignupSpec = form.getExternalSignupSpec();
		AuthenticationOptionsSelector selector = externalSignupSpec.getSpecs().get(element.getIndex());

		List<AuthNOption> options = getSignupOptions(selector);
		return options.stream().map(authNOption ->
		{
			if (enabled)
			{
				AuthenticationOptionKey authnOptionKey =
						new AuthenticationOptionKey(authNOption.authenticator.getAuthenticatorId(), authNOption.authenticatorUI.getId());
				authNOption.authenticatorUI.setAuthenticationCallback(new SignUpAuthnCallback(form, regCodeProvided, authnOptionKey));
			} else
				((HasEnabled) authNOption.authenticatorUI.getComponent()).setEnabled(false);
			return (RemoteRegistrationOption) authNOption.authenticatorUI::getComponent;
		}).toList();
	}

	@Override
	public RemoteRegistrationGrid getGrid(boolean enabled, int height)
	{
		ExternalSignupGridSpec externalSignupSpec = form.getExternalSignupGridSpec();
		List<AuthNOption> authNOptions = externalSignupSpec.getSpecs().stream()
				.flatMap(selector -> getSignupOptions(selector).stream())
				.toList();

		AuthnsGridWidget authnsGridWidget = new AuthnsGridWidget(authNOptions, msg, new RegGridAuthnPanelFactory(form, regCodeProvided, enabled), height);
		return new RemoteRegistrationGridImpl(msg, authnsGridWidget,authNOptions.isEmpty());
	}

	@Override
	public boolean performAutomaticRemoteSignupIfNeeded()
	{
		Map<AuthenticationOptionKey, AuthNOption> externalSignupOptions = resolveRemoteSignupOptions();
		if (isAutomatedAuthenticationDesired() && externalSignupOptions.size() > 0)
		{
			VaadinServletRequest httpRequest = (VaadinServletRequest) VaadinRequest.getCurrent();
			String requestedAuthnOption = httpRequest.getParameter(PreferredAuthenticationHelper.IDP_SELECT_PARAM);
			if (externalSignupOptions.size() > 1 && requestedAuthnOption == null)
			{
				log.warn("There are multiple remote signup options installed, "
								+ "and automated signup was requested without specifying (with "
								+  "{}) which one should be used. Automatic signup is skipped.",
						PreferredAuthenticationHelper.IDP_SELECT_PARAM);
				return false;
			}
			AuthNOption authnOption = requestedAuthnOption != null ?
					externalSignupOptions.get(AuthenticationOptionKey.valueOf(requestedAuthnOption)) :
					externalSignupOptions.values().iterator().next();
			if (authnOption == null)
			{
				log.warn("Remote signup option {} specified for auto signup is invalid. "
						+ "Automatic signup is skipped.", requestedAuthnOption);
				return false;
			}
			if (authnOption.authenticator instanceof ProxyAuthenticationCapable)
			{
				ProxyAuthenticationCapable proxyAuthn = (ProxyAuthenticationCapable) authnOption.authenticator;
				proxyAuthn.triggerAutomatedUIAuthentication(authnOption.authenticatorUI);
				return true;
			} else
			{
				log.warn("Automatic signup was requested but the selected remote authenticator "
						+ "is not capable of automatic triggering");
				return false;
			}
		}
		return false;
	}

	private boolean isAutomatedAuthenticationDesired()
	{
		VaadinServletRequest httpRequest = (VaadinServletRequest) VaadinRequest.getCurrent();
		String autoLogin = httpRequest.getParameter(ProxyAuthenticationFilter.TRIGGERING_PARAM);
		return Boolean.parseBoolean(autoLogin);
	}

	private List<AuthNOption> getSignupOptions(AuthenticationOptionsSelector selector)
	{
		return remoteSignupOptions.entrySet().stream()
				.filter(e -> selector.matchesAuthnOption(e.getKey()))
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	private Map<AuthenticationOptionKey, AuthNOption> resolveRemoteSignupOptions()
	{
		ExternalSignupSpec signupSpec = form.getExternalSignupSpec();
		Map<AuthenticationOptionKey, AuthNOption> externalSignupOptions = new HashMap<>();
		if (!signupSpec.isEnabled())
			return Map.of();

		Set<String> authnOptions = signupSpec.getSpecs().stream()
				.map(a -> a.authenticatorKey)
				.collect(Collectors.toSet());
		List<AuthenticationFlow> flows = authnSupport.resolveAuthenticationFlows(Lists.newArrayList(authnOptions),
				VaadinAuthentication.NAME);
		Set<AuthenticationOptionsSelector> formSignupSpec = new HashSet<>(signupSpec.getSpecs());
		for (AuthenticationFlow flow : flows)
		{
			for (AuthenticatorInstance authenticator : flow.getFirstFactorAuthenticators())
			{
				VaadinAuthentication vaadinAuthenticator = (VaadinAuthentication) authenticator.getRetrieval();
				String authenticatorKey = vaadinAuthenticator.getAuthenticatorId();
				AuthenticatorStepContext context = new AuthenticatorStepContext(
						InvocationContext.getCurrent().getRealm(), flow, null, AuthenticatorStepContext.FactorOrder.FIRST);
				Collection<VaadinAuthentication.VaadinAuthenticationUI> optionUIInstances =
						vaadinAuthenticator.createUIInstance(VaadinAuthentication.Context.REGISTRATION, context);
				for (VaadinAuthentication.VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
				{
					String optionKey = vaadinAuthenticationUI.getId();
					AuthenticationOptionKey authnOption = new AuthenticationOptionKey(authenticatorKey, optionKey);
					if (formSignupSpec.stream().anyMatch(selector -> selector.matchesAuthnOption(authnOption)))
					{
						AuthNOption signupAuthNOption = new AuthNOption(flow,
								vaadinAuthenticator,  vaadinAuthenticationUI);
						setupExpectedIdentity(vaadinAuthenticationUI, invitation);
						externalSignupOptions.put(authnOption, signupAuthNOption);
					}
				}
			}
		}
		return externalSignupOptions;
	}

	private void setupExpectedIdentity(VaadinAuthentication.VaadinAuthenticationUI vaadinAuthenticationUI, ResolvedInvitationParam invitation)
	{
		if (invitation == null)
			return;
		if (invitation.getAsRegistration().getExpectedIdentity() != null)
			vaadinAuthenticationUI.setExpectedIdentity(invitation.getAsRegistration().getExpectedIdentity());
	}
}
