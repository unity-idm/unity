/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.AuthenticationProcessor;

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
	protected UnityMessageSource msg;
	protected AuthenticationProcessor authnProcessor;
	
	public TopHeader(String title, AuthenticationProcessor authnProcessor, UnityMessageSource msg)
	{
		super(title, msg);
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		
		HorizontalLayout loggedPanel = new HorizontalLayout();
		loggedPanel.setSizeUndefined();
		loggedPanel.setSpacing(true);
		addComponent(loggedPanel);
		setComponentAlignment(loggedPanel, Alignment.BOTTOM_RIGHT);
		
		addLoggedInfo(loggedPanel);
		addButtons(loggedPanel);
	}
	
	protected void addLoggedInfo(HorizontalLayout loggedPanel)
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		String label = entity.getEntityLabel() == null ? "" : entity.getEntityLabel();
		Label loggedEntity = new Label(msg.getMessage("MainHeader.loggedAs", label,
				entity.getEntityId()));
		loggedEntity.setId("MainHeader.loggedAs");
		loggedEntity.setStyleName(Reindeer.LABEL_H2);
		loggedPanel.addComponent(loggedEntity);
		loggedPanel.setComponentAlignment(loggedEntity, Alignment.MIDDLE_RIGHT);
	}
	
	protected void addButtons(HorizontalLayout loggedPanel)
	{
		Button logout = createLogoutButton();
		loggedPanel.addComponent(logout);
	}
	
	protected Button createLogoutButton()
	{
		Button logout = new Button();
		logout.setIcon(Images.exit32.getResource());
		logout.setDescription(msg.getMessage("MainHeader.logout"));
		logout.setId("MainHeader.logout");
		logout.setStyleName(Reindeer.BUTTON_LINK);
		logout.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				authnProcessor.logout();
			}
		});
		return logout;
	}
}
