/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.List;
import java.util.Map;

import com.vaadin.ui.renderers.HtmlRenderer;

import io.imunity.upman.common.UpManGrid;
import io.imunity.upman.utils.UpManGridHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

/**
 * Displays a grid with group members
 * 
 * @author P.Piernik
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

	GroupMemebersGrid(UnityMessageSource msg, List<SingleActionHandler<GroupMemberEntry>> rowActionHandlers,
			Map<String, String> additionalAttributesName, ConfirmationInfoFormatter formatter)
	{
		super(msg, (GroupMemberEntry e) -> String.valueOf(e.getEntityId()));
		createColumns(rowActionHandlers, additionalAttributesName, formatter);
	}

	public long getManagersCount()
	{
		return getItems().stream().filter(m -> m.getRole().equals(GroupAuthorizationRole.manager)).count();
	}

	private String getRoleLabel(String caption, Images icon)
	{
		return icon.getHtml() + " " + caption;
	}

	private String getRoleLabel(GroupAuthorizationRole role)
	{
		return role != null
				? getRoleLabel(msg.getMessage("Role." + role.toString().toLowerCase()),
						role.equals(GroupAuthorizationRole.manager) ? Images.star : Images.user)
				: "";
	}

	private void createBaseColumns(ConfirmationInfoFormatter formatter)
	{
		addColumn(ie -> getRoleLabel(ie.getRole()), new HtmlRenderer())
				.setCaption(msg.getMessage(BaseColumn.role.captionKey)).setExpandRatio(1);

		addColumn(ie -> ie.getName()).setCaption(msg.getMessage(BaseColumn.name.captionKey)).setExpandRatio(3);
		UpManGridHelper.createEmailColumn(this, (GroupMemberEntry e) -> e.getEmail(),
				msg.getMessage(BaseColumn.email.captionKey), formatter);
	}

	private void createColumns(List<SingleActionHandler<GroupMemberEntry>> rowActionHandlers,
			Map<String, String> additionalAttributesName, ConfirmationInfoFormatter formatter)
	{
		createBaseColumns(formatter);
		UpManGridHelper.createAttrsColumns(this, (GroupMemberEntry e) -> e.getAttributes(),
				additionalAttributesName);
		UpManGridHelper.createActionColumn(this, rowActionHandlers,
				msg.getMessage(BaseColumn.action.captionKey));
	}
}
