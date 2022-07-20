/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.idp;

import java.util.Objects;

public class ApplicationId
{
	public final String id;

	public ApplicationId(String id)
	{
		this.id = id;
	}

	public static ApplicationId empty()
	{
		return new ApplicationId(null);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ApplicationId that = (ApplicationId) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id);
	}

	@Override
	public String toString()
	{
		return "ApplicationId{" + "id='" + id + '\'' + '}';
	}
}
