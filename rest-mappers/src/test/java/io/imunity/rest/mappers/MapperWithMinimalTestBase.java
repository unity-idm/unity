/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public abstract class MapperWithMinimalTestBase<S, T> extends MapperTestBase<S, T>
{
	protected abstract S getMinAPIObject();

	protected abstract T getMinRestObject();

	@Test
	public void shouldMapFromMinRestObject()
	{
		S mapped = getMapper().right
				.apply(getMinRestObject());
		S apiObject = getMinAPIObject();
		assertThat(mapped).isEqualTo(apiObject);
	}
}
