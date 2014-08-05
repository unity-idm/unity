/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;

/**
 * UI of the authorization endpoint, responsible for getting consent after resource owner login.
 * @author K. Benedyczak
 */
@Component("OAuthAuthzUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class OAuthAuthzUI extends UnityUIBase 
{
	@Autowired
	public OAuthAuthzUI(UnityMessageSource msg)
	{
		super(msg);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration registrationConfiguration)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		// TODO Auto-generated method stub
		
	}

}
