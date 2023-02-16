/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint_config;

import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.Vaadin82XEndpointProperties;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

import java.util.Optional;
import java.util.Properties;

public class Vaadin2XWebAppContextWithSandbox extends Vaadin2XWebAppContext
{
	public final Properties properties;
	public final Vaadin82XEndpointProperties vaadin23Properties;
	public final MessageSource messageSource;
	public final ResolvedEndpoint description;
	public final SandboxAuthnRouter sandboxRouter;

	Vaadin2XWebAppContextWithSandbox(Properties properties, Vaadin82XEndpointProperties vaadinEndpointProperties,
	                                 MessageSource messageSource, ResolvedEndpoint description, SandboxAuthnRouter sandboxRouter)
	{
		super(properties, vaadinEndpointProperties, messageSource, description);
		this.properties = properties;
		this.vaadin23Properties = vaadinEndpointProperties;
		this.messageSource = messageSource;
		this.description = description;
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
