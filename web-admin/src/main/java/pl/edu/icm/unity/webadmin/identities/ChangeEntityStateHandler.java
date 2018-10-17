/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Factory of actions which allow for changing entity status
 * 
 * @author K. Benedyczak
 */
@Component
class ChangeEntityStateHandler
{
	@Autowired
	private EntityManagement identitiesMan;
	@Autowired
	private UnityMessageSource msg;
	
	SingleActionHandler<IdentityEntry> getAction(Runnable refreshCallback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.changeEntityStatusAction"))
				.withIcon(Images.editUser.getResource())
				.withHandler(selection -> showDialog(selection,
						refreshCallback))
				.build();
	}
	
	private void showDialog(Set<IdentityEntry> selection, Runnable refreshCallback)
	{       
		EntityWithLabel entity = selection.iterator().next().getSourceEntity();
		new ChangeEntityStateDialog(msg, entity, newState -> 
			setEntityStatus(entity.getEntity().getId(), newState, refreshCallback)
		).show();
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
			NotificationPopup.showError(msg, msg.getMessage("Identities.changeEntityStatusError"), e);
			return false;
		}
	}
}
