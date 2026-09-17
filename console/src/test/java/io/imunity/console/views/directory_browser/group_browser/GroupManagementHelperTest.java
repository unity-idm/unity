/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_browser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

class GroupManagementHelperTest
{
	@Test
	void shouldRenderConfirmationMessageAsWrappingText()
	{
		// given
		String message = "Add an entity with a long identifier to /a/long/group/path?";

		// when
		ConfirmDialog dialog = GroupManagementHelper.createConfirmationDialog(message);

		// then
		assertThat(dialog.getElement().getProperty("message")).isEqualTo(message);
	}
}
