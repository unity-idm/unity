/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp;

import java.util.Objects;

public class SamlEntityId
{
	public final String id;
	public final String dnSamlId;

	public SamlEntityId(String id, String dnSamlId)
	{
		this.id = id;
		this.dnSamlId = dnSamlId;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SamlEntityId that = (SamlEntityId) o;
		return Objects.equals(id, that.id) && Objects.equals(dnSamlId, that.dnSamlId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, dnSamlId);
	}

	@Override
	public String toString()
	{
		return "SamlEntityId{" +
				"id=" + id +
				", dnSamlId='" + dnSamlId + '\'' +
				'}';
	}

	public static SamlEntityIdBuilder builder()
	{
		return new SamlEntityIdBuilder();
	}

	public static final class SamlEntityIdBuilder
	{
		private String id;
		private String dnSamlId;

		private SamlEntityIdBuilder()
		{
		}

		public SamlEntityIdBuilder withId(String id)
		{
			this.id = id;
			return this;
		}

		public SamlEntityIdBuilder withDnSamlId(String dnSamlId)
		{
			this.dnSamlId = dnSamlId;
			return this;
		}

		public SamlEntityId build()
		{
			return new SamlEntityId(id, dnSamlId);
		}
	}
}
