/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.attribute.AttributesPanel;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupBrowserComponent;
import pl.edu.icm.unity.webadmin.identities.IdentitiesComponent;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * The tab with groups, identities and attribtues management.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ContentsManagementTab extends VerticalLayout
{
	private UnityMessageSource msg;
	private GroupBrowserComponent groupBrowser;
	private AttributesPanel attributesPanel;
	private IdentitiesComponent identitiesTable;
	
	@Autowired
	public ContentsManagementTab(UnityMessageSource msg, GroupBrowserComponent groupBrowser,
			AttributesPanel attributesPanel, IdentitiesComponent identitiesTable)
	{
		super();
		this.msg = msg;
		this.groupBrowser = groupBrowser;
		this.attributesPanel = attributesPanel;
		this.identitiesTable = identitiesTable;
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("ContentsManagementTab.caption"));
		
		CompositeSplitPanel rightPanel = new CompositeSplitPanel(true, false, 
				identitiesTable, attributesPanel, 60);
		CompositeSplitPanel leftPanel = new CompositeSplitPanel(true, false, 
				groupBrowser, new Label("TODO"), 60);

		CompositeSplitPanel main = new CompositeSplitPanel(false, true, leftPanel, rightPanel, 30);

		addComponent(main);
		setSizeFull();
	}
}
