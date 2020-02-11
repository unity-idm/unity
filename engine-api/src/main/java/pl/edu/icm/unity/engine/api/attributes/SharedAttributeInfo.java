/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.Objects;

import com.google.common.base.MoreObjects;

public class SharedAttributeInfo
{
	public final String externalId;

	public SharedAttributeInfo(String externalId)
	{
		this.externalId = externalId;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(externalId);
	}

	@Override
	public boolean equals(Object object)
	{
		if (object instanceof SharedAttributeInfo)
		{
			SharedAttributeInfo that = (SharedAttributeInfo) object;
			return Objects.equals(this.externalId, that.externalId);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this).add("externalId", externalId).toString();
	}
}
