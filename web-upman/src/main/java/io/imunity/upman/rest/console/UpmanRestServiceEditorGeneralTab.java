/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.endpoint.common.api.services.DefaultServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.idp.MandatoryGroupSelection;
import io.imunity.vaadin.endpoint.common.api.services.tabs.GeneralTab;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

class UpmanRestServiceEditorGeneralTab extends GeneralTab
{

	private Binder<UpmanRestServiceConfiguration> restBinder;
	private final List<Group> allGroups;
	private final List<String> attributes;

	public UpmanRestServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type,
			List<String> usedEndpointsPaths, Set<String> serverContextPaths, List<Group> allGroups,
			List<String> attributes)
	{
		super(msg, type, usedEndpointsPaths, serverContextPaths);
		this.allGroups = allGroups;
		this.attributes = attributes;
	}

	public void initUI(Binder<DefaultServiceDefinition> serviceBinder, Binder<UpmanRestServiceConfiguration> restBinder,
			boolean editMode)
	{
		super.initUI(serviceBinder, editMode);
		this.restBinder = restBinder;
		add(buildGroupSection());
		add(buildAttributeSection());
		add(buildCorsSection());
	}

	private AccordionPanel buildAttributeSection()
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		MultiSelectComboBox<String> rootGroupAttributes = new MultiSelectComboBox<>();
		rootGroupAttributes.setWidth(TEXT_FIELD_BIG.value());
		rootGroupAttributes.setItems(attributes);
		rootGroupAttributes
				.setTooltipText(msg.getMessage("UpmanRestServiceEditorComponent.rootGroupAttributesDescription"));
		main.addFormItem(rootGroupAttributes, msg.getMessage("UpmanRestServiceEditorComponent.rootGroupAttributes"));
		restBinder.forField(rootGroupAttributes)
				.withConverter(List::copyOf, HashSet::new)
				.asRequired()
				.bind("rootGroupAttributes");

		AccordionPanel attributes = new AccordionPanel(msg.getMessage("UpmanRestServiceEditorComponent.attributes"),
				main);
		attributes.setWidthFull();
		attributes.setOpened(true);
		return attributes;

	}

	private AccordionPanel buildGroupSection()
	{

		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		MandatoryGroupSelection rootGroup = new MandatoryGroupSelection(msg);
		rootGroup.setWidth(TEXT_FIELD_BIG.value());
		rootGroup.setItems(allGroups);
		main.addFormItem(rootGroup, msg.getMessage("UpmanRestServiceEditorComponent.rootGroup"));
		restBinder.forField(rootGroup)
				.asRequired()
				.bind("rootGroup");

		MandatoryGroupSelection authorizationGroup = new MandatoryGroupSelection(msg);
		authorizationGroup.setWidth(TEXT_FIELD_BIG.value());
		authorizationGroup.setItems(allGroups);
		main.addFormItem(authorizationGroup, msg.getMessage("UpmanRestServiceEditorComponent.authorizationGroup"));

		restBinder.forField(authorizationGroup)
				.asRequired()
				.bind("authorizationGroup");

		AccordionPanel components = new AccordionPanel(msg.getMessage("UpmanRestServiceEditorComponent.groups"), main);
		components.setWidthFull();
		components.setOpened(true);
		return components;

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
