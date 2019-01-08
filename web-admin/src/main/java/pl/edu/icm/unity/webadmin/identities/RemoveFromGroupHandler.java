/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Factory of actions which show remove from group confirmation 
 * and if accepted remove selected users from given group.
 * 
 * @author K. Benedyczak
 */
@Component
class RemoveFromGroupHandler
{
	private GroupsManagement groupsMan;
	private UnityMessageSource msg;
	
	@Autowired
	RemoveFromGroupHandler(GroupsManagement groupsMan, UnityMessageSource msg)
	{
		this.groupsMan = groupsMan;
		this.msg = msg;
	}

	public SingleActionHandler<IdentityEntry> getAction(Supplier<String> groupSupplier,
			Runnable refresher)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.removeFromGroupAction"))
				.withIcon(Images.removeFromGroup.getResource())
				.withHandler(selection -> showRemoveFromGroupDialog(selection,
						groupSupplier, refresher))
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
