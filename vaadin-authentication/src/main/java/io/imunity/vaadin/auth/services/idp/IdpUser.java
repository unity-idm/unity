/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.idp;

import io.imunity.vaadin.elements.grid.FilterableEntry;
import pl.edu.icm.unity.base.entity.EntityState;

import java.util.function.Function;

public record IdpUser(
		Long entity,
		String name,
		String group,
		EntityState state) implements FilterableEntry
{

	@Override
	public boolean anyFieldContains(String searched, Function<String, String> msg)
	{
		String textLower = searched.toLowerCase();
		if (name != null && name.toLowerCase().contains(textLower))
			return true;
		if (state != null && state.toString().toLowerCase().contains(textLower))
			return true;
		return false;
	}
}
