/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.user_updates;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import io.imunity.vaadin.elements.MultiSelectGrid;
import pl.edu.icm.unity.base.message.MessageSource;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

class UpdateRequestGrid extends MultiSelectGrid<UpdateRequestModel>
{
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	public UpdateRequestGrid(MessageSource msg, Function<UpdateRequestModel, Component> menuGetter, HtmlContainer container)
	{
		addColumn(model -> model.operation)
				.setHeader(msg.getMessage("UpdateRequest.operation"))
				.setSortable(true)
				.setResizable(true);
		addColumn(model -> model.name)
				.setHeader(msg.getMessage("UpdateRequest.name"))
				.setAutoWidth(true)
				.setSortable(true)
				.setResizable(true);
		addComponentColumn(model -> model.email.generateAsComponent(msg))
				.setHeader(msg.getMessage("UpdateRequest.email"))
				.setAutoWidth(true)
				.setSortable(true)
				.setResizable(true);
		addComponentColumn(this::createGroupsLabel)
				.setHeader(msg.getMessage("UpdateRequest.groups"))
				.setAutoWidth(true)
				.setSortable(true)
				.setResizable(true);
		addColumn(model -> formatter.format(model.requestedTime))
				.setHeader(msg.getMessage("UpdateRequest.requested"))
				.setAutoWidth(true)
				.setSortable(true)
				.setResizable(true);
		addComponentColumn(menuGetter::apply)
				.setHeader(msg.getMessage("UpdateRequest.action"))
				.setTextAlign(ColumnTextAlign.END)
				.setResizable(true);
	}

	private Span createGroupsLabel(UpdateRequestModel model)
	{
		String groups = String.join(", ", model.groupsDisplayedNames);
		Span label = new Span();
		Tooltip.forComponent(label).withText(groups);
		if(groups.length() > 30)
			groups = groups.substring(0, 30) + "...";
		label.setText(groups);
		return label;
	}
}
