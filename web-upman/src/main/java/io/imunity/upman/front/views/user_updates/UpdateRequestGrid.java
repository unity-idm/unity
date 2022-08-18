/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.user_updates;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import io.imunity.vaadin23.elements.TooltipAttacher;
import pl.edu.icm.unity.MessageSource;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

class UpdateRequestGrid extends Grid<UpdateRequestModel>
{
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	public UpdateRequestGrid(MessageSource msg, Function<UpdateRequestModel, Component> menuGetter, HtmlContainer container)
	{
		addColumn(model -> model.operation)
				.setHeader(msg.getMessage("UpdateRequest.operation"))
				.setSortable(true);
		addColumn(model -> model.name)
				.setHeader(msg.getMessage("UpdateRequest.name"))
				.setAutoWidth(true)
				.setSortable(true);
		addComponentColumn(model -> new Div(new Label(model.email.getKey()), model.email.getValue().create()))
				.setHeader(msg.getMessage("UpdateRequest.email"))
				.setAutoWidth(true)
				.setSortable(true);
		addComponentColumn(model -> createGroupsLabel(model, container))
				.setHeader(msg.getMessage("UpdateRequest.groups"))
				.setAutoWidth(true)
				.setSortable(true);
		addColumn(model -> formatter.format(model.requestedTime))
				.setHeader(msg.getMessage("UpdateRequest.requested"))
				.setAutoWidth(true)
				.setSortable(true);
		addComponentColumn(menuGetter::apply)
				.setHeader(msg.getMessage("UpdateRequest.action"))
				.setTextAlign(ColumnTextAlign.END);

		setThemeName("no-border");
		setSelectionMode(Grid.SelectionMode.MULTI);
	}

	private Label createGroupsLabel(UpdateRequestModel model, HtmlContainer container)
	{
		String groups = String.join(", ", model.groupsDisplayedNames);
		Label label = new Label(groups);
		TooltipAttacher.attachTooltip(groups, label, container);
		return label;
	}
}
