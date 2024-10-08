/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;

import org.junit.jupiter.api.Test;


public abstract class MapperTestBase<S, T>
{
	protected abstract S getFullAPIObject();

	protected abstract T getFullDBObject();
	
	protected abstract Pair<Function<S, T>, Function<T, S>> getMapper();

	public void shouldMapToFullRestObject()
	{
		T mapped = getMapper().left
				.apply(getFullAPIObject());		
		T fullRestObject = getFullDBObject();
		assertThat(mapped).isEqualTo(fullRestObject);
	}

	@Test
	public void shouldMapFromFullRestObject()
	{
		S mapped = getMapper().right
				.apply(getFullDBObject());
		S apiObject = getFullAPIObject();
		assertThat(mapped).isEqualTo(apiObject);
	}
}
