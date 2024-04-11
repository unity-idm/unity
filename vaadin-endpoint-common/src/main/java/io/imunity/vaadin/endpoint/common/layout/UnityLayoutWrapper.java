/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.layout;

import java.io.File;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import io.imunity.vaadin.elements.ExtraLayoutPanel;
import io.imunity.vaadin.endpoint.common.Vaadin82XEndpointProperties;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

public class UnityLayoutWrapper
{
	public static void wrap(HasComponents main, Component toWrap,
			Vaadin82XEndpointProperties currentWebAppVaadinProperties, UnityServerConfiguration config,
			boolean authenticationView)
	{

		if (!authenticationView && currentWebAppVaadinProperties != null && !currentWebAppVaadinProperties
				.getBooleanValue(Vaadin82XEndpointProperties.EXTRA_PANELS_AFTER_ATHENTICATION))
		{
			HorizontalLayout mainLayout = new HorizontalLayout();
			mainLayout.setClassName("u-main-layout-container");
			mainLayout.add(toWrap);
			main.add(mainLayout);
			return;
		}

		Optional<File> extraTopPanel = currentWebAppVaadinProperties != null
				? currentWebAppVaadinProperties.getExtraTopPanel()
				: Optional.empty();
		Optional<File> extraBottomPanel = currentWebAppVaadinProperties != null
				? currentWebAppVaadinProperties.getExtraBottomPanel()
				: Optional.empty();
		Optional<File> extraLeftPanel = currentWebAppVaadinProperties != null
				? currentWebAppVaadinProperties.getExtraLeftPanel()
				: Optional.empty();
		Optional<File> extraRightPanel = currentWebAppVaadinProperties != null
				? currentWebAppVaadinProperties.getExtraRightPanel()
				: Optional.empty();

		if (extraTopPanel.isEmpty())
		{
			extraTopPanel = config.getExtraTopPanel();
		}

		if (extraBottomPanel.isEmpty())
		{
			extraBottomPanel = config.getExtraBottomPanel();
		}

		if (extraLeftPanel.isEmpty())
		{
			extraLeftPanel = config.getExtraLeftPanel();
		}

		if (extraRightPanel.isEmpty())
		{
			extraRightPanel = config.getExtraRightPanel();
		}

		ExtraLayoutPanel top = new ExtraLayoutPanel("unity-layout-top", extraTopPanel.orElse(null));
		ExtraLayoutPanel left = new ExtraLayoutPanel("unity-layout-left", extraLeftPanel.orElse(null));
		ExtraLayoutPanel right = new ExtraLayoutPanel("unity-layout-right", extraRightPanel.orElse(null));
		ExtraLayoutPanel bottom = new ExtraLayoutPanel("unity-layout-bottom", extraBottomPanel.orElse(null));

		HorizontalLayout mainLayout = new HorizontalLayout();
		mainLayout.setClassName("u-main-layout-container");
		mainLayout.add(left, toWrap, right);
		main.add(top, mainLayout, bottom);
	}
}
