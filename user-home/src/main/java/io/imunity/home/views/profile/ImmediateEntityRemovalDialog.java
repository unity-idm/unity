/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home.views.profile;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import io.imunity.home.HomeEndpointProperties.RemovalModes;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;

/**
 * Dialog allowing to perform user triggered account (entity) removal. 
 * Removal is performed immediately after confirmation.
 * Note that the actual action is configurable: it may happen that administrator 
 * configures system to merely disable account, to perform some cleanup operations in 
 * relaying systems.
 */
class ImmediateEntityRemovalDialog extends ConfirmDialog
{
	private final long entity;
	private final MessageSource msg;
	private final VaddinWebLogoutHandler authnProcessor;
	private final EntityManagement identitiesMan;
	private final RemovalModes removalMode;
	private final NotificationPresenter notificationPresenter;

	ImmediateEntityRemovalDialog(MessageSource msg, long entityId,
										EntityManagement identitiesManagement,
										VaddinWebLogoutHandler authnProcessor,
										RemovalModes removalMode, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.entity = entityId;
		this.identitiesMan = identitiesManagement;
		this.authnProcessor = authnProcessor;
		this.removalMode = removalMode;
		this.notificationPresenter = notificationPresenter;
		setHeader(msg.getMessage("RemoveEntityDialog.caption"));
		setText(msg.getMessage("RemoveEntityDialog.confirmImmediate"));
		setConfirmButton(msg.getMessage("ok"), e -> performRemoval());
	}

	private void performRemoval()
	{
		try
		{
			EntityParam entityP = new EntityParam(entity);
			switch (removalMode)
			{
				case blockAuthentication -> identitiesMan.setEntityStatus(entityP, EntityState.authenticationDisabled);
				case disable -> identitiesMan.setEntityStatus(entityP, EntityState.disabled);
				case remove -> identitiesMan.removeEntity(entityP);
			}
			
			close();
			authnProcessor.logout();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("RemoveEntityDialog.scheduleFailed"), e.getMessage());
		}
	}
}
