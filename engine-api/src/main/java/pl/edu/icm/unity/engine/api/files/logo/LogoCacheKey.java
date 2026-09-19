/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.files.logo;

/**
 * Identifies a single logo within a cache group/namespace (e.g. a trusted SAML IdP or a federation
 * OAuth OP) for {@link RemoteLogoCacheDownloader} and {@link CachedLogoFileLoader}.
 */
public interface LogoCacheKey
{
	/**
	 * @return a stable, filesystem-safe identifier of the owner of the logo, unique within its
	 * cache group and namespace.
	 */
	String asCacheBasename();
}
