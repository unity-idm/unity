/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.invitations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.vaadin23.elements.BlankPageAnchor;
import io.imunity.vaadin23.elements.MultiSelectGrid;
import io.imunity.vaadin23.elements.TooltipAttacher;
import pl.edu.icm.unity.MessageSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

@CssImport(value = "./styles/components/invitations-grid.css", themeFor = "vaadin-grid")
class InvitationGrid extends MultiSelectGrid<InvitationModel>
{
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	public InvitationGrid(MessageSource msg, Function<InvitationModel, Component> actionMenuGetter, HtmlContainer container)
	{
		addColumn(model -> model.email)
				.setHeader(msg.getMessage("Invitation.email"))
				.setAutoWidth(true)
				.setSortable(true)
				.setResizable(true);
		addComponentColumn(model -> createGroupsLabel(model, container))
				.setHeader(msg.getMessage("Invitation.groups"))
				.setFlexGrow(5)
				.setSortable(true)
				.setResizable(true);
		addColumn(model -> formatter.format(model.requestedTime))
				.setHeader(msg.getMessage("Invitation.lastSent"))
				.setAutoWidth(true)
				.setSortable(true)
				.setResizable(true);
		addColumn(model -> formatter.format(model.expirationTime))
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

	private Label createGroupsLabel(InvitationModel model, HtmlContainer container)
	{
		String groups = String.join(", ", model.groupsDisplayedNames);
		Label label = new Label(groups);
		TooltipAttacher.attachTooltip(groups, label, container);
		return label;
	}
}
