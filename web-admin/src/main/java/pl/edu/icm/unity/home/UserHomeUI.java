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

import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.TopHeader;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

/**
 * The main entry point of the web administration UI.
 * 
 * @author K. Benedyczak
 */
@Component("UserHomeUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class UserHomeUI extends UnityUIBase implements UnityWebUI
{
	private static final long serialVersionUID = 1L;

	private UserAccountComponent userAccount;
	private EndpointDescription endpointDescription;
	private WebAuthenticationProcessor authnProcessor;
	private HomeEndpointProperties config;

	@Autowired
	public UserHomeUI(UnityMessageSource msg, UserAccountComponent userAccountComponent,
			WebAuthenticationProcessor authnProcessor)
	{
		super(msg);
		this.userAccount = userAccountComponent;
		this.authnProcessor = authnProcessor;
	}

	@Override
	public void configure(EndpointDescription description,
			List<AuthenticationOption> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties)
	{
		this.endpointDescription = description;
		this.config = new HomeEndpointProperties(endpointProperties);
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		VerticalLayout contents = new VerticalLayout();
		TopHeader header = new TopHeader(endpointDescription.getDisplayedName().getValue(msg), 
				authnProcessor, msg);
		contents.addComponent(header);

		userAccount.initUI(config);
		
		userAccount.setWidth(80, Unit.PERCENTAGE);
		contents.addComponent(userAccount);
		contents.setComponentAlignment(userAccount, Alignment.TOP_CENTER);
		contents.setExpandRatio(userAccount, 1.0f);

		
		contents.setSizeFull();
		setContent(contents);
	}
}


