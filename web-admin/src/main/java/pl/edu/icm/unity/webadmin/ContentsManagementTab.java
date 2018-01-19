/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webadmin.attribute.AttributesComponent;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupBrowserComponent;
import pl.edu.icm.unity.webadmin.groupdetails.GroupDetailsComponent;
import pl.edu.icm.unity.webadmin.identities.IdentitiesComponent;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;

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
	private AttributesComponent attributesComponent;
	private IdentitiesComponent identitiesTable;
	private GroupDetailsComponent groupDetails;
	
	@Autowired
	public ContentsManagementTab(UnityMessageSource msg, GroupBrowserComponent groupBrowser,
			AttributesComponent attributesComponent, IdentitiesComponent identitiesTable, 
			GroupDetailsComponent groupDetails)
	{
		super();
		this.msg = msg;
		this.groupBrowser = groupBrowser;
		this.attributesComponent = attributesComponent;
		this.identitiesTable = identitiesTable;
		this.groupDetails = groupDetails;
		initUI();
	}

	private void initUI()
	{
		setMargin(false);
		setSpacing(false);
		setCaption(msg.getMessage("ContentsManagementTab.caption"));
		
		CompositeSplitPanel rightPanel = new CompositeSplitPanel(true, false, 
				identitiesTable, attributesComponent, 60);
		CompositeSplitPanel leftPanel = new CompositeSplitPanel(true, false, 
				groupBrowser, groupDetails, 50);

		CompositeSplitPanel main = new CompositeSplitPanel(false, false, leftPanel, rightPanel, 30);
		main.setMargin(new MarginInfo(true, false, false, false));

		addComponent(main);
		setSizeFull();
	}
}
