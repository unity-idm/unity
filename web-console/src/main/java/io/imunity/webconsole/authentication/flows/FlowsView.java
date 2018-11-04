/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.AuthenticationNavigationInfoProvider;
import io.imunity.webconsole.idprovider.saml.SAMLView;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Lists all flows
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class FlowsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Flows";
	private UnityMessageSource msg;

	@Autowired
	public FlowsView(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();

		Label title = new Label();
		title.setValue("Flows");
		main.addComponent(title);

		Button link2 = new Button();
		link2.setCaption("Go to SAML");
		link2.addClickListener(e -> {
			NavigationHelper.goToView(SAMLView.VIEW_NAME);
		});

		main.addComponent(link2);
		setCompositionRoot(main);

	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.authentication.flows");
	}

	@Component
	public static class FlowsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public FlowsNavigationInfoProvider(UnityMessageSource msg,
				AuthenticationNavigationInfoProvider parent,
				ObjectFactory<FlowsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage(
							"WebConsoleMenu.authentication.flows"))
					.build());

		}
	}

}
