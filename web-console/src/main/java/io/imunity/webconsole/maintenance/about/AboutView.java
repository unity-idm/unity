/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.maintenance.about;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.engine.api.version.VersionInformation;
import pl.edu.icm.unity.engine.api.version.VersionInformationProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
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

	private final MessageSource msg;
	private final VersionInformationProvider informationProvider;
	
	@Autowired
	AboutView(MessageSource msg, VersionInformationProvider infoProvider)
	{
		this.msg = msg;
		this.informationProvider = infoProvider;

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
		
		Label buildTime = new Label();
		buildTime.setCaption(msg.getMessage("AboutView.buildTime"));
		buildTime.addStyleName(Styles.bold.toString());
		main.addComponent(buildTime);
		VersionInformation versionInformation;
		try
		{
			 versionInformation = informationProvider.getVersionInformation();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, "Can not get version information", e);
			return;
		}
		version.setValue(versionInformation.version);
		buildTime.setValue(TimeUtil.formatStandardInstant(versionInformation.buildTime));
		
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
