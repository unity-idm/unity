/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.pki;

import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Represents an certificate with name
 * 
 * @author P.Piernik
 *
 */
public class NamedCertificate 
{
	public final String name;
	public final X509Certificate value;

	public NamedCertificate(String name, X509Certificate value)
	{
		this.name = name;
		this.value = value;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, value);
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
		NamedCertificate other = (NamedCertificate) obj;

		return Objects.equals(name, other.name) && Objects.equals(value, other.value);
	}
}
