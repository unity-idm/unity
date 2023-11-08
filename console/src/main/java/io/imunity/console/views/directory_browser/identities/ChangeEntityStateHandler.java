/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;

import java.util.Set;

@Component
class ChangeEntityStateHandler
{
	private final EntityManagement identitiesMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	ChangeEntityStateHandler(EntityManagement identitiesMan, MessageSource msg,
			NotificationPresenter notificationPresenter)
	{
		this.identitiesMan = identitiesMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getAction(Runnable refreshCallback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.changeEntityStatusAction"))
				.withIcon(VaadinIcon.POWER_OFF)
				.withHandler(selection -> showDialog(selection,
						refreshCallback))
				.build();
	}
	
	private void showDialog(Set<IdentityEntry> selection, Runnable refreshCallback)
	{       
		EntityWithLabel entity = selection.iterator().next().getSourceEntity();
		new ChangeEntityStateDialog(msg, entity, newState -> 
			setEntityStatus(entity.getEntity().getId(), newState, refreshCallback)
		).open();
	}
	
	private boolean setEntityStatus(long entityId, EntityInformation newState, 
			Runnable refreshCallback)
	{
		try
		{
			EntityParam entity = new EntityParam(entityId);
			
			if (newState.getState() != EntityState.onlyLoginPermitted)
				identitiesMan.setEntityStatus(entity, newState.getState());
			identitiesMan.scheduleEntityChange(entity, newState.getScheduledOperationTime(), 
						newState.getScheduledOperation());
			refreshCallback.run();
			return true;
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Identities.changeEntityStatusError"), e.getMessage());
			return false;
		}
	}
}
