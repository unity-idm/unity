/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.tabs;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Functions;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;

import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.services.DefaultServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorBase.EditorTab;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ServiceWebConfiguration;
import io.imunity.vaadin.endpoint.common.file.FileField;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;


/**
 * Service web authentication editor tab
 * 
 * @author P.Piernik
 *
 */
public class WebServiceAuthenticationTab extends VerticalLayout implements EditorTab
{
	private final MessageSource msg;
	private final UnityServerConfiguration serverConfiguration;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final List<String> registrationForms;
	private final List<String> allRealms;
	private final List<String> flows;
	
	private AccordionPanel layoutForRetUserSection;
	private List<String> authenticators;
	private WebServiceAuthnScreenLayoutEditor webScreenEditor;
	private WebServiceReturningLayoutEditor webRetUserScreenEditor;
	private Binder<DefaultServiceDefinition> binder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private GroupedValuesChipsWithDropdown authAndFlows;

	public WebServiceAuthenticationTab(MessageSource msg, 
			UnityServerConfiguration serverConfig, AuthenticatorSupportService authenticatorSupportService,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators, List<String> allRealms,
			List<String> registrationForms, String binding, String caption)
	{
		this(msg, serverConfig, authenticatorSupportService, flows, authenticators, allRealms,
				registrationForms, binding);
	}

	public WebServiceAuthenticationTab(MessageSource msg,
			UnityServerConfiguration serverConfig, AuthenticatorSupportService authenticatorSupportService,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators, List<String> allRealms,
			List<String> registrationForms, String binding)

	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.registrationForms = registrationForms;
		this.serverConfiguration = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.flows = filterBindingCompatibleAuthenticationFlow(flows, authenticators, binding);
		this.authenticators = authenticators.stream()
				.filter(a -> a.getSupportedBindings()
						.contains(binding))
				.map(a -> a.getId())
				.collect(Collectors.toList());
	}

	public static List<String> filterBindingCompatibleAuthenticationFlow(List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, String binding)
	{
		List<String> filteredFlows = new ArrayList<>();
		Map<String, AuthenticatorInfo> authenticatorsMap = authenticators.stream()
				.collect(Collectors.toMap(AuthenticatorInfo::getId, Functions.identity()));

		for (AuthenticationFlowDefinition f : flows)
		{
			boolean supportsBinding = true;
			for (String authenticatorName : f.getAllAuthenticators())
			{
				if (!authenticatorsMap.get(authenticatorName)
						.getSupportedBindings()
						.contains(binding))
				{
					supportsBinding = false;
					break;
				}
			}
			if (supportsBinding)
				filteredFlows.add(f.getName());
		}
		return filteredFlows;
	}

	public void initUI(Binder<DefaultServiceDefinition> binder, Binder<ServiceWebConfiguration> webConfigBinder)
	{
		this.binder = binder;
		this.webConfigBinder = webConfigBinder;

		add(buildMainSection());
		add(buildRegistrationSection());
		add(buildPresentationSection());
		add(buildScreenLayoutSection());
		add(buildLayoutForReturningUserSection());
	}

	private Component buildMainSection()
	{
		FormLayout mainAuthenticationLayout = new FormLayout();
		mainAuthenticationLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		mainAuthenticationLayout.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());

		ComboBox<String> realm = new ComboBox<>();
		realm.setItems(allRealms);
		binder.forField(realm)
				.asRequired()
				.bind("realm");
		mainAuthenticationLayout.addFormItem(realm, msg.getMessage("ServiceEditorBase.realm"));

		Map<String, List<String>> labels = new HashMap<>();
		labels.put(msg.getMessage("ServiceEditorBase.flows"), flows);
		labels.put(msg.getMessage("ServiceEditorBase.authenticators"), authenticators);
		authAndFlows = new GroupedValuesChipsWithDropdown(labels);
		authAndFlows.setWidth(50, Unit.EM);	
//		authAndFlows.setOverlayClassName(CssClassNames.HIDDEN_COMBO_CHECKMARK.getName());
		binder.forField(authAndFlows)
		.withConverter(List::copyOf, l -> new HashSet<>(l == null ? new ArrayList<>() : l))
		.withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();

		}).bind("authenticationOptions");
		authAndFlows.setRequiredIndicatorVisible(true);
		mainAuthenticationLayout.addFormItem(authAndFlows, msg.getMessage("ServiceEditorBase.authenticatorsAndFlows"));

		Checkbox showSearch = new Checkbox();
		showSearch.setLabel(msg.getMessage("WebServiceEditorBase.showSearch"));
		webConfigBinder.forField(showSearch)
				.bind("showSearch");
		mainAuthenticationLayout.addFormItem(showSearch, "");

		Checkbox addAllAuthnOptions = new Checkbox();
		addAllAuthnOptions.setLabel(msg.getMessage("WebServiceEditorBase.addAllAuthnOptions"));
		webConfigBinder.forField(addAllAuthnOptions)
				.bind("addAllAuthnOptions");
		mainAuthenticationLayout.addFormItem(addAllAuthnOptions, "");

		Checkbox showCancel = new Checkbox();
		showCancel.setLabel(msg.getMessage("WebServiceEditorBase.showCancel"));
		webConfigBinder.forField(showCancel)
				.bind("showCancel");
		mainAuthenticationLayout.addFormItem(showCancel, "");

		Checkbox showLastUsedAuthnOption = new Checkbox();
		showLastUsedAuthnOption.setLabel(msg.getMessage("WebServiceEditorBase.showLastUsedAuthnOption"));
		showLastUsedAuthnOption.addValueChangeListener(e ->
		{
			layoutForRetUserSection.setVisible(e.getValue());
		});
		mainAuthenticationLayout.addFormItem(showLastUsedAuthnOption, "");
		webConfigBinder.forField(showLastUsedAuthnOption)
				.bind("showLastUsedAuthnOption");

		Checkbox autoLogin = new Checkbox();
		autoLogin.setLabel(msg.getMessage("WebServiceEditorBase.autoLogin"));
		webConfigBinder.forField(autoLogin)
				.bind("autoLogin");
		mainAuthenticationLayout.addFormItem(autoLogin, "");
		return mainAuthenticationLayout;
	}

	private Component buildRegistrationSection()
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());

		Checkbox enableRegistration = new Checkbox();
		enableRegistration.setLabel(msg.getMessage("WebServiceEditorBase.enableRegistration"));
		webConfigBinder.forField(enableRegistration)
				.bind("enableRegistration");
		main.addFormItem(enableRegistration, "");

		Checkbox showRegistrationFormsInHeader = new Checkbox();
		showRegistrationFormsInHeader.setLabel(msg.getMessage("WebServiceEditorBase.showRegistrationFormsInHeader"));
		showRegistrationFormsInHeader.setEnabled(false);
		webConfigBinder.forField(showRegistrationFormsInHeader)
				.bind("showRegistrationFormsInHeader");
		main.addFormItem(showRegistrationFormsInHeader, "");

		TextField externalRegistrationURL = new TextField();
		externalRegistrationURL.setEnabled(false);
		externalRegistrationURL.setWidth(CSSVars.TEXT_FIELD_BIG.value());
		webConfigBinder.forField(externalRegistrationURL)
				.bind("externalRegistrationURL");
		main.addFormItem(externalRegistrationURL, msg.getMessage("WebServiceEditorBase.externalRegistrationURL"));

		MultiSelectComboBox<String> regFormsCombo = new MultiSelectComboBox<>();
		regFormsCombo.setEnabled(false);
		regFormsCombo.setItems(registrationForms);
		webConfigBinder.forField(regFormsCombo)
				.withConverter(List::copyOf, HashSet::new)
				.bind("registrationForms");
		main.addFormItem(regFormsCombo, msg.getMessage("WebServiceEditorBase.registrationForms"));

		enableRegistration.addValueChangeListener(e ->
		{
			boolean v = e.getValue();
			showRegistrationFormsInHeader.setEnabled(v);
			externalRegistrationURL.setEnabled(v);
			regFormsCombo.setEnabled(v);
		});

		AccordionPanel regSection = new AccordionPanel(msg.getMessage("WebServiceEditorBase.usersRegistration"), main);
		regSection.setOpened(true);
		return regSection;
	}

	private Component buildPresentationSection()
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());

		FileField logo = new FileField(msg, "image/*", "logo.jpg", serverConfiguration.getFileSizeLimit());		
		logo.configureBinding(webConfigBinder, "logo");
		main.addFormItem(logo, msg.getMessage("WebServiceEditorBase.logo"));
		
		LocalizedTextFieldDetails title = new LocalizedTextFieldDetails(msg.getEnabledLocales()
				.values(), msg.getLocale());
		webConfigBinder.forField(title)
				.withConverter(I18nString::new, v -> v != null ? v.getLocalizedMap() : new HashMap<>())
				.bind("title");
		main.addFormItem(title, msg.getMessage("WebServiceEditorBase.title"));
	
		Checkbox compactCredentialReset = new Checkbox();
		compactCredentialReset.setLabel(msg.getMessage("WebServiceEditorBase.compactCredentialReset"));
		webConfigBinder.forField(compactCredentialReset)
				.bind("compactCredentialReset");
		main.addFormItem(compactCredentialReset, "");

		
		AccordionPanel presentationSection = new AccordionPanel(msg.getMessage("WebServiceEditorBase.presentation"), main);
		presentationSection.setOpened(true);
		return presentationSection;
	}

	private Component buildScreenLayoutSection()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);

		webScreenEditor = new WebServiceAuthnScreenLayoutEditor(msg, authenticatorSupportService,
				() -> authAndFlows.getSelectedValue());
		authAndFlows.addValueChangeListener(e -> webScreenEditor.refreshColumnsElements());
		webScreenEditor.configureBinding(webConfigBinder, "authenticationLayoutConfiguration");
		main.add(webScreenEditor);
	
		AccordionPanel mainLayoutSection = new AccordionPanel(msg.getMessage("WebServiceEditorBase.mainLayout"), main);
		mainLayoutSection.setOpened(true);
		mainLayoutSection.setWidthFull();
		return mainLayoutSection;
	}

	private Component buildLayoutForReturningUserSection()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);

		webRetUserScreenEditor = new WebServiceReturningLayoutEditor(msg);
		main.add(webRetUserScreenEditor);
		webConfigBinder.forField(webRetUserScreenEditor)
				.bind("retUserLayoutConfiguration");

		layoutForRetUserSection = new AccordionPanel(msg.getMessage("WebServiceEditorBase.layoutForReturningUser"), main);
		layoutForRetUserSection.setOpened(true);
		layoutForRetUserSection.setVisible(false);
		layoutForRetUserSection.setWidthFull();

		return layoutForRetUserSection;
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.AUTHENTICATION.toString();
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.SIGN_IN;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("ServiceEditorBase.authentication");
	}
}
