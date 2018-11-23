/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.UpManNavigationInfoProviderBase;
import io.imunity.upman.UpManRootNavigationInfoProvider;
import io.imunity.upman.common.UpManView;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SidebarStyles;

/**
 * Invitations view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class InvitationsView extends CustomComponent implements UpManView
{

	public static final String VIEW_NAME = "Invitations";

	private UnityMessageSource msg;

	@Autowired
	public InvitationsView(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("Invitations");
		main.addComponent(title);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("UpManMenu.invitations");
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
		name.addStyleName(SidebarStyles.viewHeader.toString());
		Button addInvitationButton = new Button(msg.getMessage("Invitations.newInvite"),
				Images.add.getResource());
		header.addComponents(name, addInvitationButton);
		header.setComponentAlignment(name, Alignment.MIDDLE_CENTER);
		header.setComponentAlignment(addInvitationButton, Alignment.MIDDLE_CENTER);
		return header;
	}

	@Component
	public class InvitationsNavigationInfoProvider extends UpManNavigationInfoProviderBase
	{
		@Autowired
		public InvitationsNavigationInfoProvider(UnityMessageSource msg,
				UpManRootNavigationInfoProvider parent,
				ObjectFactory<InvitationsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage("UpManMenu.invitations"))
					.withIcon(Images.envelope_open.getResource()).withPosition(2)
					.build());

		}
	}

}
