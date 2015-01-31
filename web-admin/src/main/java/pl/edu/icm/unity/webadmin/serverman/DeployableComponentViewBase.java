/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;

/**
 * Represent base view of server deployable component.
 * Contains header, collapsible content and buttons to collapse/expand and deploy/undeploy/reload
 * 
 * @author P. Piernik
 *
 */
public abstract class DeployableComponentViewBase extends CustomComponent
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, DeployableComponentViewBase.class);
	public static enum Status {deployed, undeployed};
	
	protected UnityServerConfiguration config;
	protected ServerManagement serverMan;
	protected UnityMessageSource msg;
	protected HorizontalLayout header;
	protected FormLayout content;
	protected HorizontalLayout footer;
	protected ExpandCollapseButton showHideContentButton;
	protected Button undeplyButton;
	protected Button reloadButton;
	protected Button deployButton;
	protected String status;
	protected Label separator;

	public DeployableComponentViewBase(UnityServerConfiguration config,
			ServerManagement serverMan, UnityMessageSource msg, String status)
	{

		this.config = config;
		this.serverMan = serverMan;
		this.msg = msg;
		initUI();

	}

	protected void initUI()
	{
		VerticalLayout main = new VerticalLayout();

		header = new HorizontalLayout();
		header.setSpacing(true);
		header.setWidth(100, Unit.PERCENTAGE);
		
		main.addComponent(header);
		
		separator = new Label();
		separator.addStyleName(Styles.horizontalLine.toString());
		main.addComponent(separator);

		content = new FormLayout();
		content.setVisible(false);
		content.setSpacing(false);
		main.addComponent(content);
		
		footer = new HorizontalLayout();
		Label line = new Label();
		line.addStyleName(Styles.horizontalLine.toString());
		footer.setSpacing(false);
		footer.setMargin(false);
		footer.addComponent(line);
		footer.setSizeFull();
		main.addComponent(footer);
		
		setCompositionRoot(main);
		showHideContentButton = new ExpandCollapseButton(true, content, footer);
		showHideContentButton.setCustomListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				separator.setVisible(!content.isVisible());
			}
		});

		reloadButton = new Button();
		reloadButton.setIcon(Images.transfer.getResource());
		reloadButton.addStyleName(Styles.vButtonLink.toString());
		reloadButton.addStyleName(Styles.toolbarButton.toString());
		reloadButton.setDescription(msg.getMessage("DeployableComponentBase.reload"));
		reloadButton.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{	
				reload(true);	
			}
		});

		undeplyButton = new Button();
		undeplyButton.setIcon(Images.delete.getResource());
		undeplyButton.addStyleName(Styles.vButtonLink.toString());
		undeplyButton.addStyleName(Styles.toolbarButton.toString());
		undeplyButton.setDescription(msg.getMessage("DeployableComponentBase.undeploy"));
		undeplyButton.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				new ConfirmDialog(msg, msg.getMessage("DeployableComponentBase.unDeployQuestion"),
						new ConfirmDialog.Callback()
						{
							@Override
							public void onConfirm()
							{
								undeploy();
							}
						}).show();
			}
		});

		deployButton = new Button();
		deployButton.setIcon(Images.add.getResource());
		deployButton.addStyleName(Styles.vButtonLink.toString());
		deployButton.addStyleName(Styles.toolbarButton.toString());
		deployButton.setDescription(msg.getMessage("DeployableComponentBase.deploy"));
		deployButton.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				deploy();
			}
		});

	}
	
	protected abstract void updateContent();

	protected abstract void updateHeader();

	public abstract void deploy();
	
	public abstract void undeploy();
	
	public abstract void reload(boolean showSuccess);

	public String getStatus()
	{
		return status;
	}
	
	protected void setStatus(String status)
	{
		this.status = status;
		if (status.equals(Status.deployed.toString()))
		{	
			showHideContentButton.setEnabled(true);
			
		} else if (status.equals(Status.undeployed.toString()))
		{
			showHideContentButton.setEnabled(false);
			content.setVisible(false);
						
		}
		
		showHideContentButton.setIcon(content.isVisible() ? Images.zoomout.getResource()
				: Images.zoomin.getResource());
		updateContent();
		updateHeader();
		footer.setVisible(content.isVisible());
		separator.setVisible(!content.isVisible());
		deployButton.setVisible(!showHideContentButton.isEnabled());
		undeplyButton.setVisible(!deployButton.isVisible());
		reloadButton.setVisible(!deployButton.isVisible());
	}

	protected void updateHeader(String name)
	{
		header.removeAllComponents();

		header.addComponent(showHideContentButton);
		header.setComponentAlignment(showHideContentButton, Alignment.BOTTOM_LEFT);

		Label val = new Label(name);
		header.addComponent(val);
		header.setExpandRatio(val, 1);
		header.setComponentAlignment(val, Alignment.BOTTOM_LEFT);
		
		Label statusLabel = new Label(msg.getMessage("DeployableComponentBase.status"));
		statusLabel.addStyleName(Styles.bold.toString());

		Image statusIcon = new Image();
		if (status.equals(Status.deployed.toString()))
		{
			statusIcon.setSource(Images.ok.getResource());
			statusIcon.setDescription(msg.getMessage("DeployableComponentBase.deployed"));
		} else if (status.equals(Status.undeployed.toString()))
		{
			statusIcon.setSource(Images.error.getResource());
			statusIcon.setDescription(msg.getMessage("DeployableComponentBase.undeployed"));
		}
		HorizontalLayout statusBar = new HorizontalLayout(statusLabel, statusIcon);
		statusBar.setSpacing(true);
		header.addComponent(statusBar);
		header.setExpandRatio(statusBar, 2);
		header.setComponentAlignment(statusBar, Alignment.BOTTOM_LEFT);
		
		HorizontalLayout toolbar = new HorizontalLayout(reloadButton, undeplyButton, deployButton);
		toolbar.setSpacing(true);
		header.addComponent(toolbar);
		header.setExpandRatio(toolbar, 1);
		header.setComponentAlignment(toolbar, Alignment.BOTTOM_RIGHT);
	}	

	protected boolean reloadConfig()
	{
		try
		{
			serverMan.reloadConfig();
		} catch (Exception e)
		{
			log.error("Cannot reload configuration", e);
			ErrorPopup.showError(msg, msg.getMessage("Configuration.cannotReloadConfig"), e);
			return false;
		}
		return true;
	}

	protected void addCustomFieldToContent(Component component)
	{
		component.addStyleName(Styles.captionBold.toString());
		content.addComponent(component);
	}
	
	protected void addFieldToContent(String name, String value)
	{
		addField(content, name, value);
	}

	protected void addField(Layout parent, String name, String value)
	{
		Label val = new Label(value);
		val.setCaption(name + ":");
		val.addStyleName(Styles.captionBold.toString());
		parent.addComponents(val);

	}
}
