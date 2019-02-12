/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.groups;

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
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Groups management view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class GroupsView extends CustomComponent implements UpManView
{
	public static final String VIEW_NAME = "Groups";

	private UnityMessageSource msg;
	private GroupsController controller;

	@Autowired
	public GroupsView(UnityMessageSource msg, GroupsController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		String project = UpManUI.getProjectGroup();
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		setCompositionRoot(main);

		GroupsComponent groupsComponent;
		try
		{
			groupsComponent = new GroupsComponent(msg, controller, project);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		main.addComponent(groupsComponent);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("UpManMenu.groups");
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
	public class GroupsNavigationInfoProvider extends UpManNavigationInfoProviderBase
	{
		@Autowired
		public GroupsNavigationInfoProvider(UnityMessageSource msg,
				UpManRootNavigationInfoProvider parent,
				ObjectFactory<GroupsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage("UpManMenu.groups"))
					.withIcon(Images.file_tree.getResource()).withPosition(1)
					.build());

		}
	}

}
