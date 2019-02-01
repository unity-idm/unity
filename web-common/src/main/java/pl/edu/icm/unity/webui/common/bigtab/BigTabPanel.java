/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.bigtab;

import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.bigtab.BigTab.TabCallback;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;


/**
 * Component providing a modern tab pane: tabs are vertical on the left side. Each is a large
 * rectangular area, with a picture and label. When a tab is selected it is highlighted.
 * @author K. Benedyczak
 */
public class BigTabPanel extends HorizontalLayout
{
	private UnityMessageSource msg;
	private BigTabs tabs;
	private VerticalLayout main;
	private SafePanel mainPanel;

	public BigTabPanel(int tabsBarWidth, Unit widthUnit, UnityMessageSource msg)
	{
		this.msg = msg;
		tabs = new BigTabs(tabsBarWidth, widthUnit);
		mainPanel = new SafePanel();
		main = new VerticalLayout();
		main.setSizeFull();
		main.setMargin(true);
		main.setSpacing(false);
		mainPanel.setContent(main);
		Label spacer = new Label();
		spacer.setWidth(15, Unit.PIXELS);
		addComponents(tabs, spacer, mainPanel);
		setExpandRatio(mainPanel, 1.0f);
		setSpacing(false);
		setMargin(false);
	}
	
	public int getTabsCount()
	{
		return tabs.getTabsCount();
	}
	
	public void addTab(String labelKey, String descriptionKey, Images image, final Component contents)
	{
		addTab(labelKey, descriptionKey, image, contents, null);
	}
	
	public void addTab(String labelKey, String descriptionKey, Images image, final Component contents, final TabCallback callback)
	{
		String label = labelKey == null ? null : msg.getMessage(labelKey);
		String description = descriptionKey == null ? null : msg.getMessage(descriptionKey);
		tabs.addTab(label, description, image, new TabCallback()
		{
			@Override
			public void onSelection(BigTab src)
			{
				main.removeAllComponents();
				main.addComponent(contents);
				String caption = src.getCaption() == null ? "" : src.getCaption();
				mainPanel.setCaption(caption);
				if (callback != null)
					callback.onSelection(src);
			}
		});
	}
	
	public void select(int tabIndex)
	{
		tabs.select(tabIndex);
	}
}
