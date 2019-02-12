/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;

/**
 * Top bar with header. Allows to logout.
 * @author K. Benedyczak
 */
public class TopHeader extends TopHeaderLight
{
	protected UnityMessageSource msg;
	protected StandardWebAuthenticationProcessor authnProcessor;
	protected HorizontalLayout loggedPanel;
	
	public TopHeader(String title, StandardWebAuthenticationProcessor authnProcessor, UnityMessageSource msg)
	{
		super(title, msg);
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		
		loggedPanel = new HorizontalLayout();
		loggedPanel.setSizeUndefined();
		loggedPanel.setSpacing(true);
		loggedPanel.setMargin(false);
		addComponent(loggedPanel);
		setComponentAlignment(loggedPanel, Alignment.MIDDLE_RIGHT);
		addLoggedInfo();
	}
	
	protected void addLoggedInfo()
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
	
	protected Button createLogoutButton()
	{
		Button logout = new Button();
		logout.setIcon(Images.exit.getResource());
		logout.setDescription(msg.getMessage("MainHeader.logout"));
		logout.setId("MainHeader.logout");
		logout.addStyleName(Styles.vButtonLink.toString());
		logout.addStyleName(Styles.largeIcon.toString());
		logout.addClickListener((e) -> authnProcessor.logout());
		return logout;
	}
}
