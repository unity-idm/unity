/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.home.UserAccountComponent;
import pl.edu.icm.unity.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webadmin.AdminTopHeader.ViewSwitchCallback;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Styles;

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
@Theme("unityThemeValo")
public class WebAdminUI extends UnityUIBase implements UnityWebUI
{
	private static final long serialVersionUID = 1L;

	private ContentsManagementTab contentsManagement;
	private RegistrationsManagementTab registrationsManagement;
	private SchemaManagementTab schemaManagement;
	private ServerManagementTab serverManagement;
	private UserAccountComponent userAccount;
	private WebAuthenticationProcessor authnProcessor;
	
	private MainTabPanel tabPanel;
	private HomeEndpointProperties config;
	
	@Autowired
	public WebAdminUI(UnityMessageSource msg, ContentsManagementTab contentsManagement,
			SchemaManagementTab schemaManagement, RegistrationsManagementTab registrationsManagement,
			UserAccountComponent userAccount, ServerManagementTab serverManagement,
			WebAuthenticationProcessor authnProcessor)
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
			List<AuthenticationOption> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties)
	{
		super.configure(description, authenticators, regCfg, endpointProperties);
		this.config = new HomeEndpointProperties(endpointProperties);
	}
	
	@Override
	protected void appInit(VaadinRequest request)
	{
		VerticalLayout contents = new VerticalLayout();

		final VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.setSizeFull();
		mainWrapper.setMargin(true);

		AdminTopHeader header = new AdminTopHeader(endpointDescription.getDisplayedName().getValue(msg), 
				authnProcessor, msg, 
				new ViewSwitchCallback()
				{
					@Override
					public void showView(boolean admin)
					{
						switchView(mainWrapper, admin ? tabPanel : userAccount);
					}
				});

		
		createMainTabPanel();
		
		userAccount.initUI(config, sandboxRouter, getSandboxServletURL());
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
		tabPanel = new MainTabPanel(contentsManagement, registrationsManagement, schemaManagement,
				serverManagement);
		tabPanel.addStyleName(Styles.largeTabsheet.toString());
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
