/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.elements;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Span;

@NpmPackage(value = "flag-icons", version = "6.6.3")
@CssImport("flag-icons/css/flag-icons.css")
@CssImport("flag-icons/css/flag-icons.min.css")
public class FlagIcon extends Span
{
	public FlagIcon(String langCode)
	{
		langCode = translateLangCodeToISOAlpha2Code(langCode);
		addClassNames("fi", "fi-" + langCode);
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
