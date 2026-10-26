/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;


import com.vaadin.flow.function.SerializablePredicate;

interface EntityFilter extends SerializablePredicate<IdentityEntry>
{
	/**
	 * Invoked whenever the grid contents are (re)loaded, possibly for another group. Filters depending on
	 * data computed upfront (e.g. a DB search) shall recompute it here.
	 */
	default void refresh(String groupPath)
	{
	}
}
