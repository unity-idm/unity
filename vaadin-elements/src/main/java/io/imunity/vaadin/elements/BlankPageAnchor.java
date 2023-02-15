/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.server.AbstractStreamResource;

public class BlankPageAnchor extends Anchor
{
	public BlankPageAnchor(String href, String text)
	{
		super(href, text);
		setTarget( "_blank" );
	}

	public BlankPageAnchor(String href, String text, AnchorTarget target)
	{
		super(href, text, target);
		setTarget( "_blank" );
	}

	public BlankPageAnchor(AbstractStreamResource href, String text)
	{
		super(href, text);
		setTarget( "_blank" );
	}

	public BlankPageAnchor(String href, Component... components)
	{
		super(href, components);
		setTarget( "_blank" );
	}
}
