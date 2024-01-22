/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.ldap;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import io.imunity.otp.HashFunction;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.TooltipFactory;
import io.imunity.vaadin.elements.grid.EditableGrid;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.ldap.client.config.SearchSpecification;
import pl.edu.icm.unity.ldap.client.config.ServerSpecification;
import pl.edu.icm.unity.ldap.client.config.common.LDAPCommonConfiguration.UserDNResolving;
import pl.edu.icm.unity.ldap.client.config.common.LDAPConnectionProperties.ConnectionMode;
import pl.edu.icm.unity.ldap.client.config.common.LDAPConnectionProperties.SearchScope;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.Set;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


@PrototypeComponent
class OTPWithLDAPAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private final PKIManagement pkiMan;

	private Binder<OTPWithLDAPConfiguration> configBinder;
	private final Set<String> validators;
	private RadioButtonGroup<UserDNResolving> userDNResolvingMode;


	@Autowired
	OTPWithLDAPAuthenticatorEditor(MessageSource msg, PKIManagement pkiMan) throws EngineException
	{
		super(msg);
		this.pkiMan = pkiMan;
		this.validators = pkiMan.getValidatorNames();
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		boolean editMode = init(
				 msg.getMessage("OTPWithLDAPAuthenticatorEditor.defaultName"),
				toEdit, forceNameEditable);
		
		configBinder = new Binder<>(OTPWithLDAPConfiguration.class);

		FormLayout header = buildHeaderSection();
		AccordionPanel ldapHeaderSection = buildLdapHeaderSection();
		ldapHeaderSection.setOpened(true);
		ldapHeaderSection.setWidthFull();
		AccordionPanel otpHeaderSection = buildOtpHeaderSection();
		otpHeaderSection.setOpened(true);
		otpHeaderSection.setWidthFull();

		AccordionPanel userDNresolvingSettings = buildUserDNResolvingSection();
		userDNresolvingSettings.setOpened(true);
		userDNresolvingSettings.setWidthFull();

		AccordionPanel serverConnectionConfiguration = buildServersConnectionConfigurationSection();
		serverConnectionConfiguration.setOpened(true);
		serverConnectionConfiguration.setWidthFull();

		AccordionPanel interactiveLoginSettingsSection = buildInteractiveLoginSettingsSection();
		interactiveLoginSettingsSection.setOpened(true);
		interactiveLoginSettingsSection.setWidthFull();

		OTPWithLDAPConfiguration config = new OTPWithLDAPConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}

		configBinder.setBean(config);

		VerticalLayout mainView = new VerticalLayout();
		mainView.setPadding(false);
		mainView.add(header, ldapHeaderSection, otpHeaderSection, userDNresolvingSettings, serverConnectionConfiguration,
				interactiveLoginSettingsSection);

		return mainView;
	
	}

	private FormLayout buildHeaderSection()
	{
		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));

		return header;
	}

	private AccordionPanel buildOtpHeaderSection()
	{
		FormLayout otp = new FormLayout();
		otp.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		otp.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		Select<Integer> codeLength = new Select<>();
		codeLength.setItems(6, 8);
		configBinder.forField(codeLength).asRequired().bind("codeLength");
		otp.addFormItem(codeLength, msg.getMessage("OTPCredentialDefinitionEditor.codeLength"))
				.add(TooltipFactory.get(msg.getMessage("OTPWithLDAPAuthenticatorEditor.codeLength.tip")));

		IntegerField allowedTimeDrift = new IntegerField();
		allowedTimeDrift.setStepButtonsVisible(true);
		allowedTimeDrift.setMin(0);
		allowedTimeDrift.setMax(2880);
		configBinder.forField(allowedTimeDrift).asRequired().bind("allowedTimeDriftSteps");
		otp.addFormItem(allowedTimeDrift, msg.getMessage("OTPWithLDAPAuthenticatorEditor.allowedTimeDrift"))
				.add(TooltipFactory.get(msg.getMessage("OTPWithLDAPAuthenticatorEditor.allowedTimeDrift.tip")));

		IntegerField timeStep = new IntegerField();
		timeStep.setStepButtonsVisible(true);
		timeStep.setMin(5);
		timeStep.setMax(180);
		configBinder.forField(timeStep).asRequired().bind("timeStepSeconds");
		otp.addFormItem(timeStep, msg.getMessage("OTPWithLDAPAuthenticatorEditor.timeStep"))
				.add(TooltipFactory.get(msg.getMessage("OTPWithLDAPAuthenticatorEditor.timeStep.tip")));

		Select<HashFunction> hashAlgorithm = new Select<>();
		hashAlgorithm.setItemLabelGenerator(item -> msg.getMessage("OTPWithLDAPAuthenticatorEditor.hashAlgorithm." + item));
		hashAlgorithm.setItems(HashFunction.values());
		hashAlgorithm.setValue(HashFunction.SHA1);
		configBinder.forField(hashAlgorithm).asRequired().bind("hashFunction");
		otp.addFormItem(hashAlgorithm, msg.getMessage("OTPWithLDAPAuthenticatorEditor.hashAlgorithm"))
				.add(TooltipFactory.get(msg.getMessage("OTPWithLDAPAuthenticatorEditor.hashAlgorithm.tip")));

		return new AccordionPanel(msg.getMessage("OTPWithLDAPAuthenticatorEditor.otp"),
				otp);
	}
	
	private AccordionPanel buildLdapHeaderSection()
	{
		FormLayout ldap = new FormLayout();
		ldap.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		ldap.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		TextField systemDN = new TextField();
		systemDN.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(systemDN).asRequired().bind("systemDN");
		ldap.addFormItem(systemDN, msg.getMessage("LdapAuthenticatorEditor.systemDN"));

		TextField systemPassword = new TextField();
		systemPassword.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(systemPassword).asRequired()
				.bind("systemPassword");
		ldap.addFormItem(systemPassword, msg.getMessage("LdapAuthenticatorEditor.systemPassword"));

		TextField validUserFilter = new TextField();
		validUserFilter.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(validUserFilter).withValidator((v, c) -> {
			try
			{
				Filter.create(v);
			} catch (LDAPException e)
			{
				return ValidationResult.error(
						msg.getMessage("LdapAuthenticatorEditor.invalidValidUserFilter"));
			}

			return ValidationResult.ok();

		}).bind("validUserFilter");
		ldap.addFormItem(validUserFilter, msg.getMessage("LdapAuthenticatorEditor.validUserFilter"));

		TextField secretAttribute = new TextField();
		configBinder.forField(secretAttribute).asRequired().bind("secretAttribute");
		ldap.addFormItem(secretAttribute, msg.getMessage("OTPWithLDAPAuthenticatorEditor.secretAttribute"));
		
		TextField usernameExtractorRegexp = new TextField();
		usernameExtractorRegexp.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(usernameExtractorRegexp).bind("usernameExtractorRegexp");
		ldap.addFormItem(usernameExtractorRegexp, msg.getMessage("LdapAuthenticatorEditor.usernameExtractorRegexp"));

		return new AccordionPanel(msg.getMessage("OTPWithLDAPAuthenticatorEditor.ldap"),
				ldap);
	}

	private AccordionPanel buildUserDNResolvingSection()
	{
		FormLayout userDNResolvingLayout = new FormLayout();
		userDNResolvingLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		userDNResolvingLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		userDNResolvingMode = new RadioButtonGroup<>();
		userDNResolvingMode.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		userDNResolvingMode.setItemLabelGenerator(
				v -> msg.getMessage("LdapAuthenticatorEditor.userDNResolvingMode." + v.toString()));
		userDNResolvingMode.setItems(UserDNResolving.values());
		configBinder.forField(userDNResolvingMode).bind("userDNResolving");
		userDNResolvingLayout.addFormItem(userDNResolvingMode, "");

		TextField userDNtemplate = new TextField();
		userDNtemplate.setWidth(TEXT_FIELD_BIG.value());
		userDNtemplate.setPlaceholder("uid={USERNAME},dc=myorg,dc=global");

		configBinder.forField(userDNtemplate).withValidator((v, c) -> {

			if (v != null && v.contains("{USERNAME}")
					|| !userDNResolvingMode.getValue().equals(UserDNResolving.template))
			{
				return ValidationResult.ok();
			} else
			{
				return ValidationResult
						.error(msg.getMessage("LdapAuthenticatorEditor.invalidUserDNtemplate"));
			}
		}).bind("userDNTemplate");

		userDNResolvingLayout.addFormItem(userDNtemplate, msg.getMessage("LdapAuthenticatorEditor.userDNtemplate"))
				.add(TooltipFactory.get(msg.getMessage("LdapAuthenticatorEditor.userDNtemplate.desc")));
		
		
		TextField ldapSearchBaseName = new TextField();
		configBinder.forField(ldapSearchBaseName).asRequired(getLdapSearchRequiredValidator())
				.bind("ldapSearchBaseName");
		userDNResolvingLayout.addFormItem(ldapSearchBaseName, msg.getMessage("LdapAuthenticatorEditor.searchSpecification.baseName"));

		TextField ldapSearchFilter = new TextField();
		configBinder.forField(ldapSearchFilter).withValidator((v, c) -> {
			if (userDNResolvingMode.getValue().equals(UserDNResolving.ldapSearch))
			{
				return getFilterValidator().apply(v, c);
			} else
			{

				return ValidationResult.ok();
			}

		}).bind("ldapSearchFilter");
		userDNResolvingLayout.addFormItem(ldapSearchFilter, msg.getMessage("LdapAuthenticatorEditor.searchSpecification.filter"));

		Select<SearchScope> ldapSearchScope = new Select<>();
		ldapSearchScope.setEmptySelectionAllowed(false);
		ldapSearchScope.setItems(SearchScope.values());
		configBinder.forField(ldapSearchScope).bind("ldapSearchScope");
		userDNResolvingLayout.addFormItem(ldapSearchScope, msg.getMessage("LdapAuthenticatorEditor.searchSpecification.scope"));

		userDNResolvingMode.addValueChangeListener(e -> {
			UserDNResolving v = userDNResolvingMode.getValue();
			userDNtemplate.getParent().get().setVisible(v.equals(UserDNResolving.template));
			ldapSearchBaseName.getParent().get().setVisible(v.equals(UserDNResolving.ldapSearch));
			ldapSearchFilter.getParent().get().setVisible(v.equals(UserDNResolving.ldapSearch));
			ldapSearchScope.getParent().get().setVisible(v.equals(UserDNResolving.ldapSearch));
			if (v.equals(UserDNResolving.template))
			{
				ldapSearchBaseName.clear();
				ldapSearchFilter.clear();

			} else
			{
				userDNtemplate.clear();
			}		
		});

		return new AccordionPanel(msg.getMessage("OTPWithLDAPAuthenticatorEditor.userDNResolving"),
				userDNResolvingLayout);
	}

	private AccordionPanel buildServersConnectionConfigurationSection()
	{
		FormLayout serverConnectionLayout = new FormLayout();
		serverConnectionLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		serverConnectionLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		EditableGrid<ServerSpecification> serverConfig = new EditableGrid<>(msg::getMessage, ServerSpecification::new);
		serverConfig.setWidth(TEXT_FIELD_BIG.value());
		serverConfig.setHeight("20em");
		serverConfig.addColumn(ServerSpecification::getServer, ServerSpecification::setServer, true)
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.server"))
				.setAutoWidth(true);
		serverConfig.addIntColumn(ServerSpecification::getPort, ServerSpecification::setPort)
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.port"))
				.setAutoWidth(true);

		configBinder.forField(serverConfig).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			} else
			{
				return ValidationResult.ok();
			}
		}).bind("servers");

		serverConnectionLayout.addFormItem(serverConfig, "");

		Select<ConnectionMode> connectionMode = new Select<>();
		connectionMode.setItems(ConnectionMode.values());
		configBinder.forField(connectionMode).bind("connectionMode");
		serverConnectionLayout.addFormItem(connectionMode, msg.getMessage("LdapAuthenticatorEditor.connectionMode"));

		IntegerField followReferrals = new IntegerField();
		followReferrals.setMin(0);
		configBinder.forField(followReferrals).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind("followReferrals");
		serverConnectionLayout.addFormItem(followReferrals, msg.getMessage("LdapAuthenticatorEditor.followReferrals"));

		IntegerField searchTimeLimit = new IntegerField();
		searchTimeLimit.setMin(0);
		configBinder.forField(searchTimeLimit).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind("searchTimeLimit");
		serverConnectionLayout.addFormItem(searchTimeLimit, msg.getMessage("LdapAuthenticatorEditor.searchTimeLimit"));

		IntegerField socketTimeout = new IntegerField();
		socketTimeout.setMin(0);
		configBinder.forField(socketTimeout).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind("socketTimeout");
		serverConnectionLayout.addFormItem(socketTimeout, msg.getMessage("LdapAuthenticatorEditor.socketTimeout"));

		Checkbox trustAllCerts = new Checkbox(msg.getMessage("LdapAuthenticatorEditor.trustAllCerts"));
		configBinder.forField(trustAllCerts).bind("trustAllCerts");
		serverConnectionLayout.addFormItem(trustAllCerts, "");
		
		ComboBox<String> clientTrustStore = new ComboBox<>();
		clientTrustStore.setItems(validators);
		configBinder.forField(clientTrustStore).bind("clientTrustStore");
		serverConnectionLayout.addFormItem(clientTrustStore, msg.getMessage("LdapAuthenticatorEditor.clientTrustStore"));

		trustAllCerts.addValueChangeListener(e -> {
			clientTrustStore.setEnabled(!e.getValue());
		});		
		
		IntegerField resultEntriesLimit = new IntegerField();
		resultEntriesLimit.setMin(0);
		configBinder.forField(resultEntriesLimit).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind("resultEntriesLimit");
		serverConnectionLayout.addFormItem(resultEntriesLimit, msg.getMessage("LdapAuthenticatorEditor.resultEntriesLimit"));

		return new AccordionPanel(msg.getMessage("OTPWithLDAPAuthenticatorEditor.serverConnectionConfiguration"),
				serverConnectionLayout);
	}

	private AccordionPanel buildInteractiveLoginSettingsSection()
	{
		FormLayout interactiveLoginSettings = new FormLayout();
		interactiveLoginSettings.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		interactiveLoginSettings.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		
		LocalizedTextFieldDetails retrievalName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		retrievalName.setWidth(TEXT_FIELD_MEDIUM.value());
		configBinder.forField(retrievalName)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind(OTPWithLDAPConfiguration::getRetrievalName, OTPWithLDAPConfiguration::setRetrievalName);
		
		interactiveLoginSettings.addFormItem(retrievalName, msg.getMessage("OTPWithLDAPAuthenticatorEditor.displayedName"));
		return new AccordionPanel(
				msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
	}
	
	private Validator<String> getLdapSearchRequiredValidator()
	{
		return (v, c) -> {
			if (userDNResolvingMode.getValue().equals(UserDNResolving.ldapSearch)
					&& (v == null || v.isEmpty()))
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			} else
			{
				return ValidationResult.ok();
			}
		};
	}
	
	private Validator<String> getFilterValidator()
	{
		return (v, c) -> {

			try
			{
				SearchSpecification.createFilter(v, "test");
			} catch (LDAPException e)
			{
				return ValidationResult.error(msg.getMessage(
						"LdapAuthenticatorEditor.searchSpecification.invalidFilter"));
			}

			return ValidationResult.ok();
		};
	}
	
	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{

		return new AuthenticatorDefinition(getName(), OTPWithLDAPVerificator.NAME, getConfiguration(), null);
	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
		{
			throw new FormValidationException();
		}

		OTPWithLDAPConfiguration conf = configBinder.getBean();
		try
		{
			conf.validateConfiguration(pkiMan);
		} catch (Exception e)
		{

			throw new FormValidationException("Invalid ldap authenticator configuration", e);
		}

		return conf.toProperties(msg);
	}
	@org.springframework.stereotype.Component
	static class OTPWithLDAPEditorFactory implements AuthenticatorEditorFactory
	{
		private ObjectFactory<OTPWithLDAPAuthenticatorEditor> factory;

		@Autowired
		OTPWithLDAPEditorFactory(ObjectFactory<OTPWithLDAPAuthenticatorEditor> factory)
		{
			this.factory = factory;
		}

		@Override
		public String getSupportedAuthenticatorType()
		{
			return OTPWithLDAPVerificator.NAME;
		}

		@Override
		public AuthenticatorEditor createInstance() throws EngineException
		{
			return factory.getObject();
		}
	}
}
