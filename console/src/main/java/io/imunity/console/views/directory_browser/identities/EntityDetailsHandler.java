/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.SingleActionHandler;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;

import java.util.Collection;
import java.util.Set;


@Component
class EntityDetailsHandler
{
	private final ObjectFactory<EntityDetailsPanel> entityDetailsPanelFactory;
	private final EntityManagement entityMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	EntityDetailsHandler(ObjectFactory<EntityDetailsPanel> entityDetailsPanelFactory, EntityManagement entityMan,
			MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.entityDetailsPanelFactory = entityDetailsPanelFactory;
		this.entityMan = entityMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getShowEntityAction()
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.showEntityDetails"))
				.withIcon(VaadinIcon.SEARCH).withHandler(this::showEntityDetails)
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
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			return;
		}
		identityDetailsPanel.setInput(entity, groups);
		new EntityDetailsDialog(msg, identityDetailsPanel).open();
	}
}
