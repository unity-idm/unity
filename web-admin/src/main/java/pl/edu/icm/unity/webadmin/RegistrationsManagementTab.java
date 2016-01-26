/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.reg.formman.RegistrationFormsComponent;
import pl.edu.icm.unity.webadmin.reg.invitation.InvitationsComponent;
import pl.edu.icm.unity.webadmin.reg.reqman.RequestsComponent;
import pl.edu.icm.unity.webui.registration.RegistrationFormsChooserComponent;

import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.webui.common.Styles;

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
			RequestsComponent requestsComponent, RegistrationFormsChooserComponent reqFillComponent,
			InvitationsComponent invitationsComponent)
	{
		super();
		this.msg = msg;
		reqFillComponent.setShowNonPublic(true);
		reqFillComponent.initUI(TriggeringMode.manualAdmin);
		this.tabs = new MainTabPanel(requestsComponent, reqFillComponent, invitationsComponent, regComponent);
		this.tabs.addStyleName(Styles.vTabsheetMinimal.toString());
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
