/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web.console;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.tabs.GeneralTab;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;


/**
 * JWT service editor general tab
 * 
 * @author P.Piernik
 *
 */
class JWTServiceEditorGeneralTab extends GeneralTab
{
	private Binder<JWTServiceConfiguration> jwtBinder;
	private Set<String> credentials;

	JWTServiceEditorGeneralTab(MessageSource msg, EndpointTypeDescription type, List<String> usedEndpointsPaths, Set<String> serverContextPaths,
			Set<String> credentials)
	{
		super(msg, type, usedEndpointsPaths, serverContextPaths);
		this.credentials = credentials;
	}

	void initUI(Binder<DefaultServiceDefinition> serviceBinder, Binder<JWTServiceConfiguration> jwtBinder,
			boolean editMode)
	{
		super.initUI(serviceBinder, editMode);
		this.jwtBinder = jwtBinder;

		add(buildCorsSection());
		add(buildJWTSection());
	}

	private AccordionPanel buildCorsSection()
	{

		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		MultiSelectComboBox<String> allowedCORSheaders = new CustomValuesMultiSelectComboBox();
		allowedCORSheaders.setWidth(TEXT_FIELD_BIG.value());
		allowedCORSheaders.setPlaceholder(msg.getMessage("typeAndConfirm"));
		main.addFormItem(allowedCORSheaders, msg.getMessage("JWTServiceEditorComponent.allowedCORSheaders"));
		jwtBinder.forField(allowedCORSheaders)
				.withConverter(List::copyOf, HashSet::new)
				.bind("allowedCORSheaders");
		MultiSelectComboBox<String> allowedCORSorigins = new CustomValuesMultiSelectComboBox();
		allowedCORSorigins.setWidth(TEXT_FIELD_BIG.value());
		allowedCORSorigins.setPlaceholder(msg.getMessage("typeAndConfirm"));
		main.addFormItem(allowedCORSorigins, msg.getMessage("JWTServiceEditorComponent.allowedCORSorigins"));
		jwtBinder.forField(allowedCORSorigins)
				.withConverter(List::copyOf, HashSet::new)
				.bind("allowedCORSorigins");
		AccordionPanel corsSection = new AccordionPanel(msg.getMessage("JWTServiceEditorComponent.cors"), main);
		corsSection.setWidthFull();
		return corsSection;
	}

	private AccordionPanel buildJWTSection()
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		ComboBox<String> credential = new ComboBox<>();
		credential.setId("cre");
		credential.setItems(credentials);
		jwtBinder.forField(credential).asRequired(msg.getMessage("fieldRequired")).bind("credential");
		main.addFormItem(credential, msg.getMessage("JWTServiceEditorComponent.signingCredential"));

		IntegerField ttl = new IntegerField();
		ttl.setStepButtonsVisible(true);
		ttl.setMin(0);
		jwtBinder.forField(ttl).asRequired(msg.getMessage("fieldRequired"))
				.bind("ttl");
		main.addFormItem(ttl, msg.getMessage("JWTServiceEditorComponent.tokenTTL"));

		
		AccordionPanel jwtSection = new AccordionPanel(msg.getMessage("JWTServiceEditorComponent.jwt"), main);
		jwtSection.setOpened(true);
		jwtSection.setWidthFull();
		return jwtSection;
	}
}
