/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;

import java.util.Set;
import java.util.function.Consumer;


@Component
class DeleteIdentityHandler
{
	private final IdentityTypeSupport idTypeSupport;
	private final EntityManagement identitiesMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	DeleteIdentityHandler(IdentityTypeSupport idTypeSupport, EntityManagement identitiesMan, MessageSource msg,
			NotificationPresenter notificationPresenter)
	{
		this.idTypeSupport = idTypeSupport;
		this.identitiesMan = identitiesMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getAction(Consumer<IdentityEntry> removeCallback,
			Runnable resetAllCallback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.deleteIdentityAction"))
				.withIcon(VaadinIcon.TRASH)
				.withHandler(selection -> showConfirmationDialog(selection,
						removeCallback, resetAllCallback))
				.multiTarget()
				.withDisabledPredicate(ie -> ie.getSourceIdentity() == null)
				.build();
	}
	
	private void showConfirmationDialog(Set<IdentityEntry> selection, 
			Consumer<IdentityEntry> removeCallback,
			Runnable resetAllCallback)
	{
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("Identities.confirmIdentityDelete", IdentitiesMessageHelper.getConfirmTextForIdentitiesNodes(msg, selection)),
				msg.getMessage("ok"),
				e -> {
					boolean requiresRefresh = false;
					for (IdentityEntry id : selection)
					{
						if (idTypeSupport.getTypeDefinition(id.getSourceIdentity().getTypeId())
								.isRemovable())
						{
							removeIdentity(id, removeCallback);
						} else
						{
							resetIdentity(id);
							requiresRefresh = true;
						}
					}
					if (requiresRefresh)
						resetAllCallback.run();
				},
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}
	
	private void removeIdentity(IdentityEntry removed, Consumer<IdentityEntry> removeCallback)
	{
		try
		{
			identitiesMan.removeIdentity(removed.getSourceIdentity());
			removeCallback.accept(removed);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Identities.removeIdentityError"), e.getMessage());
		}
	}
	
	private void resetIdentity(IdentityEntry identityEntry)
	{
		try
		{
			Identity identity = identityEntry.getSourceIdentity();
			identitiesMan.resetIdentity(new EntityParam(identity.getEntityId()), identity.getTypeId(), 
					identity.getRealm(), identity.getTarget());
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Identities.removeIdentityError"), e.getMessage());
		}
	}
}
