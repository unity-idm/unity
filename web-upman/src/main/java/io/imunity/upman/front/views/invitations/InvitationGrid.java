/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.invitations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import io.imunity.vaadin.elements.BlankPageAnchor;
import io.imunity.vaadin.elements.MultiSelectGrid;
import pl.edu.icm.unity.base.message.MessageSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;

class InvitationGrid extends MultiSelectGrid<InvitationModel>
{
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	public InvitationGrid(MessageSource msg, Function<InvitationModel, Component> actionMenuGetter)
	{
		addColumn(model -> model.email)
				.setHeader(msg.getMessage("Invitation.email"))
				.setAutoWidth(true)
				.setSortable(true)
				.setResizable(true);
		addComponentColumn(this::createGroupsLabel)
				.setHeader(msg.getMessage("Invitation.groups"))
				.setFlexGrow(5)
				.setSortable(true)
				.setResizable(true);
		addColumn(model -> Optional.ofNullable(model.requestedTime)
				.map(formatter::format)
				.orElse(null))
						.setHeader(msg.getMessage("Invitation.lastSent"))
				.setAutoWidth(true)
				.setSortable(true)
				.setResizable(true);
		addColumn(model -> format(model.expirationTime))
				.setHeader(msg.getMessage("Invitation.expiration"))
				.setAutoWidth(true)
				.setSortable(true)
				.setResizable(true);
		addComponentColumn(model -> new BlankPageAnchor(model.link, VaadinIcon.EXTERNAL_LINK.create()))
				.setAutoWidth(true)
				.setTextAlign(ColumnTextAlign.END)
				.setHeader(msg.getMessage("Invitation.link"))
				.setResizable(true);
		addComponentColumn(actionMenuGetter::apply)
				.setHeader(msg.getMessage("Invitation.action"))
				.setAutoWidth(true)
				.setTextAlign(ColumnTextAlign.END)
				.setResizable(true);

		setClassNameGenerator(model -> model.expirationTime.isBefore(Instant.now()) ? "light-red-row" : "usual-row");
	}

	private static String format(Instant instant)
	{
		if(instant == null)
			return "";
		return formatter.format(instant);
	}

	private Span createGroupsLabel(InvitationModel model)
	{
		String groups = String.join(", ", model.groupsDisplayedNames);
		Span label = new Span(groups);
		Tooltip.forComponent(label)
				.withText(groups);
		return label;
	}
}
