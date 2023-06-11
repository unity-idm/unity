/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.pki;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.settings.SettingsNavigationInfoProvider;
import io.imunity.webconsole.settings.pki.cert.CertificatesComponent;
import io.imunity.webconsole.settings.pki.cert.CertificatesController;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Public Key Infrastructure view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class PKIView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "PKI";

	private MessageSource msg;
	private CertificatesController certController;

	@Autowired
	public PKIView(MessageSource msg, CertificatesController controller)
	{
		this.msg = msg;
		this.certController = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{	
		VerticalLayout main = new VerticalLayout();
		main.addComponent(new CertificatesComponent(msg, certController));
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{

		return msg.getMessage("WebConsoleMenu.settings.pki");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class PKINavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static String ID = VIEW_NAME;
		
		@Autowired
		public PKINavigationInfoProvider(MessageSource msg, ObjectFactory<PKIView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(SettingsNavigationInfoProvider.ID).withObjectFactory(factory)
					.withIcon(Images.diploma.getResource())
					.withShortCaption(msg.getMessage("WebConsoleMenu.settings.pki"))
					.withCaption(msg.getMessage("WebConsoleMenu.settings.publicKeyInfrastructure"))
					.withPosition(30)
					.build());
		}
	}

}
