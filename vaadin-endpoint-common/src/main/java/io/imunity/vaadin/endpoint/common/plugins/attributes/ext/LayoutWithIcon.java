/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

class LayoutWithIcon extends HorizontalLayout
{
	LayoutWithIcon(Component representation, Icon icon)
	{
		setWidthFull();
		add(representation, icon);
		setAlignSelf(Alignment.CENTER, icon);		
	}
}
