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

import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component.Event;

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
			VerticalLayout contents = new VerticalLayout();
			contents.addComponent(new Label("Web UI. Endpoint types: " + enpT.toString()));
			Button logout = new Button("logout");
			logout.addClickListener(new Button.ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					System.out.println("Session destroyed111");
					/*
					VaadinSession vs = VaadinSession.getCurrent();
					System.out.println("Session destroyed111");
					WrappedSession s = vs.getSession();
					System.out.println("Session destroyed222");
					Page p = Page.getCurrent();
					System.out.println("Session destroyed333");
					s.invalidate();
					System.out.println("Session destroyed");
					p.setLocation("/admin");
					*/
				}
			});
			contents.addComponent(logout);
			setContent(contents);
		} catch (EngineException e)
		{
			e.printStackTrace();
		}
	}

}
