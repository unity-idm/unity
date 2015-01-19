/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.home.UserAccountComponent;
import pl.edu.icm.unity.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webadmin.AdminTopHeader.ViewSwitchCallback;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.AuthenticationProcessor;

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
	private RegistrationsManagementTab registrationsManagement;
	private SchemaManagementTab schemaManagement;
	private ServerManagementTab serverManagement;
	private UserAccountComponent userAccount;
	private AuthenticationProcessor authnProcessor;
	
	private MainTabPanel tabPanel;
	private EndpointDescription endpointDescription;
	private HomeEndpointProperties config;
	
	@Autowired
	public WebAdminUI(UnityMessageSource msg, ContentsManagementTab contentsManagement,
			SchemaManagementTab schemaManagement, RegistrationsManagementTab registrationsManagement,
			UserAccountComponent userAccount, ServerManagementTab serverManagement,
			AuthenticationProcessor authnProcessor)
	{
		super(msg);
		this.contentsManagement = contentsManagement;
		this.schemaManagement = schemaManagement;
		this.userAccount = userAccount;
		this.serverManagement = serverManagement;
		this.registrationsManagement = registrationsManagement;
		this.authnProcessor = authnProcessor;
	}
	
	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties)
	{
		this.endpointDescription = description;
		this.config = new HomeEndpointProperties(endpointProperties);
	}
	
	@Override
	protected void appInit(VaadinRequest request)
	{
		VerticalLayout contents = new VerticalLayout();

		final VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.setSizeFull();

		AdminTopHeader header = new AdminTopHeader(endpointDescription.getId(), authnProcessor, msg, 
				new ViewSwitchCallback()
				{
					@Override
					public void showView(boolean admin)
					{
						switchView(mainWrapper, admin ? tabPanel : userAccount);
					}
				});

		
		createMainTabPanel();
		
		userAccount.initUI(config);
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
		tabPanel.addTab(registrationsManagement);
		tabPanel.addTab(schemaManagement);
		tabPanel.addTab(serverManagement);
		tabPanel.setSizeFull();
	}
	
	private void switchView(VerticalLayout contents, com.vaadin.ui.Component component)
	{
		contents.removeAllComponents();
		contents.addComponent(component);
		contents.setComponentAlignment(component, Alignment.TOP_CENTER);
		contents.setExpandRatio(component, 1.0f);		
	}
	
	@Override
	public void setSandboxRouter(SandboxAuthnRouter sandboxRouter) 
	{
		serverManagement.setSandboxNotifier(sandboxRouter);
	}	
}
