/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentitiesTreeGridTest
{
	@Test
	void shouldRenderEntityContentOutsideTreeToggle()
	{
		assertThat(IdentitiesTreeGrid.ENTITY_HIERARCHY_TEMPLATE)
				.contains("></vaadin-grid-tree-toggle><span>${item.entityName}</span>")
				.doesNotContain("<span>${item.entityName}</span></vaadin-grid-tree-toggle>");
	}
}
