/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home.iddetails;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import io.imunity.home.HomeEndpointProperties;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.association.afterlogin.ConnectIdWizardProvider;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.composite.CompositeLayoutAdapter;
import pl.edu.icm.unity.webui.sandbox.wizard.SandboxWizardDialog;

import java.util.Set;

/**
 * Shows {@link UserDetailsPanel}, {@link UserIdentitiesPanel}, {@link UserAttributesPanel}
 * and the  under it. This class also controls whether to
 * show the subcomponents.
 * @author K. Benedyczak
 */
public class EntityDetailsWithActions extends CustomComponent
{
	private MessageSource msg;
	private UserAttributesPanel attrsPanel;
	private UserIdentitiesPanel identitiesPanel;
	private Set<String> disabled;
	private Button save;

	public EntityDetailsWithActions(Set<String> disabled,
			UserDetailsPanel detailsPanel, UserIdentitiesPanel identitiesPanel,
			UserAttributesPanel attrsPanel, 
			Button removalButton, MessageSource msg,
			ConnectIdWizardProvider accountAssociationWizardProvider) throws EngineException
	{
		this.disabled = disabled;
		this.identitiesPanel = identitiesPanel;
		this.attrsPanel = attrsPanel;
		this.msg = msg;
		VerticalLayout root = new VerticalLayout();
		root.setMargin(false);
		root.setSpacing(false);
		FormLayout mainForm = new FormLayout();
		CompositeLayoutAdapter layoutAdapter = new CompositeLayoutAdapter(mainForm);
//		if (!disabled.contains(HomeEndpointProperties.Components.userInfo.toString()))
//			layoutAdapter.addContainer(detailsPanel.getContents());
//
//		if (!disabled.contains(HomeEndpointProperties.Components.identitiesManagement.toString()))
//			layoutAdapter.addContainer(identitiesPanel.getContents());

		if (!disabled.contains(HomeEndpointProperties.Components.attributesManagement.toString()))
			layoutAdapter.addContainer(attrsPanel.getContents());
		
		root.addComponent(mainForm);
		root.addComponent(getButtonsBar(disabled, 
				removalButton, accountAssociationWizardProvider));
		setCompositionRoot(root);
	}
	
	private Component getButtonsBar(Set<String> disabled, Button removalButton,
			final ConnectIdWizardProvider accountAssociationWizardProvider)
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth(100, Unit.PERCENTAGE);
		buttons.setMargin(new MarginInfo(false, false, true, false));

		save = new Button(msg.getMessage("save"));
		save.setIcon(Images.save.getResource());
		save.addClickListener(event -> saveChanges());
		buttons.addComponent(save);
		save.setVisible(shouldShowSave());
		buttons.setComponentAlignment(save, Alignment.BOTTOM_LEFT);
		
		Button refresh = new Button(msg.getMessage("refresh"));
		refresh.setIcon(Images.refresh.getResource());
		refresh.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				try
				{
					identitiesPanel.refresh();
					attrsPanel.refresh();
					refresh();
				} catch (EngineException e)
				{
					NotificationPopup.showError(msg, msg.getMessage("error"), e);
				}
			}
		});
		buttons.addComponent(refresh);
		buttons.setComponentAlignment(refresh, Alignment.BOTTOM_LEFT);
		
		Label spacer = new Label();
		buttons.addComponent(spacer);
		buttons.setExpandRatio(spacer, 1);
		
		if (!disabled.contains(HomeEndpointProperties.Components.accountLinking.toString()))
		{
			Button associationButton = new Button(msg.getMessage("EntityDetailsWithActions.associateAccount"));
			associationButton.setId("EntityDetailsWithActions.associateAccount");
			associationButton.addClickListener(new ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					SandboxWizardDialog dialog = new SandboxWizardDialog(
							accountAssociationWizardProvider.getWizardInstance(),
							accountAssociationWizardProvider.getCaption());
					dialog.setHeight(50, Unit.PERCENTAGE);
					dialog.setWidth(60, Unit.PERCENTAGE);
					dialog.show();
				}
			});
			buttons.addComponent(associationButton);
			buttons.setComponentAlignment(associationButton, Alignment.BOTTOM_RIGHT);
		}

		if (!disabled.contains(HomeEndpointProperties.Components.accountRemoval.toString()))
		{
			buttons.addComponent(removalButton);
			buttons.setComponentAlignment(removalButton, Alignment.BOTTOM_RIGHT);
		}
		return buttons;   
	}
	
	public void refresh()
	{
		save.setVisible(shouldShowSave());
	}
	
	private boolean shouldShowSave()
	{
		boolean showSave = false;
//		if (!disabled.contains(HomeEndpointProperties.Components.identitiesManagement.toString()))
//			showSave = identitiesPanel.hasEditable();

		if (!disabled.contains(HomeEndpointProperties.Components.attributesManagement.toString()))
			showSave |= attrsPanel.hasEditable();
		return showSave;
	}
	
	private void saveChanges()
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
		{
			NotificationPopup.showError(msg.getMessage("error"), 
					msg.getMessage("EntityDetailsWithActions.errorSaving"));
			return;
		}
		
		try
		{
			identitiesPanel.saveChanges();
			identitiesPanel.refresh();
			if (attrsPanel.saveChanges())
				attrsPanel.refresh();
			
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, 
				msg.getMessage("EntityDetailsWithActions.errorSaving"), e);
		}
	}
}
