/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.maintenance.backupAndRestore;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.importExport.ImportExportComponent;
import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.maintenance.MaintenanceNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * 
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class BackupAndRestoreView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "BackupAndRestore";

	private UnityMessageSource msg;
	private ImportExportComponent importExportComponent;
	

	@Autowired
	BackupAndRestoreView(UnityMessageSource msg, ImportExportComponent importExportComponent)
	{
		this.msg = msg;
		this.importExportComponent = importExportComponent;
		
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		importExportComponent.setCaption(null);
		main.addComponent(importExportComponent);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.maintenance.backupAndRestore");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class AutomationNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public AutomationNavigationInfoProvider(UnityMessageSource msg,
				MaintenanceNavigationInfoProvider parent,
				ObjectFactory<BackupAndRestoreView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.maintenance.backupAndRestore"))
					.withPosition(10).build());

		}
	}
}
