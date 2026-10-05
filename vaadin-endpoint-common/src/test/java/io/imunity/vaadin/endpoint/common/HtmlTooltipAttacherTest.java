/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.shared.Tooltip.TooltipPosition;

class HtmlTooltipAttacherTest
{
	@Test
	void shouldAttachMarkdownTooltip()
	{
		// given
		Button target = new Button();

		// when
		Tooltip tooltip = HtmlTooltipAttacher.to(target, "Help <b>text</b>");

		// then
		assertThat(tooltip.getText()).isEqualTo("Help <b>text</b>");
		assertThat(tooltip.getPosition()).isEqualTo(TooltipPosition.BOTTOM);
		assertThat(Tooltip.forComponent(target)).isSameAs(tooltip);
	}
}
