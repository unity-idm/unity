/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities.credentials;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.formlayout.FormLayout;

class SingleCredentialPanelTest
{
	@Test
	void shouldAllowCredentialDescriptionToShrinkAndWrap()
	{
		// when
		Html description = SingleCredentialPanel.createCredentialName();
		description.setHtmlContent("<div>A credential description that is too long for the dialog</div>");
		FormLayout form = SingleCredentialPanel.createCredentialFormLayout();

		// then
		assertThat(description.getStyle().get("width")).isEqualTo("100%");
		assertThat(description.getStyle().get("min-width")).isEqualTo("0");
		assertThat(description.getStyle().get("overflow-wrap")).isEqualTo("anywhere");
		assertThat(description.getStyle().get("white-space")).isEqualTo("normal");
		assertThat(form.getWidth()).isEqualTo("100%");
		assertThat(form.getStyle().get("min-width")).isEqualTo("0");
	}
}
