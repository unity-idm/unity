/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Images;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;

/**
 * Shows {@link UserDetailsPanel}, {@link UserIdentitiesPanel}, {@link UserAttributesPanel}
 * and the {@link EntityRemovalButton} under it. This class also controls whether to
 * show the subcomponents.
 * @author K. Benedyczak
 */
public class EntityDetailsWithActions extends CustomComponent
{
	private UnityMessageSource msg;
	private UserAttributesPanel attrsPanel;
	private UserIdentitiesPanel identitiesPanel;

	public EntityDetailsWithActions(Set<String> disabled,
			UserDetailsPanel detailsPanel, UserIdentitiesPanel identitiesPanel,
			UserAttributesPanel attrsPanel, 
			EntityRemovalButton removalButton, UnityMessageSource msg) throws EngineException
	{
		this.identitiesPanel = identitiesPanel;
		this.attrsPanel = attrsPanel;
		this.msg = msg;
		boolean showButtons = false;
		FormLayout main = new FormLayout();
		if (!disabled.contains(HomeEndpointProperties.Components.userInfo.toString()))
			detailsPanel.addIntoLayout(main);
		
		if (!disabled.contains(HomeEndpointProperties.Components.identitiesManagement.toString()))
		{
			identitiesPanel.addIntoLayout(main);
			showButtons = identitiesPanel.hasEditable();
		}

		if (!disabled.contains(HomeEndpointProperties.Components.attributesManagement.toString()))
		{
			attrsPanel.addIntoLayout(main);
			showButtons |= attrsPanel.hasEditable();
		}
		
		if (showButtons)
			main.addComponent(getButtonsBar());

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
	
	private Component getButtonsBar()
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setSpacing(true);

		Button save = new Button(msg.getMessage("save"));
		save.setIcon(Images.save.getResource());
		save.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				attrsPanel.saveChanges();
				//TODO save identities
			}
		});

		Button refresh = new Button(msg.getMessage("refresh"));
		refresh.setIcon(Images.refresh.getResource());
		refresh.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				attrsPanel.refreshEditable();
				//TODO refresh identities
			}
		});
		buttons.addComponents(save, refresh);
		return buttons;   

	}
}
