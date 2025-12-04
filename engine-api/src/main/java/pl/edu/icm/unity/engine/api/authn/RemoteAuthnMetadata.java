/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import java.util.List;

public record RemoteAuthnMetadata(Protocol protocol, String remoteIdPId, List<String> classReferences)
{
	public static final String UNDEFINED_IDP = "undefined";
	
	public enum Protocol
	{
		SAML, OIDC, OTHER
	};
	
	@Override
	public String toString()
	{
		return "[protocol=" + protocol + ", remoteIdPId=" + remoteIdPId
				+ ", classReferences=" + classReferences + "]";
	}
}
