/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.List;
import java.util.Map;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

import io.imunity.upman.common.UpManGrid;
import io.imunity.upman.utils.UpManGridHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Displays a grid with group members
 * 
 * @author P.Piernik
 *
 */

class GroupMemebersGrid extends UpManGrid<GroupMemberEntry>
{

	enum BaseColumn
	{
		role("GroupMember.role"), name("GroupMember.name"), email("GroupMember.email"), action(
				"GroupMember.action");
		private String captionKey;

		BaseColumn(String captionKey)
		{
			this.captionKey = captionKey;
		}
	};

	public GroupMemebersGrid(UnityMessageSource msg, List<SingleActionHandler<GroupMemberEntry>> rowActionHandlers,
			Map<String, String> additionalAttributesName)
	{
		super(msg, (GroupMemberEntry e) -> String.valueOf(e.getEntityId()));
		createColumns(rowActionHandlers, additionalAttributesName);

	}

	public long getManagersCount()
	{
		return getItems().stream().filter(m -> m.getRole().equals(GroupAuthorizationRole.manager)).count();
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
			if (ie.getRole() != null)
			{
				return getRoleLabel(msg.getMessage("Role." + ie.getRole().toString().toLowerCase()),
						ie.getRole().equals(GroupAuthorizationRole.manager) ? Images.star
								: Images.user);
			} else
			{
				return null;
			}

		}).setCaption(msg.getMessage(BaseColumn.role.captionKey)).setExpandRatio(2).setResizable(false);

		addColumn(ie -> ie.getName()).setCaption(msg.getMessage(BaseColumn.name.captionKey)).setExpandRatio(3);
		addColumn(ie -> ie.getEmail()).setCaption(msg.getMessage(BaseColumn.email.captionKey))
				.setExpandRatio(3);
	}

	private void createColumns(List<SingleActionHandler<GroupMemberEntry>> rowActionHandlers,
			Map<String, String> additionalAttributesName)
	{
		createBaseColumns();
		UpManGridHelper.createAttrsColumns(this, (GroupMemberEntry e) -> e.getAttributes(),
				additionalAttributesName);
		UpManGridHelper.createActionColumn(this, rowActionHandlers,
				msg.getMessage(BaseColumn.action.captionKey));
	}
}
