/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.console.v8;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.HomeEndpointProperties.RemovalModes;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.GridWithEditor;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;

import java.util.List;
import java.util.Set;

public class HomeServiceEditorGeneralTab extends GeneralTab
{
	private Binder<HomeServiceConfiguration> homeBinder;
	private List<String> allAttributes;
	private List<String> allImageAttributes;
	private List<Group> allGroups;
	private List<String> upManServices;
	private List<String> enquiryForms;

	private ChipsWithDropdown<String> enabledControls;
	private CheckBox allowRemovalSheduling;

	public HomeServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type, List<String> usedEndpointsPaths, Set<String> serverContextPaths,
			List<String> allAttributes, List<String> allImageAttributes, List<Group> allGroups, List<String> upManServices,
			List<String> enquiryForms, List<String> registrationForms)
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
		mainLayout.addComponent(buildContentSection());
	}

	private Component buildContentSection()
	{
		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		ChipsWithDropdown<String> enabledTabs = new ChipsWithDropdown<>();
		enabledTabs.setCaption(msg.getMessage("HomeServiceEditorComponent.enabledSections"));
		enabledTabs.setItems(HomeServiceEditorComponent.getAvailableTabs());
		homeBinder.forField(enabledTabs).bind("enabledTabs");
		main.addComponent(enabledTabs);

		enabledControls = new ChipsWithDropdown<>();
		enabledControls.setCaption(msg.getMessage("HomeServiceEditorComponent.enabledUserDetailsControls"));
		enabledControls.setItems(HomeServiceEditorComponent.getAvailableControls());
		homeBinder.forField(enabledControls).bind("enabledUserDetailsControls");
		main.addComponent(enabledControls);

		GridWithEditor<ExposedAttribute> exposedAttributes = new GridWithEditor<>(msg, ExposedAttribute.class);
		exposedAttributes.setEnabled(false);
		exposedAttributes.setCaption(msg.getMessage("HomeServiceEditorComponent.exposedAttributes"));
		exposedAttributes.addComboColumn(s -> s.getName(), (t, v) -> t.setName(v),
				msg.getMessage("HomeServiceEditorComponent.attribute"), allAttributes, 10, false);

		exposedAttributes.addCheckBoxColumn(s -> s.isEditable(), (t, v) -> t.setEditable(v),
				msg.getMessage("HomeServiceEditorComponent.attributeEditable"), 10);

		exposedAttributes.addCheckBoxColumn(s -> s.isShowGroup(), (t, v) -> t.setShowGroup(v),
				msg.getMessage("HomeServiceEditorComponent.attributeShowGroup"), 10);

		MandatoryGroupSelection groupCombo = new MandatoryGroupSelection(msg);
		groupCombo.setItems(allGroups);

		exposedAttributes.addCustomColumn(s -> s.getGroup(), g -> g.group.getDisplayedName().getValue(msg),
				(t, v) -> t.setGroup(v), groupCombo,
				msg.getMessage("HomeServiceEditorComponent.attributeGroup"), 20);

		exposedAttributes.setWidth(100, Unit.PERCENTAGE);
		homeBinder.forField(exposedAttributes).bind("exposedAttributes");
		main.addComponent(exposedAttributes);

		enabledTabs.addValueChangeListener(e -> {
			boolean userDetTabEnabled = e.getValue()
					.contains(HomeEndpointProperties.Components.userDetailsTab.toString());
			exposedAttributes.setEnabled(userDetTabEnabled);
			enabledControls.setEnabled(userDetTabEnabled);
		});

		allowRemovalSheduling = new CheckBox();
		allowRemovalSheduling.setEnabled(false);
		allowRemovalSheduling.setCaption(msg.getMessage("HomeServiceEditorComponent.enableSelfRemovalScheduling"));
		homeBinder.forField(allowRemovalSheduling).bind("allowRemovalSheduling");
		main.addComponent(allowRemovalSheduling);

		ComboBox<RemovalModes> removalMode = new ComboBox<>();
		removalMode.setEnabled(false);
		removalMode.setCaption(msg.getMessage("HomeServiceEditorComponent.removalMode"));
		removalMode.setItems(RemovalModes.values());
		removalMode.setEmptySelectionAllowed(false);
		homeBinder.forField(removalMode).asRequired((v, c) -> {
			if (allowRemovalSheduling.getValue() && (v == null))
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			return ValidationResult.ok();
		}).bind("removalMode");
		main.addComponent(removalMode);

		enabledControls.addValueChangeListener(e -> {
			boolean userAccountRemovalEnabled = e.getValue()
					.contains(HomeEndpointProperties.Components.accountRemoval.toString());
			allowRemovalSheduling.setEnabled(userAccountRemovalEnabled);
			removalMode.setEnabled(isAccountRemovalEnabled());
		});

		allowRemovalSheduling.addValueChangeListener(
				e -> removalMode.setEnabled(isAccountRemovalEnabled() && e.getValue()));

		CheckBox allow2ndFactorOptIn = new CheckBox();
		allow2ndFactorOptIn.setCaption(msg.getMessage("HomeServiceEditorComponent.allow2ndFactorOptIn"));
		homeBinder.forField(allow2ndFactorOptIn).bind("allow2ndFactorOptIn");
		main.addComponent(allow2ndFactorOptIn);
		
		CheckBox enableUpMan = new CheckBox();
		enableUpMan.setCaption(msg.getMessage("HomeServiceEditorComponent.enableUpMan"));
		homeBinder.forField(enableUpMan).bind("enableUpMan");
		main.addComponent(enableUpMan);

		ComboBox<String> upmanService = new ComboBox<>();
		upmanService.setEnabled(false);
		upmanService.setCaption(msg.getMessage("HomeServiceEditorComponent.upmanService"));
		upmanService.setItems(upManServices);
		upmanService.setEmptySelectionAllowed(false);
		homeBinder.forField(upmanService).bind("upManService");
		main.addComponent(upmanService);

		ComboBox<String> imageAttributes = new ComboBox<>();
		imageAttributes.setCaption(msg.getMessage("HomeServiceEditorComponent.imageAttribute"));
		imageAttributes.setItems(allImageAttributes);
		imageAttributes.setEmptySelectionAllowed(false);
		homeBinder.forField(imageAttributes).bind("imageAttribute");
		main.addComponent(imageAttributes);

		enableUpMan.addValueChangeListener(e -> {
			upmanService.setEnabled(e.getValue());
		});

		ChipsWithDropdown<String> enquiryFormsCombo = new ChipsWithDropdown<>();
		enquiryFormsCombo.setEnabled(false);
		enquiryFormsCombo.setCaption(msg.getMessage("HomeServiceEditorComponent.enquiryForms"));
		enquiryFormsCombo.setItems(enquiryForms);
		homeBinder.forField(enquiryFormsCombo).bind("enquiryForms");
		main.addComponent(enquiryFormsCombo);

		enabledTabs.addValueChangeListener(e -> {
			boolean accountUpdateEnabled = e.getValue()
					.contains(HomeEndpointProperties.Components.accountUpdateTab.toString());
			enquiryFormsCombo.setEnabled(accountUpdateEnabled);
		});

		CollapsibleLayout contentSection = new CollapsibleLayout(
				msg.getMessage("HomeServiceEditorComponent.content"), main);
		contentSection.expand();
		return contentSection;
	}

	private boolean isAccountRemovalEnabled()
	{
		return enabledControls.getValue().contains(HomeEndpointProperties.Components.accountRemoval.toString())
				&& allowRemovalSheduling.getValue();
	}

}
