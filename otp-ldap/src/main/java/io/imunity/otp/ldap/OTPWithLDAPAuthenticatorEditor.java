/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.ldap;

import static io.imunity.tooltip.TooltipExtension.tooltip;

import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.risto.stepper.IntStepper;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.otp.HashFunction;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.ldap.client.config.common.LDAPCommonConfiguration.UserDNResolving;
import pl.edu.icm.unity.ldap.client.config.common.LDAPConnectionProperties.ConnectionMode;
import pl.edu.icm.unity.ldap.client.config.common.LDAPConnectionProperties.SearchScope;
import pl.edu.icm.unity.ldap.client.config.SearchSpecification;
import pl.edu.icm.unity.ldap.client.config.ServerSpecification;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithEditor;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

@PrototypeComponent
class OTPWithLDAPAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private final PKIManagement pkiMan;
	
	private Binder<OTPWithLDAPConfiguration> configBinder;
	private Set<String> validators;
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

		FormLayoutWithFixedCaptionWidth header = buildHeaderSection();
		CollapsibleLayout ldapHeaderSection = buildLdapHeaderSection();
		ldapHeaderSection.expand();
		CollapsibleLayout otpHeaderSection = buildOtpHeaderSection();
		otpHeaderSection.expand();
		
		CollapsibleLayout userDNresolvingSettings = buildUserDNResolvingSection();
		userDNresolvingSettings.expand();

		CollapsibleLayout serverConnectionConfiguration = buildServersConnectionConfigurationSection();
		serverConnectionConfiguration.expand();
		
		CollapsibleLayout interactiveLoginSettingsSection = buildInteractiveLoginSettingsSection();

		OTPWithLDAPConfiguration config = new OTPWithLDAPConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}

		configBinder.setBean(config);

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(otpHeaderSection);
		mainView.addComponent(ldapHeaderSection);
		mainView.addComponent(userDNresolvingSettings);
		mainView.addComponent(serverConnectionConfiguration);
		mainView.addComponent(interactiveLoginSettingsSection);
		
		return mainView;
	
	}

	private FormLayoutWithFixedCaptionWidth buildHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);

		return header;
	}

	private CollapsibleLayout buildOtpHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth otp = new FormLayoutWithFixedCaptionWidth();
		
		ComboBox<Integer> codeLength = new ComboBox<>(msg.getMessage("OTPCredentialDefinitionEditor.codeLength"));
		tooltip(codeLength, msg.getMessage("OTPWithLDAPAuthenticatorEditor.codeLength.tip"));
		codeLength.setItems(6, 8);
		codeLength.setEmptySelectionAllowed(false);
		configBinder.forField(codeLength).asRequired().bind("codeLength");
		otp.addComponent(codeLength);

		IntStepper allowedTimeDrift = new IntStepper(msg.getMessage("OTPWithLDAPAuthenticatorEditor.allowedTimeDrift"));
		tooltip(allowedTimeDrift, msg.getMessage("OTPWithLDAPAuthenticatorEditor.allowedTimeDrift.tip"));
		allowedTimeDrift.setWidth(3, Unit.EM);
		allowedTimeDrift.setMinValue(0);
		allowedTimeDrift.setMaxValue(2880);
		configBinder.forField(allowedTimeDrift).asRequired().bind("allowedTimeDriftSteps");
		otp.addComponent(allowedTimeDrift);		
		
		IntStepper timeStep = new IntStepper(msg.getMessage("OTPWithLDAPAuthenticatorEditor.timeStep"));
		tooltip(timeStep, msg.getMessage("OTPWithLDAPAuthenticatorEditor.timeStep.tip"));
		timeStep.setWidth(3, Unit.EM);
		timeStep.setMinValue(5);
		timeStep.setMaxValue(180);
		configBinder.forField(timeStep).asRequired().bind("timeStepSeconds");
		otp.addComponent(timeStep);		

		EnumComboBox<HashFunction> hashAlgorithm = new EnumComboBox<>(
				msg.getMessage("OTPWithLDAPAuthenticatorEditor.hashAlgorithm"), 
				msg, "OTPWithLDAPAuthenticatorEditor.hashAlgorithm.", HashFunction.class, HashFunction.SHA1);
		tooltip(hashAlgorithm, msg.getMessage("OTPWithLDAPAuthenticatorEditor.hashAlgorithm.tip"));
		configBinder.forField(hashAlgorithm).asRequired().bind("hashFunction");
		otp.addComponent(hashAlgorithm);		

		return new CollapsibleLayout(msg.getMessage("OTPWithLDAPAuthenticatorEditor.otp"),
				otp);
	}
	
	private CollapsibleLayout buildLdapHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth ldap = new FormLayoutWithFixedCaptionWidth();
	
		TextField systemDN = new TextField(msg.getMessage("LdapAuthenticatorEditor.systemDN"));
		systemDN.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(systemDN).asRequired().bind("systemDN");
		ldap.addComponent(systemDN);

		TextField systemPassword = new TextField(msg.getMessage("LdapAuthenticatorEditor.systemPassword"));
		systemPassword.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(systemPassword).asRequired()
				.bind("systemPassword");
		ldap.addComponent(systemPassword);

		TextField validUserFilter = new TextField(msg.getMessage("LdapAuthenticatorEditor.validUserFilter"));
		validUserFilter.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
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
		ldap.addComponent(validUserFilter);

		TextField secretAttribute = new TextField(msg.getMessage("OTPWithLDAPAuthenticatorEditor.secretAttribute"));
		configBinder.forField(secretAttribute).asRequired().bind("secretAttribute");
		ldap.addComponent(secretAttribute);
		
		TextField usernameExtractorRegexp = new TextField(
				msg.getMessage("LdapAuthenticatorEditor.usernameExtractorRegexp"));
		usernameExtractorRegexp.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(usernameExtractorRegexp).bind("usernameExtractorRegexp");
		ldap.addComponent(usernameExtractorRegexp);

		return new CollapsibleLayout(msg.getMessage("OTPWithLDAPAuthenticatorEditor.ldap"),
				ldap);
	}

	private CollapsibleLayout buildUserDNResolvingSection()
	{
		FormLayoutWithFixedCaptionWidth userDNResolvingLayout = new FormLayoutWithFixedCaptionWidth();
		userDNResolvingLayout.setMargin(false);

		userDNResolvingMode = new RadioButtonGroup<>();
		userDNResolvingMode.setItemCaptionGenerator(
				v -> msg.getMessage("LdapAuthenticatorEditor.userDNResolvingMode." + v.toString()));
		userDNResolvingMode.setItems(UserDNResolving.values());
		configBinder.forField(userDNResolvingMode).bind("userDNResolving");
		userDNResolvingLayout.addComponent(userDNResolvingMode);

		TextField userDNtemplate = new TextField(msg.getMessage("LdapAuthenticatorEditor.userDNtemplate"));
		userDNtemplate.setPlaceholder("uid={USERNAME},dc=myorg,dc=global");
		userDNtemplate.setDescription(msg.getMessage("LdapAuthenticatorEditor.userDNtemplate.desc"));
		
		userDNtemplate.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
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

		userDNResolvingLayout.addComponent(userDNtemplate);
		
		
		TextField ldapSearchBaseName = new TextField(
				msg.getMessage("LdapAuthenticatorEditor.searchSpecification.baseName"));
		ldapSearchBaseName.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(ldapSearchBaseName).asRequired(getLdapSearchRequiredValidator())
				.bind("ldapSearchBaseName");
		userDNResolvingLayout.addComponent(ldapSearchBaseName);

		TextField ldapSearchFilter = new TextField(
				msg.getMessage("LdapAuthenticatorEditor.searchSpecification.filter"));
		ldapSearchFilter.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(ldapSearchFilter).withValidator((v, c) -> {
			if (userDNResolvingMode.getValue().equals(UserDNResolving.ldapSearch))
			{
				return getFilterValidator().apply(v, c);
			} else
			{

				return ValidationResult.ok();
			}

		}).bind("ldapSearchFilter");
		userDNResolvingLayout.addComponent(ldapSearchFilter);

		ComboBox<SearchScope> ldapSearchScope = new ComboBox<>(
				msg.getMessage("LdapAuthenticatorEditor.searchSpecification.scope"));
		ldapSearchScope.setEmptySelectionAllowed(false);
		ldapSearchScope.setItems(SearchScope.values());
		configBinder.forField(ldapSearchScope).bind("ldapSearchScope");
		userDNResolvingLayout.addComponent(ldapSearchScope);

		userDNResolvingMode.addValueChangeListener(e -> {
			UserDNResolving v = userDNResolvingMode.getValue();
			userDNtemplate.setVisible(v.equals(UserDNResolving.template));
			ldapSearchBaseName.setVisible(v.equals(UserDNResolving.ldapSearch));
			ldapSearchFilter.setVisible(v.equals(UserDNResolving.ldapSearch));
			ldapSearchScope.setVisible(v.equals(UserDNResolving.ldapSearch));
			if (v.equals(UserDNResolving.template))
			{
				ldapSearchBaseName.clear();
				ldapSearchFilter.clear();

			} else
			{
				userDNtemplate.clear();
			}		
		});

		return new CollapsibleLayout(msg.getMessage("OTPWithLDAPAuthenticatorEditor.userDNResolving"),
				userDNResolvingLayout);
	}

	private CollapsibleLayout buildServersConnectionConfigurationSection()
	{
		FormLayoutWithFixedCaptionWidth serverConnectionLayout = new FormLayoutWithFixedCaptionWidth();
		serverConnectionLayout.setMargin(false);

		GridWithEditor<ServerSpecification> serverConfig = new GridWithEditor<>(msg, ServerSpecification.class);
		serverConnectionLayout.addComponent(serverConfig);
		serverConfig.addTextColumn(s -> s.getServer(), (t, v) -> t.setServer(v),
				msg.getMessage("LdapAuthenticatorEditor.server"), 40, true);

		serverConfig.addIntColumn(s -> s.getPort(), (t, v) -> t.setPort(v),
				msg.getMessage("LdapAuthenticatorEditor.port"), 10,
				Optional.of(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, 65535)));

		serverConfig.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(serverConfig).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			} else
			{
				return ValidationResult.ok();
			}
		}).bind("servers");

		ComboBox<ConnectionMode> connectionMode = new ComboBox<>(
				msg.getMessage("LdapAuthenticatorEditor.connectionMode"));
		connectionMode.setItems(ConnectionMode.values());
		connectionMode.setEmptySelectionAllowed(false);
		configBinder.forField(connectionMode).bind("connectionMode");
		serverConnectionLayout.addComponent(connectionMode);

		TextField followReferrals = new TextField(msg.getMessage("LdapAuthenticatorEditor.followReferrals"));
		configBinder.forField(followReferrals).asRequired(msg.getMessage("notAPositiveNumber"))
				.withConverter(new StringToIntegerConverter(msg.getMessage("notAPositiveNumber")))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("followReferrals");
		serverConnectionLayout.addComponent(followReferrals);

		TextField searchTimeLimit = new TextField(msg.getMessage("LdapAuthenticatorEditor.searchTimeLimit"));
		configBinder.forField(searchTimeLimit).asRequired(msg.getMessage("notAPositiveNumber"))
				.withConverter(new StringToIntegerConverter(msg.getMessage("notAPositiveNumber")))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("searchTimeLimit");
		serverConnectionLayout.addComponent(searchTimeLimit);

		TextField socketTimeout = new TextField(msg.getMessage("LdapAuthenticatorEditor.socketTimeout"));
		configBinder.forField(socketTimeout).asRequired(msg.getMessage("notAPositiveNumber"))
				.withConverter(new StringToIntegerConverter(msg.getMessage("notAPositiveNumber")))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("socketTimeout");
		serverConnectionLayout.addComponent(socketTimeout);

		CheckBox trustAllCerts = new CheckBox(msg.getMessage("LdapAuthenticatorEditor.trustAllCerts"));
		configBinder.forField(trustAllCerts).bind("trustAllCerts");
		serverConnectionLayout.addComponent(trustAllCerts);		
		
		ComboBox<String> clientTrustStore = new ComboBox<>(
				msg.getMessage("LdapAuthenticatorEditor.clientTrustStore"));
		clientTrustStore.setItems(validators);
		configBinder.forField(clientTrustStore).bind("clientTrustStore");
		serverConnectionLayout.addComponent(clientTrustStore);

		trustAllCerts.addValueChangeListener(e -> {
			clientTrustStore.setEnabled(!e.getValue());
		});		
		
		TextField resultEntriesLimit = new TextField(
				msg.getMessage("LdapAuthenticatorEditor.resultEntriesLimit"));
		configBinder.forField(resultEntriesLimit).asRequired(msg.getMessage("notAPositiveNumber"))
				.withConverter(new StringToIntegerConverter(msg.getMessage("notAPositiveNumber")))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("resultEntriesLimit");
		serverConnectionLayout.addComponent(resultEntriesLimit);

		return new CollapsibleLayout(msg.getMessage("OTPWithLDAPAuthenticatorEditor.serverConnectionConfiguration"),
				serverConnectionLayout);
	}
	
	private CollapsibleLayout buildInteractiveLoginSettingsSection()
	{
		FormLayoutWithFixedCaptionWidth interactiveLoginSettings = new FormLayoutWithFixedCaptionWidth();
		interactiveLoginSettings.setMargin(false);
		
		I18nTextField retrievalName = new I18nTextField(msg);
		retrievalName.setCaption(msg.getMessage("OTPWithLDAPAuthenticatorEditor.displayedName"));
		configBinder.forField(retrievalName).bind("retrievalName");
		
		interactiveLoginSettings.addComponent(retrievalName);
		CollapsibleLayout wrapper = new CollapsibleLayout(
				msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
		return wrapper;
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
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
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
