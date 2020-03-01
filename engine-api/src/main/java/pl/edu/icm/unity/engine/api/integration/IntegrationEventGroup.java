/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.integration;

import com.google.common.base.Objects;

/**
 * Integration event grouping indicator
 * 
 * @author P.Piernik
 *
 */
public class IntegrationEventGroup
{
	public final String name;

	public IntegrationEventGroup(String name)
	{
		this.name = name;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final IntegrationEventGroup that = (IntegrationEventGroup) o;
		return Objects.equal(this.name, that.name);
	}
}
