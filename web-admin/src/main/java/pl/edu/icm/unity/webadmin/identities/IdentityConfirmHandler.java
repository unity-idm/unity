/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Factory of actions which set identity as confirmed.
 * 
 * @author K. Benedyczak
 */
@Component
class IdentityConfirmHandler
{
	@Autowired
	private EntityManagement entityManagement;
	@Autowired
	private IdentityTypeSupport idTypeSupport;
	@Autowired
	private UnityMessageSource msg;
	
	public SingleActionHandler<IdentityEntry> getAction(Runnable refreshCallback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("IdentityConfirmHandler.confirmAction"))
				.withIcon(Images.ok_small.getResource())
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
				NotificationPopup.showError(msg, msg.getMessage("IdentityConfirmHandler.cannotConfirm"), e);
			}
		}
		refreshCallback.run();
	}
}
