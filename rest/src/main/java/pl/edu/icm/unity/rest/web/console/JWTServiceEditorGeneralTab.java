/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web.console;

import java.util.List;
import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;

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

		mainLayout.addComponent(buildCorsSection());
		mainLayout.addComponent(buildJWTSection());
	}

	private Component buildCorsSection()
	{

		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		ChipsWithTextfield allowedCORSheaders = new ChipsWithTextfield(msg);
		allowedCORSheaders.setCaption(msg.getMessage("JWTServiceEditorComponent.allowedCORSheaders"));
		jwtBinder.forField(allowedCORSheaders).bind("allowedCORSheaders");
		main.addComponent(allowedCORSheaders);

		ChipsWithTextfield allowedCORSorigins = new ChipsWithTextfield(msg);
		allowedCORSorigins.setCaption(msg.getMessage("JWTServiceEditorComponent.allowedCORSorigins"));
		main.addComponent(allowedCORSorigins);
		jwtBinder.forField(allowedCORSorigins).bind("allowedCORSorigins");

		CollapsibleLayout corsSection = new CollapsibleLayout(msg.getMessage("JWTServiceEditorComponent.cors"),
				main);
		return corsSection;
	}

	private Component buildJWTSection()
	{
		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		ComboBox<String> credential = new ComboBox<>();
		credential.setId("cre");
		credential.setCaption(msg.getMessage("JWTServiceEditorComponent.signingCredential"));
		credential.setEmptySelectionAllowed(false);
		credential.setItems(credentials);
		jwtBinder.forField(credential).asRequired(msg.getMessage("fieldRequired")).bind("credential");
		main.addComponent(credential);

		TextField ttl = new TextField();
		ttl.setCaption(msg.getMessage("JWTServiceEditorComponent.tokenTTL"));
		jwtBinder.forField(ttl).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToIntegerConverter(msg.getMessage("notAPositiveNumber")))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("ttl");
		main.addComponent(ttl);

		CollapsibleLayout jwtSection = new CollapsibleLayout(msg.getMessage("JWTServiceEditorComponent.jwt"),
				main);
		jwtSection.expand();
		return jwtSection;
	}
}
