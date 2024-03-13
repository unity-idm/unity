/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.web.console;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.endpoint.common.api.services.DefaultServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.tabs.GeneralTab;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * REST Admin service editor general tab
 * 
 * @author P.Piernik
 *
 */
public class RestAdminServiceEditorGeneralTab extends GeneralTab
{
	private Binder<RestAdminServiceConfiguration> restBinder;

	public RestAdminServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type,
			List<String> usedEndpointsPaths, List<String> usedNames,  Set<String> serverContextPaths)
	{
		super(msg, type, usedEndpointsPaths, usedNames, serverContextPaths);
	}

	public void initUI(Binder<DefaultServiceDefinition> serviceBinder, Binder<RestAdminServiceConfiguration> restBinder,
			boolean editMode)
	{
		super.initUI(serviceBinder, editMode);
		this.restBinder = restBinder;
		add(buildCorsSection());
	}

	private Component buildCorsSection()
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
