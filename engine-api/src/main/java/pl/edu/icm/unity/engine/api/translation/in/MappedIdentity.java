/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.in;

import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * {@link IdentityParam} with {@link IdentityEffectMode}.
 * @author K. Benedyczak
 */
public class MappedIdentity
{
	private IdentityEffectMode mode;
	private IdentityParam identity;
	private String credentialRequirement;

	public MappedIdentity(IdentityEffectMode mode, IdentityParam identity,
			String credentialRequirement)
	{
		this.mode = mode;
		this.identity = identity;
		this.credentialRequirement = credentialRequirement;
	}

	public IdentityEffectMode getMode()
	{
		return mode;
	}

	public IdentityParam getIdentity()
	{
		return identity;
	}

	public String getCredentialRequirement()
	{
		return credentialRequirement;
	}

	@Override
	public String toString()
	{
		return "MappedIdentity [mode=" + mode + ", identity=" + identity
				+ ", credentialRequirement=" + credentialRequirement + "]";
	}
}
