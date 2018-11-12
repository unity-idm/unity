/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.UpManNavigationInfoProviderBase;
import io.imunity.upman.UpManRootNavigationInfoProvider;
import io.imunity.upman.UpManUI;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Members view, Acts as default view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class GroupMembersView extends CustomComponent implements UnityView
{

	public static final String VIEW_NAME = "Members";

	private UnityMessageSource msg;
	private GroupMembersController controller;
	private String project;

	@Autowired
	public GroupMembersView(UnityMessageSource msg, GroupMembersController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		this.project = UpManUI.getProjectName();
		VerticalLayout main = new VerticalLayout();

		ComboBox<String> subGroupCombo = new ComboBox<>(
				msg.getMessage("GroupMemberView.subGroupComboCaption"));
		main.addComponent(new FormLayout(subGroupCombo));
		Map<String, String> groups;
		try
		{
			groups = controller.getGroupsMap(project);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}

		List<String> sortedGroups = groups.keySet().stream().sorted()
				.collect(Collectors.toList());
		subGroupCombo.setItems(
				groups.keySet().stream().sorted().collect(Collectors.toList()));
		subGroupCombo.setItemCaptionGenerator(i -> groups.get(i));
		subGroupCombo.setEmptySelectionAllowed(false);

		GroupMembersComponent groupMembersComponent;
		try
		{
			groupMembersComponent = new GroupMembersComponent(msg, controller);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		main.addComponent(groupMembersComponent);
		subGroupCombo.addValueChangeListener(
				e -> groupMembersComponent.setGroup(project, e.getValue()));
		subGroupCombo.setValue(sortedGroups.iterator().next());

		setCompositionRoot(main);
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
		header.setMargin(true);
		Label name = new Label(getDisplayedName());
		name.setStyleName(Styles.textXLarge.toString());
		name.addStyleName(Styles.bold.toString());
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
