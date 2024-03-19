/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthnContext
{
	public static final String UNDEFINED_IDP = "undefined";
	
	public enum Protocol
	{
		SAML, OIDC, OTHER
	};

	public final Protocol protocol;
	public final String remoteIdPId;
	public final List<String> classReferences;

	@JsonCreator
	public AuthnContext(@JsonProperty("protocol") Protocol protocol, @JsonProperty("remoteIdPId") String remoteIdPId,
			@JsonProperty("classReferences") List<String> classReferences)
	{
		this.protocol = protocol;
		this.remoteIdPId = remoteIdPId;
		this.classReferences = classReferences;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(classReferences, protocol, remoteIdPId);
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
		AuthnContext other = (AuthnContext) obj;
		return Objects.equals(classReferences, other.classReferences) && protocol == other.protocol
				&& Objects.equals(remoteIdPId, other.remoteIdPId);
	}

}
