/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home.iddetails;

import com.vaadin.ui.Button;

import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.HomeEndpointProperties.RemovalModes;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Button allowing to launch {@link ScheduledEntityRemovalDialog}
 * @author K. Benedyczak
 */
public class EntityRemovalButton extends Button
{
	public EntityRemovalButton(final MessageSource msg, final long entity, 
			final EntityManagement identitiesManagement, 
			final EntityManagement insecureIdentitiesManagement, 
			final StandardWebLogoutHandler authnProcessor,
			HomeEndpointProperties config)
	{
		super(msg.getMessage("EntityRemovalButton.removeAccount"), Images.delete.getResource());
		addClickListener((event) ->
		{
			if (config.getBooleanValue(HomeEndpointProperties.DISABLE_REMOVAL_SCHEDULE))
			{
				RemovalModes removalMode = config.getEnumValue(
						HomeEndpointProperties.REMOVAL_MODE, RemovalModes.class);
				new ImmediateEntityRemovalDialog(msg, entity, insecureIdentitiesManagement, 
						authnProcessor, removalMode).show();
			} else
			{
				new ScheduledEntityRemovalDialog(msg, entity, identitiesManagement, 
						authnProcessor).show();
			}
		});
	}
}
