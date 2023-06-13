/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.console;

import java.util.List;
import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;

public class UpmanServiceEditorGeneralTab extends GeneralTab
{
	private Binder<UpmanServiceConfiguration> upmanBinder;
	private List<String> homeServices;

	public UpmanServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type,
			List<String> usedEndpointsPaths, Set<String> serverContextPaths, List<String> homeServices)
	{
		super(msg, type, usedEndpointsPaths, serverContextPaths);
		this.homeServices = homeServices;
	}

	public void initUI(Binder<DefaultServiceDefinition> binder, Binder<UpmanServiceConfiguration> homeBinder,
			boolean editMode)
	{
		super.initUI(binder, editMode);
		this.upmanBinder = homeBinder;
		mainLayout.addComponent(buildContentSection());
	}

	private Component buildContentSection()
	{
		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		CheckBox enableHome = new CheckBox();
		enableHome.setCaption(msg.getMessage("UpmanServiceEditorGeneralTab.enableHome"));
		upmanBinder.forField(enableHome).bind("enableHome");
		main.addComponent(enableHome);

		ComboBox<String> homeService = new ComboBox<>();
		homeService.setEnabled(false);
		homeService.setCaption(msg.getMessage("UpmanServiceEditorGeneralTab.homeService"));
		homeService.setItems(homeServices);
		homeService.setEmptySelectionAllowed(false);
		upmanBinder.forField(homeService).bind("homeService");
		main.addComponent(homeService);

		enableHome.addValueChangeListener(e -> {
			homeService.setEnabled(e.getValue());
		});

		CollapsibleLayout contentSection = new CollapsibleLayout(
				msg.getMessage("UpmanServiceEditorGeneralTab.content"), main);
		contentSection.expand();
		return contentSection;
	}

}
