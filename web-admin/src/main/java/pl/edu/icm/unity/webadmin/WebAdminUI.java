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

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.home.UserAccountComponent;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webadmin.AdminTopHeader.ViewSwitchCallback;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnRouter;

/**
 * The main entry point of the web administration UI.
 * 
 * @author K. Benedyczak
 */
@Component("WebAdminUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class WebAdminUI extends UnityEndpointUIBase implements UnityWebUI
{
	private ContentsManagementTab contentsManagement;
	private RegistrationsManagementTab registrationsManagement;
	private SchemaManagementTab schemaManagement;
	private ServerManagementTab serverManagement;
	private UserAccountComponent userAccount;
	private StandardWebAuthenticationProcessor authnProcessor;
	
	private MainTabPanel tabPanel;
	private HomeEndpointProperties config;
	private VerticalLayout mainWrapper;
	private VerticalLayout contents;
	
	@Autowired
	public WebAdminUI(UnityMessageSource msg, ContentsManagementTab contentsManagement,
			SchemaManagementTab schemaManagement, RegistrationsManagementTab registrationsManagement,
			UserAccountComponent userAccount, ServerManagementTab serverManagement,
			StandardWebAuthenticationProcessor authnProcessor, EnquiresDialogLauncher enquiryDialogLauncher)
	{
		super(msg, enquiryDialogLauncher);
		this.contentsManagement = contentsManagement;
		this.schemaManagement = schemaManagement;
		this.userAccount = userAccount;
		this.serverManagement = serverManagement;
		this.registrationsManagement = registrationsManagement;
		this.authnProcessor = authnProcessor;
	}
	
	@Override
	public void configure(ResolvedEndpoint description,
			List<AuthenticationFlow> authenticationFlows,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties)
	{
		super.configure(description, authenticationFlows, regCfg, endpointProperties);
		this.config = new HomeEndpointProperties(endpointProperties);
	}
	
	@Override
	protected void enter(VaadinRequest request)
	{
		contents = new VerticalLayout();
		contents.setMargin(false);
		contents.setSpacing(false);

		mainWrapper = new VerticalLayout();
		mainWrapper.setSizeFull();
		mainWrapper.setSpacing(false);

		I18nString displayedName = endpointDescription.getEndpoint().getConfiguration().getDisplayedName();
		AdminTopHeader header = new AdminTopHeader(displayedName.getValue(msg), 
				authnProcessor, msg, 
				new ViewSwitchCallback()
				{
					@Override
					public void showView(boolean admin)
					{
						switchView(admin ? tabPanel : userAccount,
								!admin);
					}
				});

		
		createMainTabPanel();
		
		userAccount.initUI(config, sandboxRouter, getSandboxServletURLForAssociation());
		userAccount.setWidth(80, Unit.PERCENTAGE);

		contents.addComponent(header);
		contents.addComponent(mainWrapper);		
		contents.setExpandRatio(mainWrapper, 1.0f);		
		contents.setComponentAlignment(mainWrapper, Alignment.TOP_LEFT);
		contents.setSizeFull();
		
		setContent(contents);
	
		switchView(tabPanel, false);
	}

	private void createMainTabPanel()
	{
		tabPanel = new MainTabPanel(contentsManagement, registrationsManagement, schemaManagement,
				serverManagement);
		tabPanel.addStyleName(Styles.largeTabsheet.toString());
	}
	
	private void switchView(com.vaadin.ui.Component component, boolean setUndefinedHeight)
	{
		mainWrapper.removeAllComponents();
		mainWrapper.addComponent(component);
		mainWrapper.setComponentAlignment(component, Alignment.TOP_CENTER);
		mainWrapper.setExpandRatio(component, 1.0f);
		if (setUndefinedHeight)
		{
			mainWrapper.setHeightUndefined();
			contents.setHeightUndefined();
		} else
		{
			mainWrapper.setHeight(100, Unit.PERCENTAGE);
			contents.setHeight(100, Unit.PERCENTAGE);
		}
	}
	
	@Override
	public void setSandboxRouter(SandboxAuthnRouter sandboxRouter) 
	{
		super.setSandboxRouter(sandboxRouter);
		serverManagement.setSandboxNotifier(sandboxRouter);
	}	
}
