/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.Label;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;

/**
 * Top bar with header. Allows to logout.
 * @author K. Benedyczak
 */
public class TopHeader extends TopHeaderLight
{
	protected UnityMessageSource msg;
	protected WebAuthenticationProcessor authnProcessor;
	
	public TopHeader(String title, WebAuthenticationProcessor authnProcessor, UnityMessageSource msg)
	{
		super(title, msg);
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		
		HorizontalLayout loggedPanel = new HorizontalLayout();
		loggedPanel.setSizeUndefined();
		loggedPanel.setSpacing(true);
		addComponent(loggedPanel);
		setComponentAlignment(loggedPanel, Alignment.MIDDLE_RIGHT);
		
		addLoggedInfo(loggedPanel);
		addButtons(loggedPanel);
	}
	
	protected void addLoggedInfo(HorizontalLayout loggedPanel)
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		String label = entity.getEntityLabel() == null ? "" : entity.getEntityLabel();
		Label loggedEntity = new Label(entity.getEntityLabel() != null ? 
				msg.getMessage("MainHeader.loggedAs", label) :
				msg.getMessage("MainHeader.loggedAsWithId", entity.getEntityId()));
		loggedEntity.setId("MainHeader.loggedAs");
		loggedPanel.addComponent(loggedEntity);
		loggedPanel.setComponentAlignment(loggedEntity, Alignment.MIDDLE_RIGHT);
	}
	
	protected void addButtons(HorizontalLayout loggedPanel)
	{
		Button logout = createLogoutButton();
		loggedPanel.addComponent(logout);
		loggedPanel.setComponentAlignment(logout, Alignment.MIDDLE_RIGHT);
	}
	
	protected Button createLogoutButton()
	{
		Button logout = new Button();
		logout.setIcon(Images.exit32.getResource());
		logout.setDescription(msg.getMessage("MainHeader.logout"));
		logout.setId("MainHeader.logout");
		logout.addStyleName(Styles.vButtonLink.toString());
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
