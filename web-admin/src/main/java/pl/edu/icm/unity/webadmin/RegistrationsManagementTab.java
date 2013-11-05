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
import pl.edu.icm.unity.webadmin.reg.formman.RegistrationFormsComponent;
import pl.edu.icm.unity.webadmin.reg.reqman.RequestsComponent;
import pl.edu.icm.unity.webui.registration.RegistrationFormsChooserComponent;

import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Tab containing management views for the management of registrations: forms, requests and form filling.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RegistrationsManagementTab  extends VerticalLayout
{
	private UnityMessageSource msg;
	private MainTabPanel tabs;

	@Autowired
	public RegistrationsManagementTab(UnityMessageSource msg, RegistrationFormsComponent regComponent,
			RequestsComponent requestsComponent, RegistrationFormsChooserComponent reqFillComponent)
	{
		super();
		this.msg = msg;
		reqFillComponent.setShowNonPublic(true);
		reqFillComponent.setAddAutoAccept(true);
		reqFillComponent.initUI();
		this.tabs = new MainTabPanel(requestsComponent, reqFillComponent, regComponent);
		this.tabs.setStyleName(Reindeer.TABSHEET_MINIMAL);
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("RegistrationsManagementTab.caption"));
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(true);
		wrapper.addComponent(tabs);
		wrapper.setSizeFull();
		addComponent(wrapper);
		setSizeFull();
	}
}
