/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.console;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.HomeEndpointProperties.RemovalModes;
import io.imunity.vaadin.elements.grid.EditableGrid;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.idp.MandatoryGroupSelection;
import io.imunity.vaadin.auth.services.tabs.GeneralTab;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

public class HomeServiceEditorGeneralTab extends GeneralTab
{
	private final List<String> allAttributes;
	private final List<String> allImageAttributes;
	private final List<Group> allGroups;
	private final List<String> upManServices;
	private final List<String> enquiryForms;

	private Binder<HomeServiceConfiguration> homeBinder;
	private MultiSelectComboBox<String> enabledControls;
	private Checkbox allowRemovalSheduling;

	public HomeServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type, List<String> usedEndpointsPaths,
			Set<String> serverContextPaths, List<String> allAttributes, List<String> allImageAttributes,
			List<Group> allGroups, List<String> upManServices, List<String> enquiryForms,
			List<String> registrationForms)
	{
		super(msg, type, usedEndpointsPaths, serverContextPaths);
		this.allAttributes = allAttributes;
		this.allImageAttributes = allImageAttributes;
		this.allGroups = allGroups;
		this.upManServices = upManServices;
		this.enquiryForms = enquiryForms;
	}

	public void initUI(Binder<DefaultServiceDefinition> binder, Binder<HomeServiceConfiguration> homeBinder,
			boolean editMode)
	{
		super.initUI(binder, editMode);
		this.homeBinder = homeBinder;
		add(buildContentSection());
	}

	private AccordionPanel buildContentSection()
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());

		MultiSelectComboBox<String> enabledTabs = new MultiSelectComboBox<>();
		enabledTabs.setWidth(TEXT_FIELD_BIG.value());

		enabledTabs.setItems(HomeServiceEditorComponent.getAvailableTabs());
		homeBinder.forField(enabledTabs)
				.withConverter(List::copyOf, HashSet::new)
				.bind("enabledTabs");
		main.addFormItem(enabledTabs, msg.getMessage("HomeServiceEditorComponent.enabledSections"));

		enabledControls = new MultiSelectComboBox<>();
		enabledControls.setWidth(TEXT_FIELD_BIG.value());
		enabledControls.setItems(HomeServiceEditorComponent.getAvailableControls());
		homeBinder.forField(enabledControls)
				.withConverter(List::copyOf, HashSet::new)
				.bind("enabledUserDetailsControls");
		main.addFormItem(enabledControls, msg.getMessage("HomeServiceEditorComponent.enabledUserDetailsControls"));

		EditableGrid<ExposedAttribute> exposedAttributes = new EditableGrid<>(msg::getMessage, ExposedAttribute::new);
		exposedAttributes.setEnabled(false);
		exposedAttributes.setWidthFull();
		exposedAttributes.addComboBoxColumn(ExposedAttribute::getName, ExposedAttribute::setName, allAttributes)
				.setHeader(msg.getMessage("HomeServiceEditorComponent.attribute"))
				.setAutoWidth(true);

		exposedAttributes.addCheckboxColumn(ExposedAttribute::isEditable, ExposedAttribute::setEditable)
				.setHeader(msg.getMessage("HomeServiceEditorComponent.attributeEditable"));

		exposedAttributes.addCheckboxColumn(ExposedAttribute::isShowGroup, ExposedAttribute::setShowGroup)
				.setHeader(msg.getMessage("HomeServiceEditorComponent.attributeShowGroup"));

		MandatoryGroupSelection groupCombo = new MandatoryGroupSelection(msg);
		groupCombo.setWidth(TEXT_FIELD_BIG.value());
		groupCombo.setItems(allGroups);

		exposedAttributes.addCustomColumn(s -> s.getGroup(), new ComponentRenderer<>(eg -> new NativeLabel(eg.getGroup()
				.group()
				.getDisplayedName()
				.getValue(msg))), (t, v) -> t.setGroup(v), groupCombo);
		homeBinder.forField(exposedAttributes)
				.bind("exposedAttributes");
		main.addFormItem(exposedAttributes, msg.getMessage("HomeServiceEditorComponent.exposedAttributes"));

		enabledTabs.addValueChangeListener(e ->
		{
			boolean userDetTabEnabled = e.getValue()
					.contains(HomeEndpointProperties.Components.userDetailsTab.toString());
			exposedAttributes.setEnabled(userDetTabEnabled);
			enabledControls.setEnabled(userDetTabEnabled);
		});

		allowRemovalSheduling = new Checkbox();
		allowRemovalSheduling.setEnabled(false);
		allowRemovalSheduling.setLabel(msg.getMessage("HomeServiceEditorComponent.enableSelfRemovalScheduling"));
		homeBinder.forField(allowRemovalSheduling)
				.bind("allowRemovalSheduling");
		main.addFormItem(allowRemovalSheduling, "");

		ComboBox<RemovalModes> removalMode = new ComboBox<>();
		removalMode.setEnabled(false);
		removalMode.setItems(RemovalModes.values());
		homeBinder.forField(removalMode)
				.asRequired((v, c) ->
				{
					if (allowRemovalSheduling.getValue() && (v == null))
						return ValidationResult.error(msg.getMessage("fieldRequired"));
					return ValidationResult.ok();
				})
				.bind("removalMode");
		main.addFormItem(removalMode, msg.getMessage("HomeServiceEditorComponent.removalMode"));

		enabledControls.addValueChangeListener(e ->
		{
			boolean userAccountRemovalEnabled = e.getValue()
					.contains(HomeEndpointProperties.Components.accountRemoval.toString());
			allowRemovalSheduling.setEnabled(userAccountRemovalEnabled);
			removalMode.setEnabled(isAccountRemovalEnabled());
		});

		allowRemovalSheduling
				.addValueChangeListener(e -> removalMode.setEnabled(isAccountRemovalEnabled() && e.getValue()));

		Checkbox allow2ndFactorOptIn = new Checkbox();
		allow2ndFactorOptIn.setLabel(msg.getMessage("HomeServiceEditorComponent.allow2ndFactorOptIn"));
		homeBinder.forField(allow2ndFactorOptIn)
				.bind("allow2ndFactorOptIn");
		main.addFormItem(allow2ndFactorOptIn, "");

		Checkbox enableUpMan = new Checkbox();
		enableUpMan.setLabel(msg.getMessage("HomeServiceEditorComponent.enableUpMan"));
		homeBinder.forField(enableUpMan)
				.bind("enableUpMan");
		main.addFormItem(enableUpMan, "");
		ComboBox<String> upmanService = new ComboBox<>();
		upmanService.setEnabled(false);
		upmanService.setItems(upManServices);
		homeBinder.forField(upmanService)
				.bind("upManService");
		main.addFormItem(upmanService, msg.getMessage("HomeServiceEditorComponent.upmanService"));

		ComboBox<String> imageAttributes = new ComboBox<>();
		imageAttributes.setWidth(TEXT_FIELD_BIG.value());
		imageAttributes.setItems(allImageAttributes);
		homeBinder.forField(imageAttributes)
				.bind("imageAttribute");
		main.addFormItem(imageAttributes, msg.getMessage("HomeServiceEditorComponent.imageAttribute"));

		enableUpMan.addValueChangeListener(e ->
		{
			upmanService.setEnabled(e.getValue());
		});

		MultiSelectComboBox<String> enquiryFormsCombo = new MultiSelectComboBox<>();
		enquiryFormsCombo.setWidth(TEXT_FIELD_BIG.value());
		enquiryFormsCombo.setEnabled(false);
		enquiryFormsCombo.setItems(enquiryForms);
		homeBinder.forField(enquiryFormsCombo)
				.withConverter(List::copyOf, HashSet::new)
				.bind("enquiryForms");
		main.addFormItem(enquiryFormsCombo, msg.getMessage("HomeServiceEditorComponent.enquiryForms"));

		enabledTabs.addValueChangeListener(e ->
		{
			boolean accountUpdateEnabled = e.getValue()
					.contains(HomeEndpointProperties.Components.accountUpdateTab.toString());
			enquiryFormsCombo.setEnabled(accountUpdateEnabled);
		});

		AccordionPanel contentSection = new AccordionPanel(msg.getMessage("HomeServiceEditorComponent.content"), main);
		contentSection.setWidthFull();
		contentSection.setOpened(true);
		return contentSection;

	}

	private boolean isAccountRemovalEnabled()
	{
		return enabledControls.getValue()
				.contains(HomeEndpointProperties.Components.accountRemoval.toString())
				&& allowRemovalSheduling.getValue();
	}

}
