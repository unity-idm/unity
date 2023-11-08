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
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;

import java.util.Set;


@Component
class IdentityConfirmationResendHandler
{
	private final EmailConfirmationManager confirmationMan;
	private final IdentityTypeSupport idTypeSupport;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	IdentityConfirmationResendHandler(EmailConfirmationManager confirmationMan,
			IdentityTypeSupport idTypeSupport,
			MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.confirmationMan = confirmationMan;
		this.idTypeSupport = idTypeSupport;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getAction()
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.resendConfirmationAction"))
				.withIcon(VaadinIcon.ENVELOPE_O)
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
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("Identities.confirmResendConfirmation",
						IdentitiesMessageHelper.getConfirmTextForIdentitiesNodes(msg, selection)),
				msg.getMessage("ok"),
				e -> sendConfirmation(selection),
				msg.getMessage("cancel"),
				e -> {}
		).open();
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
				notificationPresenter.showError(
						msg.getMessage("Identities.cannotSendConfirmation"), e.getMessage());
			}
		}
	}
}
