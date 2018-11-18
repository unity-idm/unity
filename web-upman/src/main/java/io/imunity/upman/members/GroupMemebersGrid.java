/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;

import io.imunity.upman.members.GroupMemberEntry.Role;
import io.imunity.webelements.common.SidebarStyles;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Displays a grid with group members
 * @author P.Piernik
 *
 */

public class GroupMemebersGrid extends Grid<GroupMemberEntry>
{
	public static final String ATTR_COL_PREFIX = "a::";

	enum BaseColumn
	{
		role("GroupMember.role"), 
		name("GroupMember.name"),
		email("GroupMember.email"),
		action("GroupMember.action");
		private String captionKey;

		BaseColumn(String captionKey)
		{
			this.captionKey = captionKey;

		}
	};

	private final UnityMessageSource msg;
	private List<GroupMemberEntry> groupMemberEntries;
	private ListDataProvider<GroupMemberEntry> dataProvider;
	private List<SingleActionHandler<GroupMemberEntry>> rowActionHandlers;

	public GroupMemebersGrid(UnityMessageSource msg,
			List<SingleActionHandler<GroupMemberEntry>> rowActionHandlers,
			Map<String, String>  additionalAttributesName)
	{
		this.msg = msg;
		this.rowActionHandlers = rowActionHandlers;

		groupMemberEntries = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(groupMemberEntries);
		setDataProvider(dataProvider);

		createBaseColumns();
		createAttrsColumns(additionalAttributesName);
		createActionColumn();

		setSelectionMode(SelectionMode.MULTI);
		GridSelectionSupport.installClickListener(this);
		setSizeFull();
	}

	public void setValue(Collection<GroupMemberEntry> items)
	{
		for (GroupMemberEntry entry : items)
		{
			groupMemberEntries.add(entry);
		}
		dataProvider.refreshAll();
	}

	public void clear()
	{
		groupMemberEntries.clear();
		dataProvider.refreshAll();
	}

	private Label getRoleLabel(String caption, Images icon)
	{
		Label l = new Label(icon.getHtml() + " " + caption);
		l.setContentMode(ContentMode.HTML);
		return l;
	}

	private void createBaseColumns()
	{

		addComponentColumn(ie -> {
			if (ie.getRole().equals(Role.admin))
			{
				return getRoleLabel(msg.getMessage("Role.admin"), Images.star);
			} else
			{
				return getRoleLabel(msg.getMessage("Role.regularUser"),
						Images.user);
			}

		}).setCaption(msg.getMessage(BaseColumn.role.captionKey)).setExpandRatio(2)
				.setResizable(false);

		addColumn(ie -> ie.getBaseValue(BaseColumn.name))
				.setCaption(msg.getMessage(BaseColumn.name.captionKey))
				.setExpandRatio(3);
		addColumn(ie -> ie.getBaseValue(BaseColumn.email))
				.setCaption(msg.getMessage(BaseColumn.email.captionKey))
				.setExpandRatio(3);
	}
	
	private void createAttrsColumns(Map<String, String> additionalAttributes)
	{

		for (Map.Entry<String, String> attr : additionalAttributes.entrySet())
		{
			addColumn(ie -> ie.getAttribute(attr.getKey())).setCaption(attr.getValue())
				.setExpandRatio(3).setId(ATTR_COL_PREFIX + attr.getKey());
		}

	}
	
	private void createActionColumn()
	{
		addComponentColumn(ie -> {
			HamburgerMenu<GroupMemberEntry> menu = new HamburgerMenu<GroupMemberEntry>();
			menu.setTarget(Sets.newHashSet(ie));
			menu.addActionHandlers(rowActionHandlers);
			menu.addStyleName(SidebarStyles.sidebar.toString());
			return menu;

		}).setCaption(msg.getMessage(BaseColumn.action.captionKey)).setWidth(80)
				.setResizable(false);

	}
}
