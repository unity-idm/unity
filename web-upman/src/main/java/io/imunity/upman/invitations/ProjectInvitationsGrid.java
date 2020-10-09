/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.time.Instant;
import java.util.List;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Link;

import io.imunity.upman.common.UpManGrid;
import io.imunity.upman.utils.UpManGridHelper;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Displays a grid with invitations
 * 
 * @author P.Piernik
 *
 */
class ProjectInvitationsGrid extends UpManGrid<ProjectInvitationEntry>
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
		super(msg, (ProjectInvitationEntry e) -> e.code);
		createColumns(rowActionHandlers);
		setStyleGenerator(e -> {
			if (e.expirationTime.isBefore(Instant.now()))
				return "warn";

			return null;
		});

	}

	private void createBaseColumns()
	{
		addColumn(ie -> ie.email).setCaption(msg.getMessage(BaseColumn.email.captionKey)).setExpandRatio(2);
		UpManGridHelper.createGroupsColumn(this, (ProjectInvitationEntry e) -> e.groupsDisplayedNames,
				msg.getMessage(BaseColumn.groups.captionKey)).setExpandRatio(2);

		UpManGridHelper.createDateTimeColumn(this, (ProjectInvitationEntry e) -> e.requestedTime,
				msg.getMessage(BaseColumn.requested.captionKey)).setExpandRatio(2);
		UpManGridHelper.createDateTimeColumn(this, (ProjectInvitationEntry e) -> e.expirationTime,
				msg.getMessage(BaseColumn.expiration.captionKey)).setExpandRatio(2);

		addComponentColumn(ie -> 
		{
			Link link = new Link();
			link.setCaptionAsHtml(true);
			link.setCaption(Images.external_link.getHtml());
			link.setTargetName("_blank");
			link.setResource(new ExternalResource(ie.link));
			return link;
		}).setCaption(msg.getMessage(BaseColumn.link.captionKey)).setWidth(80).setResizable(false);
	}

	private void createColumns(List<SingleActionHandler<ProjectInvitationEntry>> rowActionHandlers)
	{

		createBaseColumns();
		UpManGridHelper.createActionColumn(this, rowActionHandlers, msg.getMessage(BaseColumn.action.captionKey));
	}
}
