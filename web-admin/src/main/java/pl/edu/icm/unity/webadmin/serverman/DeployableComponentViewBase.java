/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Represent base view of server deployable component.
 * Contains header,collapsible content and buttons to callapse/expand and deploy/undeploy/reload
 * 
 * @author P. Piernik
 *
 */
public abstract class DeployableComponentViewBase extends CustomComponent
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, DeployableComponentViewBase.class);
	public static enum Status {deployed, undeployed};
	
	protected UnityServerConfiguration config;
	protected UnityMessageSource msg;
	protected GridLayout header;
	protected FormLayout content;
	protected HorizontalLayout footer;
	protected Button showHideContentButton;
	protected Button undeplyButton;
	protected Button reloadButton;
	protected Button deployButton;
	protected String status;
	protected Label separator;

	public DeployableComponentViewBase(UnityServerConfiguration config, UnityMessageSource msg,
			String status)
	{

		this.config = config;
		this.msg = msg;
		initUI();

	}

	protected void initUI()
	{
		VerticalLayout main = new VerticalLayout();

		header = new GridLayout(10, 1);
		header.setSpacing(true);
		header.setColumnExpandRatio(2, 0);
		
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
		showHideContentButton = new Button();
		showHideContentButton.setIcon(Images.zoomin.getResource());
		showHideContentButton.addStyleName(Reindeer.BUTTON_LINK);
		showHideContentButton.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				if (content.isVisible())
				{
					showHideContentButton.setIcon(Images.zoomin.getResource());
					separator.setVisible(true);
					content.setVisible(false);
					footer.setVisible(false);
				} else
				{
					showHideContentButton.setIcon(Images.zoomout.getResource());
					separator.setVisible(false);
					content.setVisible(true);
					footer.setVisible(true);
				}

			}
		});

		reloadButton = new Button();
		reloadButton.setIcon(Images.transfer.getResource());
		reloadButton.addStyleName(Reindeer.BUTTON_LINK);
		reloadButton.addStyleName(Styles.toolbarButton.toString());
		reloadButton.setDescription(msg.getMessage("DeployableComponentBase.reload"));
		reloadButton.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				reload();

			}
		});

		undeplyButton = new Button();
		undeplyButton.setIcon(Images.delete.getResource());
		undeplyButton.addStyleName(Reindeer.BUTTON_LINK);
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
		deployButton.addStyleName(Reindeer.BUTTON_LINK);
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

	protected abstract void deploy();
	
	protected abstract void undeploy();
	
	protected abstract void reload();

	protected void setStatus(String status)
	{
		if (status.equals(Status.deployed.toString()))
		{
			this.status = status;
			updateHeader();
			updateContent();
			showHideContentButton.setEnabled(true);
			if (content.isVisible())
			{
				showHideContentButton.setIcon(Images.zoomout.getResource());

			} else
			{
				showHideContentButton.setIcon(Images.zoomin.getResource());

			}

			undeplyButton.setVisible(true);
			reloadButton.setVisible(true);
			deployButton.setVisible(false);

		} else if (status.equals(Status.undeployed.toString()))
		{
			this.status = status;
			updateHeader();
			showHideContentButton.setEnabled(false);
			showHideContentButton.setIcon(Images.zoomin.getResource());
			content.removeAllComponents();
			content.setVisible(false);
			undeplyButton.setVisible(false);
			reloadButton.setVisible(false);
			deployButton.setVisible(true);

		}
		footer.setVisible(content.isVisible());
		separator.setVisible(!content.isVisible());

	}

	protected void updateHeader(String name)
	{
		header.removeAllComponents();

		header.addComponent(showHideContentButton);
		header.setComponentAlignment(showHideContentButton, Alignment.BOTTOM_LEFT);

		HorizontalLayout nameFieldLayout = new HorizontalLayout();
		HorizontalLayout h=new HorizontalLayout();
		h.setSpacing(true);
		h.setMargin(false);
		Label val=new Label(name);
		h.addComponents(val);
		nameFieldLayout.setMargin(false);
		nameFieldLayout.setWidth(500, Unit.PIXELS);	
		nameFieldLayout.addComponents(h);
		header.addComponent(nameFieldLayout);
		header.setComponentAlignment(nameFieldLayout, Alignment.BOTTOM_CENTER);

		Label statusLabel = new Label(msg.getMessage("DeployableComponentBase.status") + ":");
		statusLabel.addStyleName(Styles.bold.toString());
		header.addComponent(statusLabel);
		header.setComponentAlignment(statusLabel, Alignment.BOTTOM_CENTER);

		Image statusImage = new Image();
		if (status.equals(Status.deployed.toString()))
		{
			statusImage.setSource(Images.ok.getResource());
			statusImage.setDescription(msg.getMessage("DeployableComponentBase.deployed"));
		} else if (status.equals(Status.undeployed.toString()))
		{

			statusImage.setSource(Images.error.getResource());
			statusImage.setDescription(msg.getMessage("DeployableComponentBase.undeployed"));
		}
		header.addComponent(statusImage);

		Label spacer = new Label();
		spacer.setWidth(30, Unit.PIXELS);
		header.addComponent(spacer);

		header.addComponent(reloadButton);
		header.setComponentAlignment(reloadButton, Alignment.BOTTOM_LEFT);

		header.addComponent(undeplyButton);
		header.setComponentAlignment(undeplyButton, Alignment.BOTTOM_LEFT);

		header.addComponent(deployButton);
		header.setComponentAlignment(deployButton, Alignment.BOTTOM_LEFT);

	}	

	protected boolean reloadConfig()
	{
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{
			log.error("Cannot reload configuration", e);
			ErrorPopup.showError(msg, msg.getMessage("Configuration.cannotReloadConfig"), e);
			return false;
		}
		return true;
	}

	protected void addFieldToContent(String name, String value)
	{
		addField(content, name, value);
	}

	protected void addField(Layout parent, String name, String value)
	{
		Label val = new Label(value, ContentMode.HTML);
		val.setCaption(name + ":");
		val.addStyleName(Styles.captionBold.toString());
		parent.addComponents(val);

	}
}
