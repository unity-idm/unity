/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public abstract class MapperWithMinimalTestBase<S, T> extends MapperTestBase<S, T>
{
	protected abstract S getMinAPIObject();

	protected abstract T getMinDBObject();

	@Test
	public void shouldMapFromMinRestObject()
	{
		S mapped = getMapper().right
				.apply(getMinDBObject());
		S apiObject = getMinAPIObject();
		assertThat(mapped).isEqualTo(apiObject);
	}
}
