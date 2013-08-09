/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.home.UserAccountComponent;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webadmin.AdminTopHeader.ViewSwitchCallback;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

/**
 * The main entry point of the web administration UI.
 * 
 * @author K. Benedyczak
 */
@Component("WebAdminUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class WebAdminUI extends UnityUIBase implements UnityWebUI
{
	private static final long serialVersionUID = 1L;

	private ContentsManagementTab contentsManagement;
	private SchemaManagementTab schemaManagement;
	private UserAccountComponent userAccount;
	
	private MainTabPanel tabPanel;
	private EndpointDescription endpointDescription;
	
	@Autowired
	public WebAdminUI(UnityMessageSource msg, ContentsManagementTab contentsManagement,
			SchemaManagementTab schemaManagement,
			UserAccountComponent userAccount)
	{
		super(msg);
		this.contentsManagement = contentsManagement;
		this.schemaManagement = schemaManagement;
		this.userAccount = userAccount;
	}
	
	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		this.endpointDescription = description;
	}
	
	@Override
	protected void appInit(VaadinRequest request)
	{
		VerticalLayout contents = new VerticalLayout();

		final VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.setSizeFull();

		AdminTopHeader header = new AdminTopHeader(endpointDescription.getId(), msg, 
				new ViewSwitchCallback()
				{
					@Override
					public void showView(boolean admin)
					{
						switchView(mainWrapper, admin ? tabPanel : userAccount);
					}
				});

		
		createMainTabPanel();
		userAccount.setWidth(80, Unit.PERCENTAGE);

		contents.addComponent(header);
		contents.addComponent(mainWrapper);		
		contents.setExpandRatio(mainWrapper, 1.0f);		
		contents.setComponentAlignment(mainWrapper, Alignment.TOP_LEFT);
		contents.setSizeFull();
		
		setContent(contents);
	
		switchView(mainWrapper, tabPanel);
	}

	private void createMainTabPanel()
	{
		tabPanel = new MainTabPanel();		
		tabPanel.addTab(contentsManagement);
		tabPanel.addTab(schemaManagement);
		tabPanel.setSizeFull();
	}
	
	private void switchView(VerticalLayout contents, com.vaadin.ui.Component component)
	{
		contents.removeAllComponents();
		contents.addComponent(component);
		contents.setComponentAlignment(component, Alignment.TOP_CENTER);
		contents.setExpandRatio(component, 1.0f);		
	}
	
}
