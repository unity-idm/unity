/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;


@Component
class DeleteEntityHandler
{
	private final EntityManagement identitiesMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	DeleteEntityHandler(EntityManagement identitiesMan, MessageSource msg,
			NotificationPresenter notificationPresenter)
	{
		this.identitiesMan = identitiesMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getAction(Consumer<EntityWithLabel> callback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.deleteEntityAction"))
				.withIcon(VaadinIcon.TRASH)
				.withHandler(selection -> showConfirmationDialog(selection,
						callback))
				.multiTarget()
				.build();
	}
	
	void showConfirmationDialog(Set<IdentityEntry> selection, 
			Consumer<EntityWithLabel> callback)
	{       
		final HashMap<Long, EntityWithLabel> toRemove = new HashMap<>();
		for (IdentityEntry node : selection)
			toRemove.put(node.getSourceEntity().getEntity().getId(), 
					node.getSourceEntity());
		String confirmText = MessageUtils.createConfirmFromStrings(msg, toRemove.values());
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("Identities.confirmEntityDelete", confirmText),
				msg.getMessage("ok"),
				e -> {
					for (EntityWithLabel entity : toRemove.values())
						removeEntity(entity, callback);
				},
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}
	
	private void removeEntity(EntityWithLabel removed, Consumer<EntityWithLabel> callback)
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		long entityId = removed.getEntity().getId();
		if (entityId == entity.getEntityId())
		{
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("Identities.notRemovingLoggedUser"));
			return;
		}
		try
		{
			identitiesMan.removeEntity(new EntityParam(entityId));
			callback.accept(removed);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Identities.removeEntityError"), e.getMessage());
		}
	}
}
