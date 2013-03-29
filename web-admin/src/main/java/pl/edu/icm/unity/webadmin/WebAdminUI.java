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

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.UnityWebUI;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

/**
 * The main entry point of the web administration UI 
 * @author K. Benedyczak
 */
@Component("WebAdminUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WebAdminUI extends UI implements UnityWebUI
{
	private static final long serialVersionUID = 1L;

	@Autowired
	private EndpointManagement test;
	
	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void init(VaadinRequest request)
	{
		try
		{
			List<EndpointTypeDescription> enpT = test.getEndpointTypes();
			setContent(new Label("Web UI. Endpoint types: " + enpT.toString()));
		} catch (EngineException e)
		{
			setContent(new Label("Web UI. Got error: " + e));
		}
	}

}
