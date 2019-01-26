/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
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
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;
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
	private ConfirmationInfoFormatter formatter;

	@Autowired
	public GroupMembersView(UnityMessageSource msg, GroupMembersController controller, ConfirmationInfoFormatter formatter)
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

		List<DelegatedGroup> groups;
		try
		{
			groups = controller.getProjectGroups(project);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		
		MandatoryGroupSelection subGroupCombo = new MandatoryGroupSelection(msg);
		subGroupCombo.setWidth(30, Unit.EM);
		subGroupCombo.setCaption(msg.getMessage("GroupMemberView.subGroupComboCaption"));
		subGroupCombo.setItems(groups.stream().map(dg -> {
			Group g = new Group(dg.path);
			if (dg.path.equals(project))
			{
				g.setDisplayedName(new I18nString(dg.displayedName + " (" + msg.getMessage("AllMemebers") + ")"));
			}else
			{
				g.setDisplayedName(new I18nString(dg.displayedName));
			}
			return g;
		}).collect(Collectors.toList()));
		subGroupCombo.setRequiredIndicatorVisible(false);
		
		FormLayout subGroupComboWrapper = new FormLayout(subGroupCombo);
		main.addComponent(subGroupComboWrapper);
		main.setExpandRatio(subGroupComboWrapper, 0);
		GroupMembersComponent groupMembersComponent;
		try
		{
			groupMembersComponent = new GroupMembersComponent(msg, controller, project, formatter);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}
		main.addComponent(groupMembersComponent);
		main.setExpandRatio(groupMembersComponent, 2);
		subGroupCombo.addValueChangeListener(e -> groupMembersComponent.setGroup(subGroupCombo.getSelectedGroup()));
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
				ObjectFactory<GroupMembersView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.DefaultView)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("UpManMenu.members"))
					.withIcon(Images.family.getResource()).withPosition(0).build());

		}
	}

}
