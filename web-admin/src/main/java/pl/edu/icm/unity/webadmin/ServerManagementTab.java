/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.serverman.AuthenticatorsComponent;
import pl.edu.icm.unity.webadmin.serverman.EndpointsComponent;
import pl.edu.icm.unity.webadmin.serverman.ImportExportComponent;
import pl.edu.icm.unity.webadmin.serverman.TranslationProfilesComponent;

import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Tab containing server management views.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ServerManagementTab  extends VerticalLayout
{
	private UnityMessageSource msg;
	private MainTabPanel tabs;

	@Autowired
	public ServerManagementTab(UnityMessageSource msg, ImportExportComponent ieComponent,EndpointsComponent eComponent,AuthenticatorsComponent aComponent,TranslationProfilesComponent tComponent)
	{
		super();
		this.msg = msg;
		this.tabs = new MainTabPanel(eComponent,aComponent,tComponent,ieComponent);
		this.tabs.setStyleName(Reindeer.TABSHEET_MINIMAL);
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("ServerManagementTab.caption"));
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(true);
		wrapper.addComponent(tabs);
		wrapper.setSizeFull();
		addComponent(wrapper);
		setSizeFull();
	}

}
