/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorBase.EditorTab;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import io.imunity.vaadin.endpoint.common.api.services.tabs.WebServiceAuthnScreenLayoutEditor;
import io.imunity.vaadin.endpoint.common.file.FileField;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

class AuthenticationOptionsTab extends VerticalLayout implements EditorTab
{
	private final MessageSource msg;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final Supplier<Set<String>> authnOptionSupplier;
	private final UnityServerConfiguration serverConfig;
	private WebServiceAuthnScreenLayoutEditor webScreenEditor;
	private Binder<AttrIntrospectionAuthnScreenConfiguration> authnScreenConfigBinder;

	AuthenticationOptionsTab(MessageSource msg, UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService, Supplier<Set<String>> authnOptionSupplier)
	{
		this.msg = msg;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.authnOptionSupplier = authnOptionSupplier;

	}

	void initUI(Binder<AttrIntrospectionAuthnScreenConfiguration> authnScreenConfigBinder)
	{
		this.authnScreenConfigBinder = authnScreenConfigBinder;
		add(buildScreenLayoutSection());
		setMargin(false);
	}

	private Component buildScreenLayoutSection()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setPadding(false);

		FormLayout wrapper = new FormLayout();
		wrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		wrapper.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		FileField logo = new FileField(msg, "image/*", "logo.jpg", serverConfig.getFileSizeLimit());
		logo.configureBinding(authnScreenConfigBinder, "logo");
		wrapper.addFormItem(logo, msg.getMessage("AuthenticationOptionsTab.logo"));

		LocalizedTextFieldDetails title = new LocalizedTextFieldDetails(msg.getEnabledLocales()
				.values(), msg.getLocale());
		title.setWidth(TEXT_FIELD_MEDIUM.value());
		authnScreenConfigBinder.forField(title)
				.withConverter(I18nString::new, v -> v != null ? v.getLocalizedMap() : new HashMap<>())
				.bind("title");

		wrapper.addFormItem(title, msg.getMessage("AuthenticationOptionsTab.title"));

		Checkbox enableSearch = new Checkbox();
		enableSearch.setLabel(msg.getMessage("AuthenticationOptionsTab.enableSearch"));
		authnScreenConfigBinder.forField(enableSearch)
				.bind("enableSearch");
		wrapper.addFormItem(enableSearch, "");
		main.add(wrapper);

		webScreenEditor = new WebServiceAuthnScreenLayoutEditor(msg, authenticatorSupportService, authnOptionSupplier);
		webScreenEditor.setRegistrationEnabled(false);
		webScreenEditor.setLastUsedOptionEnabled(false);
		webScreenEditor.setAddColumnEnabled(false);
		webScreenEditor.configureBinding(authnScreenConfigBinder, "authnLayoutConfiguration");
		main.add(webScreenEditor);
		return main;
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.AUTHENTICATION.toString();
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.SIGN_IN;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("AuthenticationOptionsTab.authnenticationOptions");
	}
}
