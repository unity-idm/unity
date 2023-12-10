/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.SingleActionHandler;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;

import java.util.Set;


@Component
class IdentityConfirmHandler
{
	private final EntityManagement entityManagement;
	private final IdentityTypeSupport idTypeSupport;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	IdentityConfirmHandler(EntityManagement entityManagement, IdentityTypeSupport idTypeSupport,
			MessageSource msg,
			NotificationPresenter notificationPresenter)
	{
		this.entityManagement = entityManagement;
		this.idTypeSupport = idTypeSupport;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getAction(Runnable refreshCallback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("IdentityConfirmHandler.confirmAction"))
				.withIcon(VaadinIcon.CHEVRON_DOWN_SMALL)
				.withDisabledPredicate(ie -> ie.getSourceIdentity() == null ||
						!identityIsVerifiable(ie) ||
						identityIsConfirmed(ie))
				.withHandler(selection -> confirm(selection, refreshCallback))
				.multiTarget()
				.build();
	}
	
	private boolean identityIsConfirmed(IdentityEntry id)
	{
		return id.getSourceIdentity().getConfirmationInfo().isConfirmed();		
	}
	
	private boolean identityIsVerifiable(IdentityEntry id)
	{
		return idTypeSupport.getTypeDefinition(id.getSourceIdentity().getTypeId()).isEmailVerifiable();
	}

	private void confirm(Set<IdentityEntry> selection, Runnable refreshCallback)
	{       
		for (IdentityEntry selected : selection)
		{
			Identity id = selected.getSourceIdentity();
			try
			{
				Identity updated = id.clone();
				updated.setConfirmationInfo(new ConfirmationInfo(true));
				entityManagement.updateIdentity(id, updated);
			} catch (EngineException e)
			{
				notificationPresenter.showError(msg.getMessage("IdentityConfirmHandler.cannotConfirm"), e.getMessage());
			}
		}
		refreshCallback.run();
	}
}
