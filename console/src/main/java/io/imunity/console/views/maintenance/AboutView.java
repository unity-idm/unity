/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.BoldLabel;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.FormLayoutLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.engine.api.version.VersionInformation;
import pl.edu.icm.unity.engine.api.version.VersionInformationProvider;

import jakarta.annotation.security.PermitAll;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.maintenance.about", parent = "WebConsoleMenu.maintenance")
@Route(value = "/about", layout = ConsoleMenu.class)
public class AboutView extends ConsoleViewComponent
{
	AboutView(MessageSource msg, NotificationPresenter notificationPresenter, VersionInformationProvider informationProvider)
	{
		VersionInformation versionInformation;
		try
		{
			versionInformation = informationProvider.getVersionInformation();
		} catch (Exception e)
		{
			notificationPresenter.showError( "Can not get version information", e.getMessage());
			return;
		}

		FormLayout main = new FormLayout();
		main.addFormItem(new BoldLabel(versionInformation.version), new FormLayoutLabel(msg.getMessage("AboutView.version")));
		main.addFormItem(new BoldLabel(TimeUtil.formatStandardInstant(versionInformation.buildTime)), new FormLayoutLabel(msg.getMessage("AboutView.buildTime")));
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		getContent().add(new VerticalLayout(main));
	}
}
