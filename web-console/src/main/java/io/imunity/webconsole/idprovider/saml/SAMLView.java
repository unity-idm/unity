/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider.saml;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.idprovider.IdpNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * SAML view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class SAMLView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "SAML";

	private UnityMessageSource msg;

	@Autowired
	public SAMLView(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("SAML");
		main.addComponent(title);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.idpProvider.saml");
	}
	
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class SAMLNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public SAMLNavigationInfoProvider(UnityMessageSource msg,
				IdpNavigationInfoProvider parent, ObjectFactory<SAMLView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage(
							"WebConsoleMenu.idpProvider.saml"))
					.build());

		}
	}
}
