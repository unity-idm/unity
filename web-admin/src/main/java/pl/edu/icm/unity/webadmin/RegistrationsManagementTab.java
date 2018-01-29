/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webadmin.reg.formfill.FormsChooserComponent;
import pl.edu.icm.unity.webadmin.reg.formman.EnquiryFormsComponent;
import pl.edu.icm.unity.webadmin.reg.formman.RegistrationFormsComponent;
import pl.edu.icm.unity.webadmin.reg.invitation.InvitationsComponent;
import pl.edu.icm.unity.webadmin.reg.reqman.RequestsComponent;
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
			RequestsComponent requestsComponent, FormsChooserComponent reqFillComponent,
			InvitationsComponent invitationsComponent, EnquiryFormsComponent enquiryFormsComponent)
	{
		super();
		this.msg = msg;
		this.tabs = new MainTabPanel(requestsComponent, reqFillComponent, invitationsComponent, 
				regComponent, enquiryFormsComponent);
		this.tabs.addStyleName(Styles.vTabsheetMinimal.toString());
		initUI();
	}

	private void initUI()
	{
		setMargin(false);
		setSpacing(false);
		setCaption(msg.getMessage("RegistrationsManagementTab.caption"));
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.addComponent(tabs);
		wrapper.setSizeFull();
		addComponent(wrapper);
		setSizeFull();
	}
}
