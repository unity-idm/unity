/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store;

import org.junit.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class OneWayMapperTestBase<S, T>
{
	protected abstract S getAPIObject();

	protected abstract T getRestObject();

	protected abstract Function<S, T> getMapper();

	@Test
	public void shouldMap()
	{
		T mapped = getMapper().apply(getAPIObject());
		assertThat(mapped).isEqualTo(getRestObject());
	}
}
