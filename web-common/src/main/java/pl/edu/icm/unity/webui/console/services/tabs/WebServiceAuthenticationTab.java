/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Functions;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.chips.GroupedValuesChipsWithDropdown;
import pl.edu.icm.unity.webui.common.file.ImageField;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.console.services.authnlayout.ServiceWebConfiguration;

/**
 * Service web authentication editor tab
 * 
 * @author P.Piernik
 *
 */
public class WebServiceAuthenticationTab extends CustomComponent implements EditorTab
{
	private MessageSource msg;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfiguration;
	private AuthenticatorSupportService authenticatorSupportService;

	private List<String> registrationForms;
	private WebServiceAuthnScreenLayoutEditor webScreenEditor;
	private WebServiceReturningLayoutEditor webRetUserScreenEditor;
	private CollapsibleLayout layoutForRetUserSection;
	private List<String> allRealms;
	private List<String> flows;
	private List<String> authenticators;

	private Binder<DefaultServiceDefinition> binder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private GroupedValuesChipsWithDropdown authAndFlows;

	public WebServiceAuthenticationTab(MessageSource msg, URIAccessService uriAccessService,
			UnityServerConfiguration serverConfig, AuthenticatorSupportService authenticatorSupportService,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> allRealms, List<String> registrationForms, String binding, String caption)
	{
		this(msg, uriAccessService, serverConfig, authenticatorSupportService, flows, authenticators, allRealms,
				registrationForms, binding);
		setCaption(caption);
	}

	public WebServiceAuthenticationTab(MessageSource msg, URIAccessService uriAccessService,
			UnityServerConfiguration serverConfig, AuthenticatorSupportService authenticatorSupportService,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> allRealms, List<String> registrationForms, String binding)

	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.registrationForms = registrationForms;
		this.uriAccessService = uriAccessService;
		this.serverConfiguration = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.flows = filterBindingCompatibleAuthenticationFlow(flows, authenticators, binding);
		this.authenticators = authenticators.stream().filter(a -> a.getSupportedBindings().contains(binding))
				.map(a -> a.getId()).collect(Collectors.toList());
		setCaption(msg.getMessage("ServiceEditorBase.authentication"));
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
				if (!authenticatorsMap.get(authenticatorName).getSupportedBindings().contains(binding))
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

		setIcon(Images.sign_in.getResource());
		VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.addComponent(buildMainSection());
		mainWrapper.addComponent(buildRegistrationSection());
		mainWrapper.addComponent(buildPresentationSection());
		mainWrapper.addComponent(buildScreenLayoutSection());
		mainWrapper.addComponent(buildLayoutForReturningUserSection());

		setCompositionRoot(mainWrapper);
	}

	private Component buildMainSection()
	{
		FormLayoutWithFixedCaptionWidth mainAuthenticationLayout = new FormLayoutWithFixedCaptionWidth();
		ComboBox<String> realm = new ComboBox<>();
		realm.setCaption(msg.getMessage("ServiceEditorBase.realm"));
		realm.setItems(allRealms);
		realm.setEmptySelectionAllowed(false);
		binder.forField(realm).asRequired().bind("realm");
		mainAuthenticationLayout.addComponent(realm);

		Map<String, List<String>> labels = new HashMap<>();
		labels.put(msg.getMessage("ServiceEditorBase.flows"), flows);
		labels.put(msg.getMessage("ServiceEditorBase.authenticators"), authenticators);
		authAndFlows = new GroupedValuesChipsWithDropdown(labels);
		authAndFlows.setCaption(msg.getMessage("ServiceEditorBase.authenticatorsAndFlows"));
		binder.forField(authAndFlows).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();

		}).bind("authenticationOptions");
		authAndFlows.setRequiredIndicatorVisible(true);
		mainAuthenticationLayout.addComponent(authAndFlows);

		CheckBox showSearch = new CheckBox();
		showSearch.setCaption(msg.getMessage("WebServiceEditorBase.showSearch"));
		webConfigBinder.forField(showSearch).bind("showSearch");
		mainAuthenticationLayout.addComponent(showSearch);

		CheckBox addAllAuthnOptions = new CheckBox();
		addAllAuthnOptions.setCaption(msg.getMessage("WebServiceEditorBase.addAllAuthnOptions"));
		webConfigBinder.forField(addAllAuthnOptions).bind("addAllAuthnOptions");
		mainAuthenticationLayout.addComponent(addAllAuthnOptions);

		CheckBox showCancel = new CheckBox();
		showCancel.setCaption(msg.getMessage("WebServiceEditorBase.showCancel"));
		webConfigBinder.forField(showCancel).bind("showCancel");
		mainAuthenticationLayout.addComponent(showCancel);

		CheckBox showLastUsedAuthnOption = new CheckBox();
		showLastUsedAuthnOption.setCaption(msg.getMessage("WebServiceEditorBase.showLastUsedAuthnOption"));
		showLastUsedAuthnOption.addValueChangeListener(e -> {
			layoutForRetUserSection.setVisible(e.getValue());
		});
		mainAuthenticationLayout.addComponent(showLastUsedAuthnOption);
		webConfigBinder.forField(showLastUsedAuthnOption).bind("showLastUsedAuthnOption");

		CheckBox autoLogin = new CheckBox();
		autoLogin.setCaption(msg.getMessage("WebServiceEditorBase.autoLogin"));
		webConfigBinder.forField(autoLogin).bind("autoLogin");
		mainAuthenticationLayout.addComponent(autoLogin);
		return mainAuthenticationLayout;
	}

	private Component buildRegistrationSection()
	{
		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		CheckBox enableRegistration = new CheckBox();
		enableRegistration.setCaption(msg.getMessage("WebServiceEditorBase.enableRegistration"));
		webConfigBinder.forField(enableRegistration).bind("enableRegistration");
		main.addComponent(enableRegistration);

		CheckBox showRegistrationFormsInHeader = new CheckBox();
		showRegistrationFormsInHeader
				.setCaption(msg.getMessage("WebServiceEditorBase.showRegistrationFormsInHeader"));
		showRegistrationFormsInHeader.setEnabled(false);
		webConfigBinder.forField(showRegistrationFormsInHeader).bind("showRegistrationFormsInHeader");
		main.addComponent(showRegistrationFormsInHeader);

		TextField externalRegistrationURL = new TextField();
		externalRegistrationURL.setEnabled(false);
		externalRegistrationURL.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH,
				FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		externalRegistrationURL.setCaption(msg.getMessage("WebServiceEditorBase.externalRegistrationURL"));
		webConfigBinder.forField(externalRegistrationURL).bind("externalRegistrationURL");
		main.addComponent(externalRegistrationURL);

		ChipsWithDropdown<String> regFormsCombo = new ChipsWithDropdown<>();
		regFormsCombo.setEnabled(false);
		regFormsCombo.setCaption(msg.getMessage("WebServiceEditorBase.registrationForms"));
		regFormsCombo.setItems(registrationForms);
		webConfigBinder.forField(regFormsCombo).bind("registrationForms");
		main.addComponent(regFormsCombo);

		enableRegistration.addValueChangeListener(e -> {
			boolean v = e.getValue();
			showRegistrationFormsInHeader.setEnabled(v);
			externalRegistrationURL.setEnabled(v);
			regFormsCombo.setEnabled(v);
		});

		CollapsibleLayout regSection = new CollapsibleLayout(
				msg.getMessage("WebServiceEditorBase.usersRegistration"), main);
		regSection.expand();
		return regSection;
	}

	private Component buildPresentationSection()
	{
		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		ImageField logo = new ImageField(msg, uriAccessService, serverConfiguration.getFileSizeLimit());
		logo.setCaption(msg.getMessage("WebServiceEditorBase.logo"));
		logo.configureBinding(webConfigBinder, "logo");
		main.addComponent(logo);

		I18nTextField title = new I18nTextField(msg);
		title.setCaption(msg.getMessage("WebServiceEditorBase.title"));
		webConfigBinder.forField(title).bind("title");
		main.addComponent(title);

		CheckBox compactCredentialReset = new CheckBox();
		compactCredentialReset.setCaption(msg.getMessage("WebServiceEditorBase.compactCredentialReset"));
		webConfigBinder.forField(compactCredentialReset).bind("compactCredentialReset");
		main.addComponent(compactCredentialReset);
		
		CollapsibleLayout presentationSection = new CollapsibleLayout(
				msg.getMessage("WebServiceEditorBase.presentation"), main);
		presentationSection.expand();
		return presentationSection;
	}

	private Component buildScreenLayoutSection()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);

		webScreenEditor = new WebServiceAuthnScreenLayoutEditor(msg, authenticatorSupportService,
				() -> authAndFlows.getSelectedItems());
		authAndFlows.addValueChangeListener(e -> webScreenEditor.refreshColumnsElements());
		webScreenEditor.configureBinding(webConfigBinder, "authenticationLayoutConfiguration");
		main.addComponent(webScreenEditor);
		CollapsibleLayout mainLayoutSection = new CollapsibleLayout(
				msg.getMessage("WebServiceEditorBase.mainLayout"), main);
		mainLayoutSection.expand();
		return mainLayoutSection;
	}

	private Component buildLayoutForReturningUserSection()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);

		webRetUserScreenEditor = new WebServiceReturningLayoutEditor(msg);
		main.addComponent(webRetUserScreenEditor);
		webConfigBinder.forField(webRetUserScreenEditor).bind("retUserLayoutConfiguration");

		layoutForRetUserSection = new CollapsibleLayout(
				msg.getMessage("WebServiceEditorBase.layoutForReturningUser"), main);
		layoutForRetUserSection.expand();
		layoutForRetUserSection.setVisible(false);

		return layoutForRetUserSection;
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.AUTHENTICATION.toString();
	}

	@Override
	public CustomComponent getComponent()
	{
		return this;
	}
}
