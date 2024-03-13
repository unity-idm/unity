/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.console;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.endpoint.common.api.services.DefaultServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.tabs.GeneralTab;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;

public class UpmanServiceEditorGeneralTab extends GeneralTab
{
	private Binder<UpmanServiceConfiguration> upmanBinder;
	private List<String> homeServices;

	public UpmanServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type,
			List<String> usedEndpointsPaths, List<String> usedNames, Set<String> serverContextPaths, List<String> homeServices)
	{
		super(msg, type, usedEndpointsPaths, usedNames, serverContextPaths);
		this.homeServices = homeServices;
	}

	public void initUI(Binder<DefaultServiceDefinition> binder, Binder<UpmanServiceConfiguration> homeBinder,
			boolean editMode)
	{
		super.initUI(binder, editMode);
		this.upmanBinder = homeBinder;
		add(buildContentSection());
	}

	private AccordionPanel buildContentSection()
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		Checkbox enableHome = new Checkbox();
		upmanBinder.forField(enableHome)
				.bind("enableHome");
		enableHome.setLabel(msg.getMessage("UpmanServiceEditorGeneralTab.enableHome"));
		main.addFormItem(enableHome, "");

		ComboBox<String> homeService = new ComboBox<>();
		homeService.setEnabled(false);
		homeService.setItems(homeServices);
		upmanBinder.forField(homeService)
				.bind("homeService");
		main.addFormItem(homeService, msg.getMessage("UpmanServiceEditorGeneralTab.homeService"));

		enableHome.addValueChangeListener(e ->
		{
			homeService.setEnabled(e.getValue());
		});

		AccordionPanel contentSection = new AccordionPanel(msg.getMessage("UpmanServiceEditorGeneralTab.content"),
				main);
		contentSection.setOpened(true);
		contentSection.setWidthFull();
		return contentSection;
	}

}
