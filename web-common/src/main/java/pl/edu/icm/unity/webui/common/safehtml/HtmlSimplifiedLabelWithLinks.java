/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.safehtml;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

public class HtmlSimplifiedLabelWithLinks extends Label
{
	private static final PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

	public HtmlSimplifiedLabelWithLinks()
	{
		setContentMode(ContentMode.HTML);
	}

	@Override
	public final void setValue(String value)
	{
		super.setValue(policy.sanitize(value));
	}
}