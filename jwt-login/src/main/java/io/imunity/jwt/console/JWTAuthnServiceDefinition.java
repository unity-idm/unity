/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt.console;

import pl.edu.icm.unity.types.endpoint.Endpoint.EndpointState;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;

class JWTAuthnServiceDefinition implements ServiceDefinition
{
	final DefaultServiceDefinition webAuthzService;
	final DefaultServiceDefinition tokenService;

	JWTAuthnServiceDefinition(DefaultServiceDefinition webAuthzService, DefaultServiceDefinition tokenService)
	{
		this.webAuthzService = webAuthzService;
		this.tokenService = tokenService;
	}

	@Override
	public String getName()
	{
		return webAuthzService.getName();
	}

	@Override
	public EndpointState getState()
	{
		return webAuthzService.getState();
	}

	@Override
	public String getType()
	{
		return webAuthzService.getType();
	}

	@Override
	public String getBinding()
	{
		return webAuthzService.getBinding();
	}

	@Override
	public boolean supportsConfigReloadFromFile()
	{
		return webAuthzService.supportsConfigReloadFromFile() && tokenService.supportsConfigReloadFromFile();
	}
}
