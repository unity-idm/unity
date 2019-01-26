/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.UpManNavigationInfoProviderBase;
import io.imunity.upman.UpManRootNavigationInfoProvider;
import io.imunity.upman.UpManUI;
import io.imunity.upman.common.UpManView;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * User updates view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class UserUpdatesView extends CustomComponent implements UpManView
{

	public static final String VIEW_NAME = "UserUpdates";

	private UpdateRequestsController controller;
	private ConfirmationInfoFormatter formatter;
	private UnityMessageSource msg;

	@Autowired
	public UserUpdatesView(UnityMessageSource msg, UpdateRequestsController controller, ConfirmationInfoFormatter formatter)
	{
		this.msg = msg;
		this.controller = controller;
		this.formatter = formatter;
		setSizeFull();
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		String project = UpManUI.getProjectGroup();

		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setMargin(false);
		setCompositionRoot(main);

		UpdateRequestsComponent updateRequestsComponent;
		try
		{
			updateRequestsComponent = new UpdateRequestsComponent(msg, controller, project, formatter);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		main.addComponent(updateRequestsComponent);	
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("UpManMenu.userUpdates");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public com.vaadin.ui.Component getViewHeader()
	{
		HorizontalLayout header = new HorizontalLayout();
		header.setMargin(false);
		Label name = new Label(getDisplayedName());
		name.addStyleName(SidebarStyles.viewHeader.toString());
		header.addComponents(name);
		header.setComponentAlignment(name, Alignment.MIDDLE_CENTER);
		return header;
	}

	@Component
	public class MembersNavigationInfoProvider extends UpManNavigationInfoProviderBase
	{
		@Autowired
		public MembersNavigationInfoProvider(UnityMessageSource msg, UpManRootNavigationInfoProvider parent,
				ObjectFactory<UserUpdatesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("UpManMenu.userUpdates"))
					.withIcon(Images.user_check.getResource()).withPosition(3).build());

		}
	}

}
