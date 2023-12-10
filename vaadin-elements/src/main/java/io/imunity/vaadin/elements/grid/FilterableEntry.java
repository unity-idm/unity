/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.elements.grid;

import java.util.function.Function;

/**
 * Grid entry which can be filtered
 * @author P.Piernik
 *
 */
public interface FilterableEntry
{
	 boolean anyFieldContains(String searched, Function<String, String>  msg);
}
