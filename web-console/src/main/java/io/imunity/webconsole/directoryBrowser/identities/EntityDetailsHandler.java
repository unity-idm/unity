/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Factory of actions which allow for viewing entity details
 * 
 * @author K. Benedyczak
 */
@Component
class EntityDetailsHandler
{
	@Autowired
	private ObjectFactory<EntityDetailsPanel> entityDetailsPanelFactory;

	@Autowired
	private EntityManagement entityMan;

	@Autowired
	private MessageSource msg;

	SingleActionHandler<IdentityEntry> getShowEntityAction()
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.showEntityDetails"))
				.withIcon(Images.userMagnifier.getResource()).withHandler(this::showEntityDetails)
				.build();
	}

	private void showEntityDetails(Set<IdentityEntry> selection)
	{
		IdentityEntry selected = selection.iterator().next();
		EntityWithLabel entity = selected.getSourceEntity();
		final EntityDetailsPanel identityDetailsPanel = entityDetailsPanelFactory.getObject();
		Collection<GroupMembership> groups;
		try
		{
			groups = entityMan.getGroups(new EntityParam(entity.getEntity().getId())).values();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"), e);
			return;
		}
		identityDetailsPanel.setInput(entity, groups);
		new EntityDetailsDialog(msg, identityDetailsPanel).show();
	}
}
