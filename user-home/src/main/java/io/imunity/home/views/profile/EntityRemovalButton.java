/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home.views.profile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.HomeEndpointProperties.RemovalModes;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.VaddinWebLogoutHandler;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;

class EntityRemovalButton extends Button
{
	EntityRemovalButton(MessageSource msg, long entity,
			EntityManagement identitiesManagement,
			EntityManagement insecureIdentitiesManagement,
			VaddinWebLogoutHandler authnProcessor,
			NotificationPresenter notificationPresenter,
			HomeEndpointProperties config)
	{
		super(msg.getMessage("EntityRemovalButton.removeAccount"));
		setIcon(VaadinIcon.TRASH.create());
		addClickListener((event) ->
		{
			if (config.getBooleanValue(HomeEndpointProperties.DISABLE_REMOVAL_SCHEDULE))
			{
				RemovalModes removalMode = config.getEnumValue(
						HomeEndpointProperties.REMOVAL_MODE, RemovalModes.class);
				new ImmediateEntityRemovalDialog(msg, entity, insecureIdentitiesManagement, 
						authnProcessor, removalMode, notificationPresenter).open();
			} else
			{
				new ScheduledEntityRemovalDialog(msg, entity, identitiesManagement, 
						authnProcessor, notificationPresenter).open();
			}
		});
	}
}
