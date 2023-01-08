/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.oidc.metadata;

import java.util.Objects;

class MetaCacheKey
{
	final String url;
	final String validator;
	final String hostnameChecking;

	MetaCacheKey(String url, String validator, String hostnameChecking)
	{
		this.url = url;
		this.validator = validator;
		this.hostnameChecking = hostnameChecking;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(hostnameChecking, url, validator);
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
		MetaCacheKey other = (MetaCacheKey) obj;
		return Objects.equals(hostnameChecking, other.hostnameChecking) && Objects.equals(url, other.url)
				&& Objects.equals(validator, other.validator);
	}
}