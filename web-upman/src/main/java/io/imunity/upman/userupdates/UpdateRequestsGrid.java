/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import java.util.List;

import io.imunity.upman.common.UpManGrid;
import io.imunity.upman.utils.UpManGridHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

/**
 * Displays a grid with update requests
 * 
 * @author P.Piernik
 *
 */

public class UpdateRequestsGrid extends UpManGrid<UpdateRequestEntry>
{

	enum BaseColumn
	{
		operation("UpdateRequest.operation"), name("UpdateRequest.name"), email("UpdateRequest.email"), groups(
				"UpdateRequest.groups"), requested(
						"UpdateRequest.requested"), action("UpdateRequest.action");

		private String captionKey;

		BaseColumn(String captionKey)
		{
			this.captionKey = captionKey;
		}
	};

	public UpdateRequestsGrid(UnityMessageSource msg,
			List<SingleActionHandler<UpdateRequestEntry>> rowActionHandlers, ConfirmationInfoFormatter formatter)
	{
		super(msg, (UpdateRequestEntry e) -> e.id);
		createColumns(rowActionHandlers, formatter);
	}

	private void createBaseColumns(ConfirmationInfoFormatter formatter)
	{

		addColumn(r -> r.operation != null
				? msg.getMessage("UpdateRequest." + r.operation.toString().toLowerCase())
				: null).setCaption(msg.getMessage(BaseColumn.operation.captionKey)).setExpandRatio(2);
		addColumn(r -> r.name).setCaption(msg.getMessage(BaseColumn.name.captionKey)).setExpandRatio(2);
		
		UpManGridHelper.createEmailColumn(this, (UpdateRequestEntry e) -> e.email,
				msg.getMessage(BaseColumn.email.captionKey), formatter);

		UpManGridHelper.createGroupsColumn(this, (UpdateRequestEntry e) -> e.groupsDisplayedNames,
				msg.getMessage(BaseColumn.groups.captionKey)).setExpandRatio(4);

		UpManGridHelper.createDateTimeColumn(this, (UpdateRequestEntry e) -> e.requestedTime,
				msg.getMessage(BaseColumn.requested.captionKey)).setExpandRatio(2);

	}

	private void createColumns(List<SingleActionHandler<UpdateRequestEntry>> rowActionHandlers, ConfirmationInfoFormatter formatter)
	{
		createBaseColumns(formatter);
		UpManGridHelper.createActionColumn(this, rowActionHandlers,
				msg.getMessage(BaseColumn.action.captionKey));
	}

}
