/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Factory of actions which (after confirmation) trigger removal of entity
 * 
 * @author K. Benedyczak
 */
@Component
class DeleteEntityHandler
{
	@Autowired
	private EntityManagement identitiesMan;
	@Autowired
	private UnityMessageSource msg;
	
	public SingleActionHandler<IdentityEntry> getAction(Consumer<EntityWithLabel> callback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.deleteEntityAction"))
				.withIcon(Images.deleteEntity.getResource())
				.withHandler(selection -> showConfirmationDialog(selection,
						callback))
				.multiTarget()
				.build();
	}
	
	public void showConfirmationDialog(Set<IdentityEntry> selection, 
			Consumer<EntityWithLabel> callback)
	{       
		final HashMap<Long, EntityWithLabel> toRemove = new HashMap<>();
		for (IdentityEntry node : selection)
			toRemove.put(node.getSourceEntity().getEntity().getId(), 
					node.getSourceEntity());
		String confirmText = MessageUtils.createConfirmFromStrings(msg, toRemove.values());
		new ConfirmDialog(msg, msg.getMessage("Identities.confirmEntityDelete",
				confirmText), () ->
		{
			for (EntityWithLabel entity : toRemove.values())
				removeEntity(entity, callback);
		}).show();
	}
	
	private void removeEntity(EntityWithLabel removed, Consumer<EntityWithLabel> callback)
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		long entityId = removed.getEntity().getId();
		if (entityId == entity.getEntityId())
		{
			NotificationPopup.showError(msg.getMessage("error"), 
					msg.getMessage("Identities.notRemovingLoggedUser"));
			return;
		}
		try
		{
			identitiesMan.removeEntity(new EntityParam(entityId));
			callback.accept(removed);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Identities.removeEntityError"), e);
		}
	}
}
