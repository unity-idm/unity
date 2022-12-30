/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public abstract class MapperTestBase<S, T>
{
	protected abstract S getAPIObject();

	protected abstract T getRestObject();

	protected abstract Pair<Function<S, T>, Function<T, S>> getMapper();

	@Test
	public void shouldMapToRestObject()
	{
		T mapped = getMapper().getLeft()
				.apply(getAPIObject());
		assertThat(mapped).isEqualTo(getRestObject());
	}

	@Test
	public void shouldMapFromRestObject()
	{
		S mapped = getMapper().getRight()
				.apply(getRestObject());
		S apiObject = getAPIObject();
		assertThat(mapped).isEqualTo(apiObject);
	}
}
