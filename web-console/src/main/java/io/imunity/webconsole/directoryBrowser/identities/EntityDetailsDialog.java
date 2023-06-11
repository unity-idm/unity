/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;

/**
 * Simple dialog showing a given {@link EntityDetailsPanel}
 * @author K. Benedyczak
 */
public class EntityDetailsDialog extends AbstractDialog
{
	private EntityDetailsPanel contents;
	
	public EntityDetailsDialog(MessageSource msg, EntityDetailsPanel contents)
	{
		super(msg, msg.getMessage("IdentityDetails.entityDetailsCaption"),
				msg.getMessage("close"));
		this.contents = contents;
	}
	
	@Override
	protected void onConfirm()
	{
		close();
	}
	
	@Override
	protected com.vaadin.ui.Component getContents() throws Exception
	{
		return contents;
	}
}
