/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeader;

import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;


/**
 * Top header for admin UI. Allows to switch to (and from) user home UI view.
 * @author K. Benedyczak
 */
public class AdminTopHeader extends TopHeader
{
	private boolean adminView = true;
	private Button switchView;
	private ViewSwitchCallback callback;
	
	public AdminTopHeader(String title, WebAuthenticationProcessor authnProcessor, UnityMessageSource msg, 
			ViewSwitchCallback callback)
	{
		super(title, authnProcessor, msg);
		this.callback = callback;
	}

	@Override
	protected void addButtons(HorizontalLayout loggedPanel)
	{
		Button supportB = createSupportButton();
		loggedPanel.addComponent(supportB);
		loggedPanel.setComponentAlignment(supportB, Alignment.MIDDLE_CENTER);
		
		Button switchView = createSwitchButton();
		loggedPanel.addComponent(switchView);
		loggedPanel.setComponentAlignment(switchView, Alignment.MIDDLE_CENTER);

		Button logout = createLogoutButton();
		loggedPanel.addComponent(logout);
		loggedPanel.setComponentAlignment(logout, Alignment.MIDDLE_CENTER);
	}

	protected Button createSupportButton()
	{
//		Link support = new Link();
//		support.setResource(new ExternalResource("http://unity-idm.eu/site/support"));
//		support.setTargetName("_blank");
//		support.setDescription(msg.getMessage("AdminTopHeader.toSupport"));
//		support.setIcon(Images.support32.getResource());
//		return support;

		Button support = new Button();
		support.addStyleName(Styles.vButtonLink.toString());
		support.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				Page.getCurrent().open("http://unity-idm.eu/site/support", "_blank", false);
			}
		});
		support.setDescription(msg.getMessage("AdminTopHeader.toSupport"));
		support.setIcon(Images.support32.getResource());
		return support;
	}
	
	protected Button createSwitchButton()
	{
		switchView = new Button();
		switchView.addStyleName(Styles.vButtonLink.toString());
		switchView.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				switchView();
				callback.showView(adminView);
			}
		});
		switchView();
		return switchView;
	}

	private void switchView()
	{
		if (adminView)
		{
			switchView.setIcon(Images.toAdmin32.getResource());
			switchView.setDescription(msg.getMessage("AdminTopHeader.toAdmin"));
			adminView = false;
		} else
		{
			switchView.setIcon(Images.toProfile32.getResource());
			switchView.setDescription(msg.getMessage("AdminTopHeader.toProfile"));
			adminView = true;
		}
	}
	
	public interface ViewSwitchCallback
	{
		public void showView(boolean admin);
	}
}
