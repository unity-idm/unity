/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Factory of actions which (after confirmation) trigger removal of identities
 * 
 * @author K. Benedyczak
 */
@Component
class DeleteIdentityHandler
{
	@Autowired
	private IdentityTypeSupport idTypeSupport;
	@Autowired
	private EntityManagement identitiesMan;
	@Autowired
	private UnityMessageSource msg;
	
	SingleActionHandler<IdentityEntry> getAction(Consumer<IdentityEntry> removeCallback,
			Runnable resetAllCallback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.deleteIdentityAction"))
				.withIcon(Images.deleteIdentity.getResource())
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
		new ConfirmDialog(msg, msg.getMessage("Identities.confirmIdentityDelete",
				IdentitiesComponent.getConfirmTextForIdentitiesNodes(msg, selection)), () ->
			{
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
		}).show();

	}
	
	private void removeIdentity(IdentityEntry removed, Consumer<IdentityEntry> removeCallback)
	{
		try
		{
			identitiesMan.removeIdentity(removed.getSourceIdentity());
			removeCallback.accept(removed);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Identities.removeIdentityError"), e);
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
			NotificationPopup.showError(msg, msg.getMessage("Identities.removeIdentityError"), e);
		}
	}
}
