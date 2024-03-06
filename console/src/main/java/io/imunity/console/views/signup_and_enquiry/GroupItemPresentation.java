/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.html.Div;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

import static io.imunity.vaadin.elements.CssClassNames.BOLD;

class GroupItemPresentation extends Div
{
	GroupItemPresentation(Group group, MessageSource msg)
	{
		Div name = new Div();
		name.setText(group.getDisplayedName().getValue(msg));
		name.addClassName(BOLD.getName());
		Div path = new Div();
		path.setText(group.getPathEncoded());
		name.add(path);
		add(name, path);
	}
}
