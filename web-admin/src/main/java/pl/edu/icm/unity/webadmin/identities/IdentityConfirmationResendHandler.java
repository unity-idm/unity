/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Factory of actions which trigger re-sending of (verifiable) identity confirmation.
 * 
 * @author K. Benedyczak
 */
@Component
class IdentityConfirmationResendHandler
{
	@Autowired
	private EmailConfirmationManager confirmationMan;
	@Autowired
	private IdentityTypeSupport idTypeSupport;
	@Autowired
	private UnityMessageSource msg;
	
	public SingleActionHandler<IdentityEntry> getAction()
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.resendConfirmationAction"))
				.withIcon(Images.messageSend.getResource())
				.withDisabledPredicate(ie -> ie.getSourceIdentity() == null ||
						!identityIsVerifiable(ie) ||
						identityIsConfirmed(ie))
				.withHandler(this::showConfirmationDialog)
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

	
	public void showConfirmationDialog(Set<IdentityEntry> selection)
	{       
		new ConfirmDialog(msg,
				msg.getMessage("Identities.confirmResendConfirmation",
						IdentitiesComponent.getConfirmTextForIdentitiesNodes(msg, selection)),
				() -> sendConfirmation(selection))
		.show();
	}
	
	private void sendConfirmation(Set<IdentityEntry> selection)
	{
		for (IdentityEntry selected : selection)
		{
			Identity id = selected.getSourceIdentity();
			try
			{
				confirmationMan.sendVerification(
						new EntityParam(id.getEntityId()), id);
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, 
						msg.getMessage("Identities.cannotSendConfirmation"), e);
			}
		}
	}
}
