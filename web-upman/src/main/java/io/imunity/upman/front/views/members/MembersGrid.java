/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.members;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.vaadin23.elements.TooltipAttacher;
import pl.edu.icm.unity.MessageSource;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

class MembersGrid extends Grid<MemberModel>
{

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());
	private final MessageSource msg;
	private final Column<MemberModel> roleColumn;

	MembersGrid(Map<String, String> attributes, MessageSource msg, Function<MemberModel, Component> menuGetter, HtmlContainer container)
	{
		this.msg = msg;
		roleColumn = addComponentColumn(model ->
		{
			Div div = new Div();
			Optional.ofNullable(model.role.getValue()).map(VaadinIcon::create).ifPresent(div::add);
			div.add(new Label(" " + model.role.getKey()));
			return div;
		})
				.setHeader(msg.getMessage("GroupMember.projectrole"))
				.setFlexGrow(2)
				.setSortable(true);
		addColumn(model -> model.name)
				.setHeader(msg.getMessage("GroupMember.name"))
				.setSortable(true);
		addComponentColumn(model ->
		{
			Icon icon = model.email.icon.create();
			model.email.zonedDateTime.ifPresentOrElse(
					time -> TooltipAttacher.attachTooltip(msg.getMessage("SimpleConfirmationInfo.confirmed", formatter.format(time)), icon, container),
					() -> TooltipAttacher.attachTooltip(msg.getMessage("SimpleConfirmationInfo.unconfirmed"), icon, container)
			);
			return new Div(icon, new Label(" " + model.email.value));
		})
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
