/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

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
		boolean showSave = false;
		VerticalLayout root = new VerticalLayout();
		FormLayout mainForm = new FormLayout();
		if (!disabled.contains(HomeEndpointProperties.Components.userInfo.toString()))
			detailsPanel.addIntoLayout(mainForm);
		
		if (!disabled.contains(HomeEndpointProperties.Components.identitiesManagement.toString()))
		{
			identitiesPanel.addIntoLayout(mainForm);
			showSave = identitiesPanel.hasEditable();
		}

		if (!disabled.contains(HomeEndpointProperties.Components.attributesManagement.toString()))
		{
			attrsPanel.addIntoLayout(mainForm);
			showSave |= attrsPanel.hasEditable();
		}
		
		root.addComponent(mainForm);
		root.addComponent(getButtonsBar(showSave, disabled, removalButton));
		setCompositionRoot(root);
	}
	
	private Component getButtonsBar(boolean showSave, Set<String> disabled, EntityRemovalButton removalButton)
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setSpacing(true);
		buttons.setWidth(100, Unit.PERCENTAGE);
		buttons.setMargin(new MarginInfo(false, false, true, false));

		if (showSave)
		{
			Button save = new Button(msg.getMessage("save"));
			save.setIcon(Images.save.getResource());
			save.addClickListener(new ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					boolean ok = true;
					try
					{
						attrsPanel.validate();
					} catch (FormValidationException e)
					{
						ok = false;
					}
					try
					{
						identitiesPanel.validate();
					} catch (FormValidationException e)
					{
						ok = false;
					}
					if (!ok)
						return;
					
					try
					{
						identitiesPanel.saveChanges();
						attrsPanel.saveChanges();
					} catch (EngineException e)
					{
						NotificationPopup.showError(msg, 
							msg.getMessage("EntityDetailsWithActions.errorSaving"), e);
					}
				}
			});
			buttons.addComponent(save);
			buttons.setComponentAlignment(save, Alignment.BOTTOM_LEFT);
		}
		
		Button refresh = new Button(msg.getMessage("refresh"));
		refresh.setIcon(Images.refresh.getResource());
		refresh.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				try
				{
					identitiesPanel.clear();
					attrsPanel.clear();
					identitiesPanel.refresh();
					attrsPanel.refreshEditable();
				} catch (EngineException e)
				{
					NotificationPopup.showError(msg, msg.getMessage("error"), e);
				}
			}
		});
		buttons.addComponent(refresh);
		buttons.setComponentAlignment(refresh, Alignment.BOTTOM_LEFT);
		
		if (!disabled.contains(HomeEndpointProperties.Components.accountRemoval.toString()))
		{
			buttons.addComponent(removalButton);
			buttons.setExpandRatio(removalButton, 1);
			buttons.setComponentAlignment(removalButton, Alignment.BOTTOM_RIGHT);
		}
		
		return buttons;   
	}
}
