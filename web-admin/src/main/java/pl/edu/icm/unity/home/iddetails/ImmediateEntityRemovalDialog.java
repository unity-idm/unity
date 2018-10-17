/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.HomeEndpointProperties.RemovalModes;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Dialog allowing to perform user triggered account (entity) removal. 
 * Removal is performed immediately after confirmation.
 * Note that the actual action is configurable: it may happen that administrator 
 * configures system to merely disable account, to perform some cleanup operations in 
 * relaying systems.
 *  
 * @author K. Benedyczak
 */
public class ImmediateEntityRemovalDialog extends ConfirmDialog
{
	private long entity;
	private StandardWebAuthenticationProcessor authnProcessor;
	private EntityManagement identitiesMan;
	private RemovalModes removalMode;
	
	public ImmediateEntityRemovalDialog(UnityMessageSource msg, long entityId, 
			EntityManagement identitiesManagement, 
			StandardWebAuthenticationProcessor authnProcessor,
			RemovalModes removalMode)
	{
		super(msg, msg.getMessage("RemoveEntityDialog.caption"),
				msg.getMessage("RemoveEntityDialog.confirmImmediate"), null);
		this.entity = entityId;
		this.identitiesMan = identitiesManagement;
		this.authnProcessor = authnProcessor;
		this.removalMode = removalMode;
		setSizeMode(SizeMode.SMALL);
		super.setCallback(this::performRemoval);
	}

	private void performRemoval()
	{
		try
		{
			EntityParam entityP = new EntityParam(entity);
			switch(removalMode)
			{
			case blockAuthentication:
				identitiesMan.setEntityStatus(entityP, EntityState.authenticationDisabled);
				break;
			case disable:
				identitiesMan.setEntityStatus(entityP, EntityState.disabled);
				break;
			case remove:
				identitiesMan.removeEntity(entityP);
				break;
			}
			
			close();
			authnProcessor.logout();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RemoveEntityDialog.scheduleFailed"), e);
		}
	}
}
