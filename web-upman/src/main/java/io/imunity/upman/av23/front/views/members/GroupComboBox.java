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
import io.imunity.upman.av23.front.model.GroupTreeNode;

class GroupComboBox extends ComboBox<GroupTreeNode>
{
	GroupComboBox()
	{
		setRenderer(new ComponentRenderer<>(this::renderGroupWithIndent));
		addValueChangeListener(this::blockNullValue);
		setItemLabelGenerator(GroupTreeNode::getDisplayedName);
	}

	private void blockNullValue(AbstractField.ComponentValueChangeEvent<ComboBox<GroupTreeNode>, GroupTreeNode> event)
	{
		if(event.getValue() == null && event.isFromClient())
			setValue(event.getOldValue());
	}

	private Div renderGroupWithIndent(GroupTreeNode group)
	{
		Div div = new Div(new Text(group.getDisplayedName()));
		div.getStyle().set("text-indent", group.getLevel() + "em");
		return div;
	}
}
