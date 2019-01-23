/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home;

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
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webadmin.utils.ProjectManagementHelper;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.TopHeader;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;

/**
 * The main entry point of the web administration UI.
 * 
 * @author K. Benedyczak
 */
@Component("UserHomeUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class UserHomeUI extends UnityEndpointUIBase implements UnityWebUI
{
	private UserAccountComponent userAccount;
	private StandardWebAuthenticationProcessor authnProcessor;
	private HomeEndpointProperties config;
	private ProjectManagementHelper projectManHelper;

	@Autowired
	public UserHomeUI(UnityMessageSource msg, UserAccountComponent userAccountComponent,
			StandardWebAuthenticationProcessor authnProcessor, EnquiresDialogLauncher enquiryDialogLauncher, ProjectManagementHelper projectManHelper)
	{
		super(msg, enquiryDialogLauncher);
		this.userAccount = userAccountComponent;
		this.authnProcessor = authnProcessor;
		this.projectManHelper = projectManHelper;
	}

	@Override
	public void configure(ResolvedEndpoint description, List<AuthenticationFlow> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties)
	{
		super.configure(description, authenticators, regCfg, endpointProperties);
		this.config = new HomeEndpointProperties(endpointProperties);
	}

	@Override
	protected void enter(VaadinRequest request)
	{
		VerticalLayout contents = new VerticalLayout();
		contents.setMargin(false);
		contents.setSpacing(false);
		I18nString displayedName = endpointDescription.getEndpoint().getConfiguration().getDisplayedName();
		TopHeader header = new HomeTopHeader(displayedName.getValue(msg), authnProcessor, msg,
				projectManHelper.getProjectManLinkIfAvailable(config));
		contents.addComponent(header);

		userAccount.initUI(config, sandboxRouter, getSandboxServletURLForAssociation());

		userAccount.setWidth(80, Unit.PERCENTAGE);
		contents.addComponent(userAccount);
		contents.setComponentAlignment(userAccount, Alignment.TOP_CENTER);
		contents.setExpandRatio(userAccount, 1.0f);

		setSizeFull();
		setContent(contents);
	}
}
