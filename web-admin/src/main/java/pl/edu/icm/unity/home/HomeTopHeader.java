/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.home;

import java.util.Optional;

import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeader;

/**
 * 
 * Top header for home UI.
 *
 * @author P.Piernik
 */
public class HomeTopHeader extends TopHeader
{
	private Optional<String> projectManLink;

	public HomeTopHeader(String title, StandardWebAuthenticationProcessor authnProcessor, UnityMessageSource msg,
			Optional<String> upManLink)
	{
		super(title, authnProcessor, msg);
		this.projectManLink = upManLink;
		addButtons();
	}

	private void addButtons()
	{		
		if (projectManLink.isPresent())
		{
			Button goToProjectMan = createProjectManLinkButton();
			loggedPanel.addComponent(goToProjectMan);
			loggedPanel.setComponentAlignment(goToProjectMan, Alignment.MIDDLE_RIGHT);	
		}
		
		Button logout = createLogoutButton();
		loggedPanel.addComponent(logout);
		loggedPanel.setComponentAlignment(logout, Alignment.MIDDLE_RIGHT);
	}

	Button createProjectManLinkButton()
	{
		Button goToProjectMan = new Button();
		goToProjectMan.setIcon(Images.family.getResource());
		goToProjectMan.setDescription(msg.getMessage("HomeUIHeader.toProjectManagement"));
		goToProjectMan.addStyleName(Styles.vButtonLink.toString());
		goToProjectMan.addStyleName(Styles.largeIcon.toString());
		goToProjectMan.addClickListener(e -> Page.getCurrent().open(projectManLink.get(), null));
		return goToProjectMan;
	}
}
