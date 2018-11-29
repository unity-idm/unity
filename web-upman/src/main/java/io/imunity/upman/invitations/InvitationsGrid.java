/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Displays a grid with invitations
 * 
 * @author P.Piernik
 *
 */
class InvitationsGrid extends Grid<InvitationEntry>
{
	enum BaseColumn
	{
		email("Invitation.email"), groups("Invitation.groups"), requested("Invitation.requested"), expiration(
				"Invitation.expiration"), link("Invitation.link"), action("Invitation.action");

		private String captionKey;

		BaseColumn(String captionKey)
		{
			this.captionKey = captionKey;
		}
	};

	private final UnityMessageSource msg;
	private List<InvitationEntry> invitaionEntries;
	private ListDataProvider<InvitationEntry> dataProvider;
	private List<SingleActionHandler<InvitationEntry>> rowActionHandlers;

	public InvitationsGrid(UnityMessageSource msg, List<SingleActionHandler<InvitationEntry>> rowActionHandlers)
	{
		this.msg = msg;
		this.rowActionHandlers = rowActionHandlers;

		invitaionEntries = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(invitaionEntries);
		setDataProvider(dataProvider);

		createBaseColumns();

		setSelectionMode(SelectionMode.MULTI);
		GridSelectionSupport.installClickListener(this);
		setSizeFull();
	}

	public void setValue(Collection<InvitationEntry> items)
	{
		Set<InvitationEntry> selectedItems = getSelectedItems();
		deselectAll();
		invitaionEntries.clear();
		invitaionEntries.addAll(items);
		if (invitaionEntries.size() <= 18)
			setHeightByRows(invitaionEntries.isEmpty() ? 1 : invitaionEntries.size());
		else
			setHeight(100, Unit.PERCENTAGE);
		dataProvider.refreshAll();

		for (String selected : selectedItems.stream().map(s -> s.code).collect(Collectors.toList()))
		{
			for (InvitationEntry entry : invitaionEntries)
				if (entry.code == selected)
					select(entry);

		}

	}

	private void createBaseColumns()
	{
		addColumn(ie -> ie.email).setCaption(msg.getMessage(BaseColumn.email.captionKey)).setExpandRatio(3);
		addColumn(ie -> {
			return (ie.groups != null) ? String.join(",", ie.groups) : "";
		}).setCaption(msg.getMessage(BaseColumn.groups.captionKey)).setExpandRatio(3);

		addColumn(ie -> ie.requestedTime != null ? TimeUtil.formatMediumInstant(ie.requestedTime) : "")
				.setCaption(msg.getMessage(BaseColumn.requested.captionKey)).setExpandRatio(3);

		addColumn(ie -> ie.expirationTime != null ? TimeUtil.formatMediumInstant(ie.expirationTime) : "")
				.setCaption(msg.getMessage(BaseColumn.expiration.captionKey)).setExpandRatio(3);

		addComponentColumn(ie -> {
			Button link = new Button(Images.external_link.getResource());
			link.addStyleName(SidebarStyles.sidebar.toString());
			link.addStyleName(Styles.vButtonLink.toString());
			link.addStyleName(Styles.toolbarButton.toString());
			link.addClickListener(e -> Page.getCurrent().open(ie.link, "_blank"));
			return link;

		}).setCaption(msg.getMessage(BaseColumn.link.captionKey)).setWidth(80).setResizable(false);

		addComponentColumn(ie -> {
			HamburgerMenu<InvitationEntry> menu = new HamburgerMenu<InvitationEntry>();
			menu.setTarget(Sets.newHashSet(ie));
			menu.addActionHandlers(rowActionHandlers);
			menu.addStyleName(SidebarStyles.sidebar.toString());
			return menu;

		}).setCaption(msg.getMessage(BaseColumn.action.captionKey)).setWidth(80).setResizable(false);
	}
}
