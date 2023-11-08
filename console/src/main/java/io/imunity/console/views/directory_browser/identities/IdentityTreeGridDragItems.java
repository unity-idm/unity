/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_browser.identities;

import io.imunity.console.views.directory_browser.EntityWithLabel;

import java.util.Set;

public class IdentityTreeGridDragItems
{
	public final Set<EntityWithLabel> entityWithLabels;

	IdentityTreeGridDragItems(Set<EntityWithLabel> entityWithLabels)
	{
		this.entityWithLabels = entityWithLabels;
	}
}
