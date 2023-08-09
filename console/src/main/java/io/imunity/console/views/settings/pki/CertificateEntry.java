/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.pki;

import pl.edu.icm.unity.engine.api.pki.NamedCertificate;

import java.security.cert.X509Certificate;
import java.util.Objects;

class CertificateEntry
{
	private String name;
	private X509Certificate value;

	CertificateEntry()
	{
	}

	CertificateEntry(NamedCertificate certificate)
	{
		this.name = certificate.name;
		this.value = certificate.value;
	}

	String getName()
	{
		return name;
	}

	void setName(String name)
	{
		this.name = name;
	}

	X509Certificate getValue()
	{
		return value;
	}

	void setValue(X509Certificate value)
	{
		this.value = value;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CertificateEntry that = (CertificateEntry) o;
		return Objects.equals(name, that.name) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, value);
	}

	@Override
	public String toString()
	{
		return "CertificateEntry{" +
				"name='" + name + '\'' +
				", value=" + value +
				'}';
	}
}
