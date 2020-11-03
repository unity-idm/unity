/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.vaadin.ui.renderers.HtmlRenderer;

import io.imunity.upman.utils.UpManGridHelper;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

/**
 * Displays a grid with group members
 * 
 * @author P.Piernik
 */
class GroupMemebersGrid extends GridWithActionColumn<GroupMemberEntry>
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

	GroupMemebersGrid(MessageSource msg, List<SingleActionHandler<GroupMemberEntry>> rowActionHandlers,
			Map<String, String> additionalAttributesName, ConfirmationInfoFormatter formatter)
	{
		super(msg, Lists.newArrayList(), false, false);
		addHamburgerActions(rowActionHandlers);
		setIdProvider(e -> String.valueOf(e.getEntityId()));
		setMultiSelect(true);
		createColumns(rowActionHandlers, additionalAttributesName, formatter);
	}

	public long getManagersCount()
	{
		return getElements().stream().filter(m -> !m.getRole().equals(GroupAuthorizationRole.regular)).count();
	}

	private String getRoleLabel(String caption, Images icon)
	{
		return icon.getHtml() + " " + caption;
	}

	private String getRoleLabel(GroupAuthorizationRole role)
	{
		return role != null
				? getRoleLabel(msg.getMessage("Role." + role.toString().toLowerCase()),
						role.equals(GroupAuthorizationRole.regular) ? Images.user : Images.star)
				: "";
	}

	private void createBaseColumns(ConfirmationInfoFormatter formatter)
	{
		addColumn(ie -> getRoleLabel(ie.getRole()), msg.getMessage(BaseColumn.role.captionKey), 1)
				.setRenderer(new HtmlRenderer()).setResizable(true);
		addColumn(ie -> ie.getName(), msg.getMessage(BaseColumn.name.captionKey), 3).setResizable(true);
		UpManGridHelper.createEmailColumn(this, (GroupMemberEntry e) -> e.getEmail(),
				msg.getMessage(BaseColumn.email.captionKey), formatter);
	}

	private void createColumns(List<SingleActionHandler<GroupMemberEntry>> rowActionHandlers,
			Map<String, String> additionalAttributesName, ConfirmationInfoFormatter formatter)
	{
		createBaseColumns(formatter);
		UpManGridHelper.createAttrsColumns(this, (GroupMemberEntry e) -> e.getAttributes(),
				additionalAttributesName);
	}
}
