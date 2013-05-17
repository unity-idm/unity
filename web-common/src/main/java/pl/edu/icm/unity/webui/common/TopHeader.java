/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.net.URI;

import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;

/**
 * Top bar with header. Allows to logout.
 * @author K. Benedyczak
 */
public class TopHeader extends TopHeaderLight
{
	public TopHeader(String title, UnityMessageSource msg)
	{
		super(title, msg);
		
		HorizontalLayout loggedPanel = new HorizontalLayout();
		loggedPanel.setSizeUndefined();
		loggedPanel.setSpacing(true);
		AuthenticatedEntity entity = InvocationContext.getCurrent().getAuthenticatedEntity();
		Label loggedEntity = new Label(msg.getMessage("MainHeader.loggedAs", entity.getAuthenticatedWith().get(0),
				entity.getEntityId()));
		loggedEntity.setStyleName(Reindeer.LABEL_H2);
		loggedPanel.addComponent(loggedEntity);
		loggedPanel.setComponentAlignment(loggedEntity, Alignment.MIDDLE_RIGHT);
		
		Button logout = new Button();
		logout.setIcon(Images.logout.getResource());
		logout.setDescription(msg.getMessage("MainHeader.logout"));
		logout.setStyleName(Reindeer.BUTTON_LINK);
		logout.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				VaadinSession vs = VaadinSession.getCurrent();
				WrappedSession s = vs.getSession();
				Page p = Page.getCurrent();
				URI currentLocation = p.getLocation();
				s.invalidate();
				p.setLocation(currentLocation);
			}
		});
		loggedPanel.addComponent(logout);
		
		addComponent(loggedPanel);
		setComponentAlignment(loggedPanel, Alignment.BOTTOM_RIGHT);
	}
}
