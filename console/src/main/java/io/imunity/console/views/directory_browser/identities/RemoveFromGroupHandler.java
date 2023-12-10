/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.SingleActionHandler;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;


@Component
class RemoveFromGroupHandler
{
	private final GroupsManagement groupsMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	RemoveFromGroupHandler(GroupsManagement groupsMan, MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.groupsMan = groupsMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getAction(Supplier<String> groupSupplier,
			Runnable refresher)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.removeFromGroupAction"))
				.withIcon(VaadinIcon.BAN)
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
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("Identities.confirmRemoveFromGroup", confirmText, groupSupplier.get()),
				msg.getMessage("ok"),
				e ->
				{
					for (EntityWithLabel en : toRemove.values())
						removeFromGroup(en.getEntity().getId(),
								groupSupplier.get());
					refresher.run();
				},
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}
	
	private void removeFromGroup(long entityId, String group)
	{
		try
		{
			groupsMan.removeMember(group, new EntityParam(entityId));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Identities.removeFromGroupError"), e.getMessage());
		}
	}
}
