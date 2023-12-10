/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.console.views.directory_browser.EntityWithLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.SingleActionHandler;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;

import java.util.Set;

@Component
class ChangeCredentialRequirementHandler
{
	private final CredentialRequirementManagement credReqMan;
	private final EntityCredentialManagement eCredMan;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	ChangeCredentialRequirementHandler(CredentialRequirementManagement credReqMan,
			EntityCredentialManagement eCredMan, MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.credReqMan = credReqMan;
		this.eCredMan = eCredMan;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	SingleActionHandler<IdentityEntry> getAction(Runnable refreshCallback)
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.changeCredentialRequirementAction"))
				.withIcon(VaadinIcon.KEY)
				.withHandler(selection -> showDialog(selection,
						refreshCallback))
				.build();
	}
	
	private void showDialog(Set<IdentityEntry> selection, Runnable refreshCallback)
	{       
		EntityWithLabel entity = selection.iterator().next().getSourceEntity();
		String currentCredId = entity.getEntity().getCredentialInfo()
				.getCredentialRequirementId();
		new ChangeCredentialRequirementDialog(msg, entity, currentCredId, eCredMan,
				credReqMan, refreshCallback::run, notificationPresenter).open();
	}
}
