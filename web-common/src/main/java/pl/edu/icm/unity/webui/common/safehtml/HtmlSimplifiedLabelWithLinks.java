/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.safehtml;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

public class HtmlSimplifiedLabelWithLinks extends Label
{
	private static final PolicyFactory policy = Sanitizers.FORMATTING
			.and(new HtmlPolicyBuilder().allowStandardUrlProtocols().allowElements("a")
					.allowAttributes("href", "target").onElements("a").requireRelNofollowOnLinks().toFactory());

	public HtmlSimplifiedLabelWithLinks()
	{
		setContentMode(ContentMode.HTML);
	}

	@Override
	public final void setValue(String value)
	{
		super.setValue(policy.sanitize(value));
	}

	@Override
	public void setDescription(String description)
	{
		super.setDescription(policy.sanitize(description), ContentMode.HTML);

	}

	@Override
	public void setDescription(String description, ContentMode mode)
	{
		super.setDescription(mode.equals(ContentMode.HTML) ? policy.sanitize(description) : description, mode);

	}
}