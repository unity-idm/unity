/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home.views.profile;

import com.vaadin.flow.component.html.Span;
import io.imunity.home.HomeEndpointProperties.RemovalModes;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;

/**
 * Dialog allowing to perform user triggered account (entity) removal. 
 * Removal is performed immediately after confirmation.
 * Note that the actual action is configurable: it may happen that administrator 
 * configures system to merely disable account, to perform some cleanup operations in 
 * relaying systems.
 */
class ImmediateEntityRemovalDialog extends DialogWithActionFooter
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ImmediateEntityRemovalDialog.class);

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
		super(msg::getMessage);
		this.msg = msg;
		this.entity = entityId;
		this.identitiesMan = identitiesManagement;
		this.authnProcessor = authnProcessor;
		this.removalMode = removalMode;
		this.notificationPresenter = notificationPresenter;
		setHeaderTitle(msg.getMessage("RemoveEntityDialog.caption"));
		add(new Span(msg.getMessage("RemoveEntityDialog.confirmImmediate")));
		setActionButton(msg.getMessage("ok"), this::performRemoval);
		setCancelButtonVisible(false);
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
			log.error("Removing the account failed", e);
		}
	}
}
