/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.bigtab;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.bigtab.BigTab.TabCallback;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Tabs management for {@link BigTabPanel}
 * @author K. Benedyczak
 */
public class BigTabs extends VerticalLayout
{
	private List<BigTab> tabs = new ArrayList<BigTab>();
	private BigTab selectedTab;
	private int width;
	private Unit unit;
	
	
	public BigTabs(int width, Unit unit)
	{
		this.width = width;
		this.unit = unit;
		setWidth(width, unit);
		setSpacing(true);
		Label spacer = new Label();
		addComponent(spacer);
		setExpandRatio(spacer, 1.0f);
		setStyleName(Styles.bigTabs.toString());
	}
	
	public void addTab(String label, String description, Resource image, final TabCallback callback)
	{
		BigTab tab = new BigTab(width, unit, label, description, image, new TabCallback()
		{
			@Override
			public void onSelection(BigTab src)
			{
				if (selectedTab != null)
					selectedTab.deselect();
				callback.onSelection(src);
				selectedTab = src;
			}
		});
		tabs.add(tab);
		addComponent(tab, getComponentCount()-1);
		
		setComponentAlignment(tab, Alignment.TOP_CENTER);
	}
	
	public void select(int tabIndex)
	{
		tabs.get(tabIndex).select();
	}
}
