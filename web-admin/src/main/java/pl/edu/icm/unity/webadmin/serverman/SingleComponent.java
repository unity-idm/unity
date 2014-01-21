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

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

public class SingleComponent extends CustomComponent
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SingleComponent.class);

	public static final String STATUS_DEPLOYED = "deployed";
	public static final String STATUS_UNDEPLOYED = "undeployed";
	protected UnityServerConfiguration config;
	protected UnityMessageSource msg;
	protected GridLayout header;
	protected VerticalLayout content;
	protected Button showHideContentButton;
	protected Button undeplyButton;
	protected Button reloadButton;
	protected Button deployButton;
	protected String status;
	protected String msgPrefix;

	public SingleComponent(UnityServerConfiguration config, UnityMessageSource msg,
			String status, String msgPrefix)
	{

		this.config = config;
		this.msg = msg;
		this.msgPrefix = msgPrefix;
		initUI();

	}

	protected void initUI()
	{
		VerticalLayout main = new VerticalLayout();

		header = new GridLayout(10, 1);
		header.setSpacing(true);
		header.setColumnExpandRatio(2, 0);

		Label line = new Label();
		line.addStyleName(Styles.horizontalLine.toString());
		main.addComponent(line);

		main.addComponent(header);

		content = new VerticalLayout();
		content.setVisible(false);
		main.addComponent(content);
		line = new Label();
		line.addStyleName(Styles.horizontalLine.toString());
		main.addComponent(line);

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
					content.setVisible(false);
				} else
				{
					showHideContentButton.setIcon(Images.zoomout.getResource());
					content.setVisible(true);
				}

			}
		});

		reloadButton = new Button();
		reloadButton.setIcon(Images.transfer.getResource());
		reloadButton.addStyleName(Reindeer.BUTTON_LINK);
		reloadButton.setDescription(msg.getMessage(msgPrefix + "." + "reload"));
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
		undeplyButton.setDescription(msg.getMessage(msgPrefix + "." + "undeploy"));

		undeplyButton.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{
				new ConfirmDialog(msg, msg.getMessage(msgPrefix + "."
						+ "unDeployQuestion"), new ConfirmDialog.Callback()

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
		deployButton.setDescription(msg.getMessage(msgPrefix + "." + "deploy"));

		deployButton.addClickListener(new ClickListener()
		{

			@Override
			public void buttonClick(ClickEvent event)
			{

				deploy();

			}
		});

	}

	protected void setStatus(String status)
	{
		if (status.equals(STATUS_DEPLOYED))
		{
			this.status = status;
			updateHeader();
			updateContent();
			showHideContentButton.setEnabled(true);
			showHideContentButton.setIcon(Images.zoomin.getResource());
			content.setVisible(false);
			undeplyButton.setVisible(true);
			reloadButton.setVisible(true);
			deployButton.setVisible(false);

		} else if (status.equals(STATUS_UNDEPLOYED))
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

	}

	protected void updateHeader(String name)
	{
		header.removeAllComponents();

		header.addComponent(showHideContentButton);
		header.setComponentAlignment(showHideContentButton, Alignment.BOTTOM_LEFT);

		// Name
		HorizontalLayout nameFieldLayout = new HorizontalLayout();
		addFieldWithLabel(nameFieldLayout, msg.getMessage(msgPrefix + "." + "name"), name,
				0);
		nameFieldLayout.setMargin(false);
		nameFieldLayout.setWidth(500, Unit.PIXELS);
		header.addComponent(nameFieldLayout);
		header.setComponentAlignment(nameFieldLayout, Alignment.BOTTOM_CENTER);

		Label statusLabel = new Label(msg.getMessage(msgPrefix + "." + "status") + ":");
		statusLabel.addStyleName(Styles.bold.toString());
		header.addComponent(statusLabel);
		header.setComponentAlignment(statusLabel, Alignment.BOTTOM_CENTER);

		Image statusImage = new Image();
		if (status.equals(STATUS_DEPLOYED))
		{
			statusImage.setSource(Images.ok.getResource());
			statusImage.setDescription(msg.getMessage(msgPrefix + "." + "deployed"));
		} else if (status.equals(STATUS_UNDEPLOYED))
		{

			statusImage.setSource(Images.error.getResource());
			statusImage.setDescription(msg.getMessage(msgPrefix + "." + "undeployed"));
		}
		header.addComponent(statusImage);

		Label spacer = new Label();
		spacer.setWidth(10, Unit.PIXELS);
		header.addComponent(spacer);

		header.addComponent(reloadButton);
		header.setComponentAlignment(reloadButton, Alignment.BOTTOM_LEFT);

		header.addComponent(undeplyButton);
		header.setComponentAlignment(undeplyButton, Alignment.BOTTOM_LEFT);

		header.addComponent(deployButton);
		header.setComponentAlignment(deployButton, Alignment.BOTTOM_LEFT);

	}

	protected void updateContent()
	{

	}

	protected void updateHeader()
	{

	}

	protected boolean deploy()
	{
		return reloadConfig();
	}
	
	protected boolean undeploy()
	{
		return reloadConfig();
	}

	protected boolean reload()
	{
		return reloadConfig();
	}

	protected boolean reloadConfig()
	{
		try
		{
			config.reloadIfChanged();
		} catch (Exception e)
		{
			log.error("Cannot reload configuration", e);
			ErrorPopup.showError(msg,
					msg.getMessage(msgPrefix + ".cannotReloadConfig"), e);
			return false;
		}
		return true;
	}

	protected HorizontalLayout addFieldWithLabel(Layout parent, String name, String value,
			int space)
	{
		if (space != 0)
		{
			Label spacer = new Label();
			spacer.setWidth(space, Unit.PIXELS);
		}
		Label namel = new Label(name + ":");
		namel.addStyleName(Styles.bold.toString());
		Label nameVal = new Label(value);
		HorizontalLayout fieldLayout = new HorizontalLayout();
		fieldLayout.setSpacing(true);

		if (space != 0)
		{
			Label spacer = new Label();
			spacer.setWidth(space, Unit.PIXELS);
			fieldLayout.addComponents(spacer, namel, nameVal);

		} else
		{
			fieldLayout.addComponents(namel, nameVal);

		}

		parent.addComponent(fieldLayout);
		return fieldLayout;

	}
}
