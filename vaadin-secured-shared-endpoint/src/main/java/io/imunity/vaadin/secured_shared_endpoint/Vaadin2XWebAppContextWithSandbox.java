/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.secured_shared_endpoint;

import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.Vaadin82XEndpointProperties;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class Vaadin2XWebAppContextWithSandbox extends Vaadin2XWebAppContext
{
	public final SandboxAuthnRouter sandboxRouter;

	Vaadin2XWebAppContextWithSandbox(Properties properties, Vaadin82XEndpointProperties vaadinEndpointProperties,
	                                 MessageSource messageSource, ResolvedEndpoint description, List<AuthenticationFlow> authenticationFlows,
	                                 SandboxAuthnRouter sandboxRouter)
	{
		super(properties, vaadinEndpointProperties, messageSource, description, authenticationFlows, null);
		this.sandboxRouter = sandboxRouter;
	}

	public static SandboxAuthnRouter getCurrentWebAppSandboxAuthnRouter()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContextWithSandbox) context)
				.map(context -> context.sandboxRouter)
				.orElse(null);
	}
}
