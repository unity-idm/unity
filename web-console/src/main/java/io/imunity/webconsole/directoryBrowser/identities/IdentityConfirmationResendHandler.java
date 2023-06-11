/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
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
	private MessageSource msg;
	
	SingleActionHandler<IdentityEntry> getAction()
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

	
	void showConfirmationDialog(Set<IdentityEntry> selection)
	{       
		new ConfirmDialog(msg,
				msg.getMessage("Identities.confirmResendConfirmation",
						IdentitiesMessageHelper.getConfirmTextForIdentitiesNodes(msg, selection)),
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
