/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.UpManNavigationInfoProviderBase;
import io.imunity.upman.UpManRootNavigationInfoProvider;
import io.imunity.upman.common.UpManStyles;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;

/**
 * User updates view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class UserUpdatesView extends CustomComponent implements UnityView
{

	public static final String VIEW_NAME = "UserUpdates";

	private UnityMessageSource msg;

	@Autowired
	public UserUpdatesView(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("User updates");
		main.addComponent(title);
		setCompositionRoot(main);
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
		HorizontalLayout header = new  HorizontalLayout();
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
				ObjectFactory<UserUpdatesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage("UpManMenu.userUpdates"))
					.withIcon(Images.user_check.getResource()).withPosition(3)
					.build());

		}
	}

}
