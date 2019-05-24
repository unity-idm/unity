/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common.grid;

import com.vaadin.server.SerializablePredicate;

/**
 * 
 * @author P.Piernik
 *
 */
public interface FilterableGrid<T>
{
	void clearFilters();
	void addFilter(SerializablePredicate<T> filter);
}
