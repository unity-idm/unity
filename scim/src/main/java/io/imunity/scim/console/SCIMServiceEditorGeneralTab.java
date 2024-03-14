/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;

import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.elements.TooltipFactory;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.idp.MandatoryGroupSelection;
import io.imunity.vaadin.auth.services.tabs.GeneralTab;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

public class SCIMServiceEditorGeneralTab extends GeneralTab
{
	private Binder<SCIMServiceConfigurationBean> restBinder;
	private final List<Group> allGroups;

	public SCIMServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type, List<String> usedEndpointsPaths,
			Set<String> serverContextPaths, List<Group> allGroups)
	{
		super(msg, type, usedEndpointsPaths, serverContextPaths);
		this.allGroups = List.copyOf(allGroups);
	}

	public void initUI(Binder<DefaultServiceDefinition> serviceBinder, Binder<SCIMServiceConfigurationBean> restBinder,
			boolean editMode)
	{
		super.initUI(serviceBinder, editMode);
		this.restBinder = restBinder;
		add(buildScimSection());
		add(buildCorsSection());
	}

	private AccordionPanel buildScimSection()
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		MandatoryGroupSelection rootGroup = new MandatoryGroupSelection(msg);
		rootGroup.setWidth(TEXT_FIELD_BIG.value());
		rootGroup.setItems(allGroups);
		main.addFormItem(rootGroup, msg.getMessage("SCIMServiceEditorGeneralTab.rootGroup"));
		restBinder.forField(rootGroup)
				.asRequired()
				.bind("rootGroup");

		MandatoryGroupSelection restAdminGroup = new MandatoryGroupSelection(msg);
		restAdminGroup.setItems(allGroups);
		restAdminGroup.setWidth(TEXT_FIELD_BIG.value());
		main.addFormItem(restAdminGroup, msg.getMessage("SCIMServiceEditorGeneralTab.restAdminGroup"))
				.add(TooltipFactory.get(msg.getMessage("SCIMServiceEditorGeneralTab.restAdminGroupDesc")));
		restBinder.forField(restAdminGroup)
				.bind("restAdminGroup");

		OptionalGroupWithWildcardSelection memeberShipGroups = new OptionalGroupWithWildcardSelection(msg);
		memeberShipGroups.setItems(allGroups);
		memeberShipGroups.setWidth(TEXT_FIELD_BIG.value());
		main.addFormItem(memeberShipGroups, msg.getMessage("SCIMServiceEditorGeneralTab.memebershipGroups"))
				.add(TooltipFactory.get(msg.getMessage("SCIMServiceEditorGeneralTab.memebershipGroupsDesc")));
		restBinder.forField(memeberShipGroups)
				.withConverter(List::copyOf, HashSet::new)
				.asRequired()
				.withValidator((value, context) ->
				{
					if (value == null || value.isEmpty())
						return ValidationResult.error(msg.getMessage("fieldRequired"));
					return ValidationResult.ok();
				})
				.bind("membershipGroups");

		OptionalGroupWithWildcardSelection excludedMembershipGroups = new OptionalGroupWithWildcardSelection(msg);
		excludedMembershipGroups.setItems(allGroups);
		main.addFormItem(excludedMembershipGroups,
				msg.getMessage("SCIMServiceEditorGeneralTab.excludedMembershipGroups"))
				.add(TooltipFactory.get(msg.getMessage("SCIMServiceEditorGeneralTab.excludedMembershipGroupsDesc")));
		excludedMembershipGroups.setWidth(TEXT_FIELD_BIG.value());
		restBinder.forField(excludedMembershipGroups)
				.withConverter(List::copyOf, HashSet::new)
				.bind("excludedMembershipGroups");

		AccordionPanel scimSection = new AccordionPanel(msg.getMessage("SCIMServiceEditorGeneralTab.scimGroups"), main);
		scimSection.setWidthFull();
		scimSection.setOpened(true);
		return scimSection;
	}

	private AccordionPanel buildCorsSection()
	{

		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		MultiSelectComboBox<String> allowedCORSheaders = new CustomValuesMultiSelectComboBox();
		allowedCORSheaders.setWidth(TEXT_FIELD_BIG.value());
		allowedCORSheaders.setPlaceholder(msg.getMessage("typeAndConfirm"));
		main.addFormItem(allowedCORSheaders, msg.getMessage("RestAdminServiceEditorComponent.allowedCORSheaders"));
		restBinder.forField(allowedCORSheaders)
				.withConverter(List::copyOf, HashSet::new)
				.bind("allowedCORSheaders");
		MultiSelectComboBox<String> allowedCORSorigins = new CustomValuesMultiSelectComboBox();
		allowedCORSorigins.setWidth(TEXT_FIELD_BIG.value());
		allowedCORSorigins.setPlaceholder(msg.getMessage("typeAndConfirm"));
		main.addFormItem(allowedCORSorigins, msg.getMessage("RestAdminServiceEditorComponent.allowedCORSorigins"));
		restBinder.forField(allowedCORSorigins)
				.withConverter(List::copyOf, HashSet::new)
				.bind("allowedCORSorigins");
		AccordionPanel corsSection = new AccordionPanel(msg.getMessage("RestAdminServiceEditorComponent.cors"), main);
		corsSection.setWidthFull();
		return corsSection;
	}
}
