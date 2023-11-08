/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.*;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Factory of actions which show remove from group confirmation 
 * and if accepted remove selected users from given group.
 * 
 * @author K. Benedyczak
 */
@Component("RemoveFromGroupHandlerV8")
class RemoveFromGroupHandler
{
	private GroupsManagement groupsMan;
	private MessageSource msg;
	
	@Autowired
	RemoveFromGroupHandler(GroupsManagement groupsMan, MessageSource msg)
	{
		this.groupsMan = groupsMan;
		this.msg = msg;
	}

	SingleActionHandler<IdentityEntry> getAction(Supplier<String> groupSupplier,
			Runnable refresher)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.removeFromGroupAction"))
				.withIcon(Images.removeFromGroup.getResource())
				.withHandler(selection -> showRemoveFromGroupDialog(selection,
						groupSupplier, refresher))
				.withDisabledPredicate(r -> groupSupplier.get().equals("/"))
				.multiTarget()
				.build();
	}
	
	private void showRemoveFromGroupDialog(Set<IdentityEntry> selection, 
			Supplier<String> groupSupplier,	Runnable refresher)
	{			
		final HashMap<Long, EntityWithLabel> toRemove = new HashMap<>();
		for (IdentityEntry node : selection)
			toRemove.put(node.getSourceEntity().getEntity().getId(), node.getSourceEntity());
		String confirmText = MessageUtils.createConfirmFromStrings(msg, toRemove.values());
		new ConfirmDialog(msg, msg.getMessage("Identities.confirmRemoveFromGroup",
				confirmText, groupSupplier.get()), () ->
				{
					for (EntityWithLabel en : toRemove.values())
						removeFromGroup(en.getEntity().getId(),
								groupSupplier.get());
					refresher.run();
				}).show();
	}
	
	private void removeFromGroup(long entityId, String group)
	{
		try
		{
			groupsMan.removeMember(group, new EntityParam(entityId));
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Identities.removeFromGroupError"), e);
		}
	}
}
