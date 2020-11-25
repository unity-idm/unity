/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.time.Instant;
import java.util.List;

import com.google.common.collect.Lists;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Link;

import io.imunity.upman.utils.UpManGridHelper;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Displays a grid with invitations
 * 
 * @author P.Piernik
 *
 */
class ProjectInvitationsGrid extends GridWithActionColumn<ProjectInvitationEntry>
{

	enum BaseColumn
	{
		email("Invitation.email"), groups("Invitation.groups"), requested("Invitation.lastSent"), expiration(
				"Invitation.expiration"), link("Invitation.link"), action("Invitation.action");

		private String captionKey;

		BaseColumn(String captionKey)
		{
			this.captionKey = captionKey;
		}
	};

	public ProjectInvitationsGrid(MessageSource msg, List<SingleActionHandler<ProjectInvitationEntry>> rowActionHandlers)
	{
		super(msg, Lists.newArrayList(), false, false);
		addHamburgerActions(rowActionHandlers);
		setIdProvider(e -> e.code);
		createColumns();
		setMultiSelect(true);
		setStyleGenerator(e -> {
			if (e.expirationTime.isBefore(Instant.now()))
				return "warn";

			return null;
		});

	}

	private void createColumns()
	{
		addSortableColumn(ie -> ie.email , msg.getMessage(BaseColumn.email.captionKey) ,2).setResizable(true);
		UpManGridHelper.createGroupsColumn(this, (ProjectInvitationEntry e) -> e.groupsDisplayedNames,
				msg.getMessage(BaseColumn.groups.captionKey));

		UpManGridHelper.createDateTimeColumn(this, (ProjectInvitationEntry e) -> e.requestedTime,
				msg.getMessage(BaseColumn.requested.captionKey));
		UpManGridHelper.createDateTimeColumn(this, (ProjectInvitationEntry e) -> e.expirationTime,
				msg.getMessage(BaseColumn.expiration.captionKey));
	
		addComponentColumn(ie -> 
		{
			Link link = new Link();
			link.setCaptionAsHtml(true);
			link.setCaption(Images.external_link.getHtml());
			link.setTargetName("_blank");
			link.setResource(new ExternalResource(ie.link));
			return link;
		}, msg.getMessage(BaseColumn.link.captionKey), 1).setWidth(80).setResizable(false);
	}
}
