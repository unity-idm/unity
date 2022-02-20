/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.maintenance.about;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.maintenance.MaintenanceNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * 
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class AboutView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "About";
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AboutView.class);

	private MessageSource msg;

	@Autowired
	AboutView(MessageSource msg)
	{
		this.msg = msg;

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		FormLayout main = new FormLayout();
		main.setMargin(false);

		Label version = new Label();
		version.setCaption(msg.getMessage("AboutView.version"));
		version.addStyleName(Styles.bold.toString());
		main.addComponent(version);
		try
		{
			version.setValue(getVersion());
		} catch (IOException e)
		{
			log.error("Can not read application version", e);
		}

		setCompositionRoot(main);
	}

	public String getVersion() throws IOException
	{
		Manifest manifest = new Manifest(
				Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
		Attributes attr = manifest.getMainAttributes();
		return attr.getValue("Implementation-Version");
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.maintenance.about");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class AboutInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public AboutInfoProvider(MessageSource msg, ObjectFactory<AboutView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(MaintenanceNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.maintenance.about")).withIcon(Images.info.getResource())
					.withPosition(40).build());

		}
	}
}
