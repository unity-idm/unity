/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.members;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.imunity.upman.front.model.GroupTreeNode;
import pl.edu.icm.unity.MessageSource;

class GroupComboBox extends ComboBox<GroupTreeNode>
{
	GroupComboBox(MessageSource msg)
	{
		setRenderer(new ComponentRenderer<>(group -> renderGroupWithIndent(group, msg)));
		addValueChangeListener(this::blockNullValue);
		setItemLabelGenerator(groupTreeNode -> groupTreeNode.getDisplayedNameWithDescription(msg));
	}

	private void blockNullValue(AbstractField.ComponentValueChangeEvent<ComboBox<GroupTreeNode>, GroupTreeNode> event)
	{
		if(event.getValue() == null && event.isFromClient())
			setValue(event.getOldValue());
	}

	private Div renderGroupWithIndent(GroupTreeNode group, MessageSource msg)
	{
		Div div = new Div(new Text(group.getDisplayedNameWithDescription(msg)));
		div.getStyle().set("text-indent", group.getLevel() + "em");
		return div;
	}
}
