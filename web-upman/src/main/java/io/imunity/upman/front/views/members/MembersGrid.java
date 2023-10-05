/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.members;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.vaadin.elements.MultiSelectGrid;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

class MembersGrid extends MultiSelectGrid<MemberModel>
{
	private final MessageSource msg;
	private final Column<MemberModel> roleColumn;

	MembersGrid(Map<String, String> attributes, MessageSource msg, Function<MemberModel, Component> menuGetter, HtmlContainer container)
	{
		this.msg = msg;
		roleColumn = addComponentColumn(model ->
		{
			Div div = new Div();
			Optional.ofNullable(model.role.getValue()).map(VaadinIcon::create).ifPresent(div::add);
			div.add(new Span(" " + model.role.getKey()));
			return div;
		})
				.setHeader(msg.getMessage("GroupMember.projectrole"))
				.setFlexGrow(2)
				.setSortable(true)
				.setResizable(true);
		addColumn(model -> model.name)
				.setHeader(msg.getMessage("GroupMember.name"))
				.setSortable(true)
				.setResizable(true);
		addComponentColumn(model -> model.email.generateAsComponent(msg))
				.setHeader(msg.getMessage("GroupMember.email"))
				.setAutoWidth(true)
				.setSortable(true)
				.setResizable(true);

		attributes.forEach((key, translatedValue) ->
				addColumn(model -> model.attributes.get(key))
						.setHeader(translatedValue)
						.setSortable(true)
						.setResizable(true)
		);

		addComponentColumn(menuGetter::apply)
				.setHeader(msg.getMessage("GroupMember.action"))
				.setTextAlign(ColumnTextAlign.END)
				.setResizable(true);
	}

	void switchToSubprojectMode()
	{
		roleColumn.setHeader(msg.getMessage("GroupMember.subprojectrole"));
		roleColumn.setVisible(true);
	}

	void switchToProjectMode()
	{
		roleColumn.setHeader(msg.getMessage("GroupMember.projectrole"));
		roleColumn.setVisible(true);
	}

	void switchVToRegularSubgroupMode()
	{
		roleColumn.setHeader("");
		roleColumn.setVisible(false);
	}
}
