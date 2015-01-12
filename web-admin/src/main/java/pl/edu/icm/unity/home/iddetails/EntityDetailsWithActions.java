/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Set;

import pl.edu.icm.unity.home.HomeEndpointProperties;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

/**
 * Shows {@link EntityDetailsPanel} and the {@link EntityRemovalButton} under it. This class also controls whether to
 * show the subcomponents.
 * @author K. Benedyczak
 */
public class EntityDetailsWithActions extends CustomComponent
{
	public EntityDetailsWithActions(Set<String> disabled,
			UserDetailsPanel detailsPanel, UserAttributesPanel attrsPanel, 
			EntityRemovalButton removalButton)
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		if (!disabled.contains(HomeEndpointProperties.Components.userInfo.toString()))
			main.addComponent(detailsPanel);
		
		if (!disabled.contains(HomeEndpointProperties.Components.attributesManagement.toString()))
			main.addComponent(attrsPanel);
		
		if (!disabled.contains(HomeEndpointProperties.Components.accountRemoval.toString()))
		{
			main.addComponent(removalButton);
			main.setComponentAlignment(removalButton, Alignment.BOTTOM_RIGHT);
		}
		setCompositionRoot(main);
	}
}
