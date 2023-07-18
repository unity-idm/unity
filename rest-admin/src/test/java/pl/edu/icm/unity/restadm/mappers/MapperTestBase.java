/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public abstract class MapperTestBase<S, T>
{
	protected abstract S getFullAPIObject();

	protected abstract T getFullRestObject();
	
	protected abstract Pair<Function<S, T>, Function<T, S>> getMapper();

	@Test
	public void shouldMapToFullRestObject()
	{
		T mapped = getMapper().getLeft()
				.apply(getFullAPIObject());		
		T fullRestObject = getFullRestObject();
		assertThat(mapped).isEqualTo(fullRestObject);
	}

	@Test
	public void shouldMapFromFullRestObject()
	{
		S mapped = getMapper().getRight()
				.apply(getFullRestObject());
		S apiObject = getFullAPIObject();
		assertThat(mapped).isEqualTo(apiObject);
	}
}
