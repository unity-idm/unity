/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip.TooltipPosition;
import com.vaadin.flow.dom.Element;

class MultiTabComponentTest
{
	@Test
	void shouldAttachEndTopTooltipAsDirectChildWhenTextIsHidden()
	{
		// given
		MenuComponent menuComponent = MenuComponent.builder(new MenuComponent[0])
				.tabName("Parent")
				.icon(VaadinIcon.FOLDER)
				.build();
		MultiTabComponent component = new MultiTabComponent(menuComponent);

		// when
		component.hideText();

		// then
		Element tooltipElement = component.getElement().getChildren()
				.filter(element -> "vaadin-tooltip".equals(element.getTag()))
				.findFirst()
				.orElseThrow();
		assertThat(component.hasClassName("u-multi-tab")).isTrue();
		assertThat(tooltipElement.getAttribute("slot")).isEqualTo("tooltip");
		assertThat(component.getTooltip().getPosition()).isEqualTo(TooltipPosition.END_TOP);
	}
}
