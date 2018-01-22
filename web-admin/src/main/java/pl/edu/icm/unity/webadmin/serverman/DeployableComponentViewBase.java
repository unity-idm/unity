/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import org.apache.logging.log4j.Logger;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ExpandCollapseButton;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

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
	protected Status status;
	protected Label separator;

	public DeployableComponentViewBase(UnityServerConfiguration config, ServerManagement serverMan, 
			UnityMessageSource msg)
	{

		this.config = config;
		this.serverMan = serverMan;
		this.msg = msg;
		initUI();

	}

	protected void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		
		header = new HorizontalLayout();
		header.setMargin(false);
		header.setWidth(100, Unit.PERCENTAGE);
		
		main.addComponent(header);
		
		separator = HtmlTag.horizontalLine();
		main.addComponent(separator);

		content = new CompactFormLayout();
		content.setVisible(false);
		content.setSpacing(false);
		main.addComponent(content);
		
		footer = new HorizontalLayout();
		footer.setSpacing(false);
		footer.setMargin(false);
		footer.addComponent(HtmlTag.horizontalLine());
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
		reloadButton.setIcon(Images.reload.getResource());
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
		undeplyButton.setIcon(Images.undeploy.getResource());
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

	public Status getStatus()
	{
		return status;
	}
	
	protected void setStatus(Status status)
	{
		this.status = status;
		if (status.equals(Status.deployed))
		{	
			showHideContentButton.setEnabled(true);
			
		} else if (status.equals(Status.undeployed))
		{
			showHideContentButton.setEnabled(false);
			content.setVisible(false);
						
		}
		
		showHideContentButton.setIcon(content.isVisible() ? Images.upArrow.getResource()
				: Images.downArrow.getResource());
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

		Label statusIcon = new Label();
		statusIcon.setContentMode(ContentMode.HTML);
		if (status.equals(Status.deployed))
		{
			statusIcon.setValue(Images.ok.getHtml());
			statusIcon.setDescription(msg.getMessage("DeployableComponentBase.deployed"));
		} else if (status.equals(Status.undeployed))
		{
			statusIcon.setValue(Images.error.getHtml());
			statusIcon.setDescription(msg.getMessage("DeployableComponentBase.undeployed"));
		}
		HorizontalLayout statusBar = new HorizontalLayout(statusLabel, statusIcon);
		statusBar.setSpacing(true);
		statusBar.setMargin(false);
		header.addComponent(statusBar);
		header.setExpandRatio(statusBar, 2);
		header.setComponentAlignment(statusBar, Alignment.BOTTOM_LEFT);
		
		HorizontalLayout toolbar = new HorizontalLayout(reloadButton, undeplyButton, deployButton);
		toolbar.setSpacing(true);
		toolbar.setMargin(false);
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
			NotificationPopup.showError(msg, msg.getMessage("Configuration.cannotReloadConfig"), e);
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
