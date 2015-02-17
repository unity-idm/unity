/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Images;

import com.vaadin.ui.Button;

/**
 * Button allowing to launch {@link RemoveEntityDialog}
 * @author K. Benedyczak
 */
public class EntityRemovalButton extends Button
{
	public EntityRemovalButton(final UnityMessageSource msg, final long entity, 
			final IdentitiesManagement identitiesManagement, final WebAuthenticationProcessor authnProcessor)
	{
		super(msg.getMessage("EntityRemovalButton.removeAccount"), Images.delete.getResource());
		addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				new RemoveEntityDialog(msg, entity, identitiesManagement, authnProcessor).show();
			}
		});
	}
}
