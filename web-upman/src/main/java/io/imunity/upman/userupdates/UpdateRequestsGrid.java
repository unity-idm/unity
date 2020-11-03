/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import java.util.List;

import com.google.common.collect.Lists;

import io.imunity.upman.utils.UpManGridHelper;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

/**
 * Displays a grid with update requests
 * 
 * @author P.Piernik
 *
 */

public class UpdateRequestsGrid extends GridWithActionColumn<UpdateRequestEntry>
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

	public UpdateRequestsGrid(MessageSource msg,
			List<SingleActionHandler<UpdateRequestEntry>> rowActionHandlers, ConfirmationInfoFormatter formatter)
	{
		super(msg, Lists.newArrayList(), false, false);
		addHamburgerActions(rowActionHandlers);
		setIdProvider(e -> e.id);
		setMultiSelect(true);
		createColumns(formatter);
	}

	private void createColumns(ConfirmationInfoFormatter formatter)
	{
		addColumn(r -> r.operation != null
				? msg.getMessage("UpdateRequest." + r.operation.toString().toLowerCase())
				: null, msg.getMessage(BaseColumn.operation.captionKey), 2);
		addColumn(r -> r.name, msg.getMessage(BaseColumn.name.captionKey), 2);
		
		UpManGridHelper.createEmailColumn(this, (UpdateRequestEntry e) -> e.email,
				msg.getMessage(BaseColumn.email.captionKey), formatter);

		UpManGridHelper.createGroupsColumn(this, (UpdateRequestEntry e) -> e.groupsDisplayedNames,
				msg.getMessage(BaseColumn.groups.captionKey));

		UpManGridHelper.createDateTimeColumn(this, (UpdateRequestEntry e) -> e.requestedTime,
				msg.getMessage(BaseColumn.requested.captionKey));
	}
}
