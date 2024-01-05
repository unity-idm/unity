/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;

@org.springframework.stereotype.Component
class MockHtmlTooltipFactory implements HtmlTooltipFactory
{
	@Override
	public Component get(String tooltipText)
	{
		return new Div();
	}
}
