/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Span;

public class FlagIcon extends Span
{
	public FlagIcon(String langCode)
	{
		langCode = translateLangCodeToISOAlpha2Code(langCode);
		addClassNames("fi", "fi-" + langCode);
	}

	@Override
	protected void onAttach(AttachEvent event)
	{
		super.onAttach(event);
		// Vaadin resolves @StyleSheet URLs inside the current endpoint's servlet context.
		event.getUI().getPage().addStyleSheet("/unitygw/flag-icons/css/flag-icons.min.css");
	}

	private String translateLangCodeToISOAlpha2Code(String langCode)
	{
		if(langCode.equals("en"))
			langCode = "gb";
		if(langCode.equals("nb") || langCode.equals("nn"))
			langCode = "no";
		return langCode;
	}
}
