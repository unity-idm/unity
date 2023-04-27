/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CustomResourceProviderTest
{

	@Test
	void shouldReturnMinus1WhenCompareResources()
	{
		int resourcesComparator = CustomResourceProvider.compareResources("arg1", "arg2", "arg1");
		Assertions.assertEquals(-1, resourcesComparator);
	}

	@Test
	void shouldReturn1WhenCompareResources()
	{
		int resourcesComparator = CustomResourceProvider.compareResources("arg2", "arg1", "arg1");
		Assertions.assertEquals(1, resourcesComparator);
	}

	@Test
	void shouldReturn0WhenCompareResources()
	{
		int resourcesComparator = CustomResourceProvider.compareResources("arg2", "arg1", "arg3");
		Assertions.assertEquals(0, resourcesComparator);
	}
}