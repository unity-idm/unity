/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.html.Div;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

import static io.imunity.vaadin.elements.CssClassNames.BOLD;

class GroupItemLabel extends Div
{
	GroupItemLabel(Group group, MessageSource msg)
	{
		this(group.getDisplayedName().getValue(msg), group.getPathEncoded());
	}

	GroupItemLabel(String groupName, String groupPath)
	{
		Div name = new Div();
		name.setText(groupName);
		name.addClassName(BOLD.getName());
		Div path = new Div();
		path.setText(groupPath);
		name.add(path);
		add(name, path);
	}
}
