/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.members;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.imunity.upman.av23.front.model.Group;

class GroupComboBox extends ComboBox<Group>
{
	GroupComboBox ()
	{
		setRenderer(new ComponentRenderer<>(this::renderGroupWithIndent));
		addValueChangeListener(this::blockNullValue);
		setItemLabelGenerator(event -> event.displayedName);
	}

	private void blockNullValue(AbstractField.ComponentValueChangeEvent<ComboBox<Group>, Group> event)
	{
		if(event.getValue() == null && event.isFromClient())
			setValue(event.getOldValue());
	}

	private Div renderGroupWithIndent(Group group)
	{
		Div div = new Div(new Text(group.displayedName));
		div.getStyle().set("text-indent", group.level + "em");
		return div;
	}
}
