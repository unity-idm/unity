/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.endpoint.common.grid;

import com.vaadin.flow.function.SerializablePredicate;

public interface FilterableGrid<T>
{
	void clearFilters();
	void addFilter(SerializablePredicate<T> filter);
	void removeFilter(SerializablePredicate<T> filter);
}
