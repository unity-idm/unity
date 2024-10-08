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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;

public class UnityLayoutWrapper
{
	public static void wrap(HasComponents main, Component toWrap,
			VaadinEndpointProperties currentWebAppVaadinProperties, ExtraPanelsConfigurationProvider config,
			boolean authenticationView)
	{

		if (!authenticationView && currentWebAppVaadinProperties != null && !currentWebAppVaadinProperties
				.getBooleanValue(VaadinEndpointProperties.EXTRA_PANELS_AFTER_ATHENTICATION))
		{
			addWithoutExtraPanels(main, toWrap);
			return;
		}

		ExtraLayoutPanel top = getExtraTopPanel(currentWebAppVaadinProperties, config);
		ExtraLayoutPanel bottom = getExtraBottomPanel(currentWebAppVaadinProperties, config);
		ExtraLayoutPanel left = getExtraLeftPanel(currentWebAppVaadinProperties, config);
		ExtraLayoutPanel right = getExtraRightPanel(currentWebAppVaadinProperties, config);

		top.setWidthFull();
		left.setHeightFull();
		right.setHeightFull();
		bottom.setWidthFull();

		wrapMainLayout(main, toWrap, right, left, top, bottom);
	}
	
	private static void addWithoutExtraPanels(HasComponents main, Component toWrap)
	{
		HorizontalLayout mainLayout = new HorizontalLayout();
		mainLayout.setClassName(CssClassNames.MAIN_LAYOUT_CONTAINER.getName());
		mainLayout.add(toWrap);
		main.add(mainLayout);	
	}

	private static void wrapMainLayout(HasComponents main, Component toWrap, Component right, Component left,
			Component top, Component bottom)
	{
		HorizontalLayout mainLayout = new HorizontalLayout();
		mainLayout.setClassName(CssClassNames.MAIN_LAYOUT_CONTAINER.getName());
		toWrap.getStyle()
				.set("overflow", "auto");
		mainLayout.add(left, toWrap, right);
		mainLayout.setSizeFull();

		VerticalLayout wrapper = new VerticalLayout(top, mainLayout, bottom);
		wrapper.setSizeFull();
		wrapper.setPadding(false);
		wrapper.setMargin(false);
		wrapper.setSpacing(false);
		main.add(wrapper);

	}

	private static ExtraLayoutPanel getExtraTopPanel(VaadinEndpointProperties currentWebAppVaadinProperties,
			ExtraPanelsConfigurationProvider config)
	{
		Optional<File> extraPanel = currentWebAppVaadinProperties != null
				? currentWebAppVaadinProperties.getExtraTopPanel(config.getDefaultWebConentPath())
				: Optional.empty();

		return new ExtraLayoutPanel("unity-layout-top",
				(extraPanel.isEmpty() ? config.getExtraTopPanel() : extraPanel).orElse(null));
	}

	private static ExtraLayoutPanel getExtraBottomPanel(VaadinEndpointProperties currentWebAppVaadinProperties,
			ExtraPanelsConfigurationProvider config)
	{
		Optional<File> extraPanel = currentWebAppVaadinProperties != null
				? currentWebAppVaadinProperties.getExtraBottomPanel(config.getDefaultWebConentPath())
				: Optional.empty();

		return new ExtraLayoutPanel("unity-layout-bottom",
				(extraPanel.isEmpty() ? config.getExtraBottomPanel() : extraPanel).orElse(null));
	}

	private static ExtraLayoutPanel getExtraRightPanel(VaadinEndpointProperties currentWebAppVaadinProperties,
			ExtraPanelsConfigurationProvider config)
	{
		Optional<File> extraPanel = currentWebAppVaadinProperties != null
				? currentWebAppVaadinProperties.getExtraRightPanel(config.getDefaultWebConentPath())
				: Optional.empty();

		return new ExtraLayoutPanel("unity-layout-right",
				(extraPanel.isEmpty() ? config.getExtraRightPanel() : extraPanel).orElse(null));
	}

	private static ExtraLayoutPanel getExtraLeftPanel(VaadinEndpointProperties currentWebAppVaadinProperties,
			ExtraPanelsConfigurationProvider config)
	{
		Optional<File> extraPanel = currentWebAppVaadinProperties != null
				? currentWebAppVaadinProperties.getExtraLeftPanel(config.getDefaultWebConentPath())
				: Optional.empty();

		return new ExtraLayoutPanel("unity-layout-left",
				(extraPanel.isEmpty() ? config.getExtraLeftPanel() : extraPanel).orElse(null));
	}
}
