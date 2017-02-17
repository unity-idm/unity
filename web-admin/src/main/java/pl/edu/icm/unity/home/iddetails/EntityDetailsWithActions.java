/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.iddetails;

import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.sandbox.wizard.SandboxWizardDialog;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.association.afterlogin.ConnectIdWizardProvider;
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
import com.vaadin.ui.Label;
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
	private Set<String> disabled;
	private Button save;

	public EntityDetailsWithActions(Set<String> disabled,
			UserDetailsPanel detailsPanel, UserIdentitiesPanel identitiesPanel,
			UserAttributesPanel attrsPanel, 
			Button removalButton, UnityMessageSource msg,
			ConnectIdWizardProvider accountAssociationWizardProvider) throws EngineException
	{
		this.disabled = disabled;
		this.identitiesPanel = identitiesPanel;
		this.attrsPanel = attrsPanel;
		this.msg = msg;
		VerticalLayout root = new VerticalLayout();
		FormLayout mainForm = new FormLayout();
		if (!disabled.contains(HomeEndpointProperties.Components.userInfo.toString()))
			detailsPanel.addIntoLayout(mainForm);
		
		if (!disabled.contains(HomeEndpointProperties.Components.identitiesManagement.toString()))
			identitiesPanel.addIntoLayout(mainForm);

		if (!disabled.contains(HomeEndpointProperties.Components.attributesManagement.toString()))
			attrsPanel.addIntoLayout(mainForm);
		
		root.addComponent(mainForm);
		root.addComponent(getButtonsBar(disabled, 
				removalButton, accountAssociationWizardProvider));
		setCompositionRoot(root);
	}
	
	private Component getButtonsBar(Set<String> disabled, Button removalButton,
			final ConnectIdWizardProvider accountAssociationWizardProvider)
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setSpacing(true);
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
		if (!disabled.contains(HomeEndpointProperties.Components.identitiesManagement.toString()))
			showSave = identitiesPanel.hasEditable();

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
			NotificationPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("EntityDetailsWithActions.errorSaving"));
			return;
		}
		
		try
		{
			identitiesPanel.saveChanges();
			attrsPanel.saveChanges();
			identitiesPanel.refresh();
			attrsPanel.refresh();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, 
				msg.getMessage("EntityDetailsWithActions.errorSaving"), e);
		}
	}
}
