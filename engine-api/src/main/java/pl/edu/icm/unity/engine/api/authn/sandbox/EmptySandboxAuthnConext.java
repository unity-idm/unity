/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.sandbox;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

public class EmptySandboxAuthnConext implements SandboxAuthnContext
{
	@Override
	public Optional<RemotelyAuthenticatedPrincipal> getRemotePrincipal()
	{
		return Optional.empty();
	}

	@Override
	public Optional<Exception> getAuthnException()
	{
		return Optional.empty();
	}

	@Override
	public String getLogs()
	{
		return "";
	}
}
