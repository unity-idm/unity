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

import io.imunity.upman.ProjectController;
import io.imunity.upman.UpManNavigationInfoProviderBase;
import io.imunity.upman.UpManRootNavigationInfoProvider;
import io.imunity.upman.UpManUI;
import io.imunity.upman.common.UpManView;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
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

	private MessageSource msg;
	private GroupsController controller;
	private ProjectController projectController;

	@Autowired
	public GroupsView(MessageSource msg, GroupsController controller, ProjectController projectController)
	{
		this.msg = msg;
		this.controller = controller;
		this.projectController = projectController;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		DelegatedGroup project;
		try
		{
			project = UpManUI.getProjectGroup();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		setCompositionRoot(main);
		main.setSizeFull();
		setSizeFull();

		GroupsComponent groupsComponent;
		try
		{
			groupsComponent = new GroupsComponent(msg, controller,  projectController.getProjectRole(project.path), project);
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
		name.addStyleName(Styles.viewHeader.toString());
		header.addComponents(name);
		header.setComponentAlignment(name, Alignment.MIDDLE_CENTER);
		return header;
	}

	@Component
	public class GroupsNavigationInfoProvider extends UpManNavigationInfoProviderBase
	{
		@Autowired
		public GroupsNavigationInfoProvider(MessageSource msg,
				ObjectFactory<GroupsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(UpManRootNavigationInfoProvider.ID)
					.withObjectFactory(factory)
					.withCaption(msg.getMessage("UpManMenu.groups"))
					.withIcon(Images.file_tree.getResource()).withPosition(1)
					.build());

		}
	}

}
