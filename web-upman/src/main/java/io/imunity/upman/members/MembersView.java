/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

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
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Members view, Acts as default view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class MembersView extends CustomComponent implements UnityView
{

	public static final String VIEW_NAME = "Members";

	private UnityMessageSource msg;

	@Autowired
	public MembersView(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("Members");
		main.addComponent(title);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("UpManMenu.members");
	}

	@Override
	public com.vaadin.ui.Component getViewHeader()
	{
		HorizontalLayout header = new  HorizontalLayout();
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
				ObjectFactory<MembersView> factory)
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
