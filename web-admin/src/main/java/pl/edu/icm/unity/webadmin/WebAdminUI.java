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

import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupBrowserComponent;
import pl.edu.icm.unity.webui.UnityWebUI;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

/**
 * The main entry point of the web administration UI.
 * 
 * TODO - currently only a mess
 * @author K. Benedyczak
 */
@Component("WebAdminUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WebAdminUI extends UI implements UnityWebUI
{
	private static final long serialVersionUID = 1L;

	@Autowired
	private GroupsManagement test;
	
	@Autowired
	private GroupBrowserComponent groupBrowser;
	
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
			test.addGroup(new Group("/A"));
			test.addGroup(new Group("/A/B"));
			test.addGroup(new Group("/A/B/C"));
			test.addGroup(new Group("/D"));
			test.addGroup(new Group("/D/E"));
			test.addGroup(new Group("/D/G"));
			test.addGroup(new Group("/D/F"));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		groupBrowser.refresh();
		VerticalLayout contents = new VerticalLayout();
		contents.addComponent(groupBrowser);


		Button logout = new Button("logout");
		logout.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				VaadinSession vs = VaadinSession.getCurrent();
				WrappedSession s = vs.getSession();
				Page p = Page.getCurrent();
				s.invalidate();
				//TODO
				p.setLocation("/admin/admin");
			}
		});
		contents.addComponent(logout);
		setContent(contents);
	}

}
