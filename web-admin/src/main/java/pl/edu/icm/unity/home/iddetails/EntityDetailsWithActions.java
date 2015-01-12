/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.HomeEndpointProperties;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;

/**
 * Shows {@link EntityDetailsPanel} and the {@link EntityRemovalButton} under it. This class also controls whether to
 * show the subcomponents.
 * @author K. Benedyczak
 */
public class EntityDetailsWithActions extends CustomComponent
{
	public EntityDetailsWithActions(Set<String> disabled,
			UserDetailsPanel detailsPanel, UserAttributesPanel attrsPanel, 
			EntityRemovalButton removalButton) throws EngineException
	{
		FormLayout main = new FormLayout();
		if (!disabled.contains(HomeEndpointProperties.Components.userInfo.toString()))
			detailsPanel.addIntoLayout(main);
		
		if (!disabled.contains(HomeEndpointProperties.Components.attributesManagement.toString()))
			attrsPanel.addIntoLayout(main);
		
		HorizontalLayout actions = new HorizontalLayout();
		actions.setSpacing(true);
		actions.setWidth(100, Unit.PERCENTAGE);
		main.addComponent(actions);
		
		if (!disabled.contains(HomeEndpointProperties.Components.accountRemoval.toString()))
		{
			actions.addComponent(removalButton);
			actions.setComponentAlignment(removalButton, Alignment.BOTTOM_RIGHT);
		}
		setCompositionRoot(main);
	}
}
