/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.UpManNavigationInfoProviderBase;
import io.imunity.upman.UpManRootNavigationInfoProvider;
import io.imunity.upman.UpManUI;
import io.imunity.upman.common.UpManStyles;
import io.imunity.upman.common.UpManView;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Members view, Acts as default view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class GroupMembersView extends CustomComponent implements UpManView
{

	public static final String VIEW_NAME = "Members";

	private UnityMessageSource msg;
	private GroupMembersController controller;

	@Autowired
	public GroupMembersView(UnityMessageSource msg, GroupMembersController controller)
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
		
		if (project == null || project.isEmpty())
		{
			//TODO maybe put error icon to main
			return;
		}
		
		Map<String, String> groups;
		try
		{
			groups = controller.getProjectGroupsMap(project);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			//TODO maybe put error icon to main
			return;
		}

		groups.put(project,
				groups.get(project) + " (" + msg.getMessage("AllMemebers") + ")");
		GroupIndentCombo subGroupCombo = new GroupIndentCombo(
				msg.getMessage("GroupMemberView.subGroupComboCaption"), groups);
		main.addComponent(new FormLayout(subGroupCombo));
		GroupMembersComponent groupMembersComponent;
		try
		{
			groupMembersComponent = new GroupMembersComponent(msg, controller, project);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		main.addComponent(groupMembersComponent);
		subGroupCombo.addValueChangeListener(
				e -> groupMembersComponent.setGroup(e.getValue()));
		
		groupMembersComponent.setGroup(project);
		
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("UpManMenu.members");
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
		name.addStyleName(UpManStyles.viewHeader.toString());
		header.addComponents(name);
		return header;
	}

	@Component
	public class MembersNavigationInfoProvider extends UpManNavigationInfoProviderBase
	{
		@Autowired
		public MembersNavigationInfoProvider(UnityMessageSource msg,
				UpManRootNavigationInfoProvider parent,
				ObjectFactory<GroupMembersView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.DefaultView)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage("UpManMenu.members"))
					.withIcon(Images.family.getResource()).withPosition(0)
					.build());

		}
	}

}
