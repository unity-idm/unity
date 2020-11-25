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
	private Column<GroupMemberEntry, String> roleColumn;
	
	
	enum BaseColumn
	{
		role("GroupMember.projectrole", "GroupMember.subprojectrole"),
		name("GroupMember.name"),
		email("GroupMember.email"),
		action("GroupMember.action");

		private String projectCaptionKey;
		private String subprojectCaptionKey;
		
		BaseColumn(String projectCaptionKey, String subprojectCaptionKey)
		{
			this.projectCaptionKey = projectCaptionKey;
			this.subprojectCaptionKey = subprojectCaptionKey;
		}
		
		BaseColumn(String projectCaptionKey)
		{
			this.projectCaptionKey = projectCaptionKey;
			this.subprojectCaptionKey = projectCaptionKey;
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
		return (icon != null ? icon.getHtml() : "") + " " + caption;
	}

	private String getRoleLabel(GroupAuthorizationRole role)
	{
		return role != null
				? getRoleLabel(msg.getMessage("Role." + role.toString().toLowerCase()),
						getRoleIcon(role))
				: "";
	}
	
	private Images getRoleIcon(GroupAuthorizationRole role)
	{
		if (role.equals(GroupAuthorizationRole.manager))
		{
			return Images.star_open;
		}else if (role.equals(GroupAuthorizationRole.projectsAdmin))
		{
			return Images.star;
		}
		
		return null;
	
	}
	
	
	void switchToSubprojectMode()
	{
		roleColumn.setCaption(msg.getMessage(BaseColumn.role.subprojectCaptionKey));
		roleColumn.setHidden(false);
	}
	
	void switchToProjectMode()
	{
		roleColumn.setCaption(msg.getMessage(BaseColumn.role.projectCaptionKey));
		roleColumn.setHidden(false);
	}
	
	void switchToRegularSubgroupMode()
	{
		roleColumn.setCaption("");
		roleColumn.setHidden(true);
	}

	private void createBaseColumns(ConfirmationInfoFormatter formatter)
	{
		roleColumn = addColumn(ie -> getRoleLabel(ie.getRole()), msg.getMessage(BaseColumn.role.projectCaptionKey), 1)
				.setRenderer(new HtmlRenderer()).setResizable(true);
		addColumn(ie -> ie.getName(), msg.getMessage(BaseColumn.name.projectCaptionKey), 3).setResizable(true);
		UpManGridHelper.createEmailColumn(this, (GroupMemberEntry e) -> e.getEmail(),
				msg.getMessage(BaseColumn.email.projectCaptionKey), formatter);
	}

	private void createColumns(List<SingleActionHandler<GroupMemberEntry>> rowActionHandlers,
			Map<String, String> additionalAttributesName, ConfirmationInfoFormatter formatter)
	{
		createBaseColumns(formatter);
		UpManGridHelper.createAttrsColumns(this, (GroupMemberEntry e) -> e.getAttributes(),
				additionalAttributesName);
	}
}
