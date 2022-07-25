/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.members;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import pl.edu.icm.unity.MessageSource;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

class MembersGrid extends Grid<MemberModel>
{
	private final MessageSource msg;
	private final Column<MemberModel> roleColumn;

	MembersGrid(Map<String, String> attributes, MessageSource msg, Function<MemberModel, Component> menuGetter)
	{
		this.msg = msg;
		roleColumn = addComponentColumn(model ->
		{
			Div div = new Div();
			Optional.ofNullable(model.role.getValue()).map(VaadinIcon::create).ifPresent(div::add);
			div.add(new Label(model.role.getKey()));
			return div;
		})
				.setHeader(msg.getMessage("GroupMember.projectrole"))
				.setFlexGrow(2)
				.setSortable(true);
		addColumn(model -> model.name)
				.setHeader(msg.getMessage("GroupMember.name"))
				.setSortable(true);
		addComponentColumn(model -> new Div(model.email.getValue().create(), new Label(model.email.getKey())))
				.setHeader(msg.getMessage("GroupMember.email"))
				.setAutoWidth(true)
				.setSortable(true);

		attributes.forEach((key, translatedValue) ->
				addColumn(model -> model.attributes.get(key))
						.setHeader(translatedValue)
						.setSortable(true)
		);

		addComponentColumn(menuGetter::apply)
				.setHeader(msg.getMessage("GroupMember.action"))
				.setTextAlign(ColumnTextAlign.END);

		setThemeName("no-border");
		setSelectionMode(Grid.SelectionMode.MULTI);
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
