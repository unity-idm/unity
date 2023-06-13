/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import java.util.Objects;

/**
 *  Describes authenticator configuration.
 * 
 * @author P.Piernik
 *
 */
public class AuthenticatorDefinition
{
	public final String id;
	public final String type;
	public final String configuration;
	public final String localCredentialName;

	public AuthenticatorDefinition(String id, String type, String configuration, String localCredentialName)
	{
		this.id = id;
		this.type = type;
		this.configuration = configuration;
		this.localCredentialName = localCredentialName;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, localCredentialName, type, configuration);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuthenticatorDefinition other = (AuthenticatorDefinition) obj;
		return Objects.equals(id, other.id) && Objects.equals(localCredentialName, other.localCredentialName)
				&& Objects.equals(type, other.type)
				&& Objects.equals(configuration, other.configuration);
	}
}
