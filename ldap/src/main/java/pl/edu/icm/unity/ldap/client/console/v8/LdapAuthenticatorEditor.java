/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.console.v8;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.*;
import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.ldap.client.LdapCertVerificator;
import pl.edu.icm.unity.ldap.client.LdapPasswordVerificator;
import pl.edu.icm.unity.ldap.client.config.GroupSpecification;
import pl.edu.icm.unity.ldap.client.config.LdapConfiguration;
import pl.edu.icm.unity.ldap.client.config.LdapProperties.BindAs;
import pl.edu.icm.unity.ldap.client.config.SearchSpecification;
import pl.edu.icm.unity.ldap.client.config.ServerSpecification;
import pl.edu.icm.unity.ldap.client.config.common.LDAPCommonConfiguration.UserDNResolving;
import pl.edu.icm.unity.ldap.client.config.common.LDAPConnectionProperties.ConnectionMode;
import pl.edu.icm.unity.ldap.client.config.common.LDAPConnectionProperties.SearchScope;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.*;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * LDAP Authenticator editor
 * 
 * @author P.Piernik
 *
 */
class LdapAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private PKIManagement pkiMan;
	private InputTranslationProfileFieldFactory profileFieldFactory;
	private Binder<LdapConfiguration> configBinder;
	private Set<String> validators;

	private ComboBox<BindAs> bindAsCombo;
	private RadioButtonGroup<UserDNResolving> userDNResolvingMode;

	private TextField systemDN;
	private TextField systemPassword;

	private CollapsibleLayout remoteDataMapping;
	private CollapsibleLayout groupRetrievalSettings;
	private CollapsibleLayout advandcedAttrSearchSettings;

	private String forType;
	private List<String> registrationForms;

	LdapAuthenticatorEditor(MessageSource msg, PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory, List<String> registrationForms,
			String forType) throws EngineException
	{
		super(msg);
		this.pkiMan = pkiMan;
		this.validators = pkiMan.getValidatorNames();
		this.profileFieldFactory = profileFieldFactory;
		this.forType = forType;
		this.registrationForms = registrationForms;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		boolean editMode = init(
				forType.equals(LdapCertVerificator.NAME)
						? msg.getMessage("LdapAuthenticatorEditor.defaultLdapCertName")
						: msg.getMessage("LdapAuthenticatorEditor.defaultName"),
				toEdit, forceNameEditable);

		configBinder = new Binder<>(LdapConfiguration.class);

		FormLayoutWithFixedCaptionWidth header = buildHeaderSection();
		CollapsibleLayout userDNresolvingSettings = buildUserDNResolvingSection();
		userDNresolvingSettings.expand();

		CollapsibleLayout serverConnectionConfiguration = buildServersConnectionConfigurationSection();
		serverConnectionConfiguration.expand();

		remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher, configBinder,
				"translationProfile");

		groupRetrievalSettings = buildGroupRetrievalSettingsSection();

		advandcedAttrSearchSettings = buildAdvancedAttributeSearchSettingsSection();

		CollapsibleLayout interactiveLoginSettings = buildInteractiveLoginSettingsSection();

		LdapConfiguration config = new LdapConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, forType, msg);
		}

		if (forType.equals(LdapCertVerificator.NAME))
		{
			config.setBindAs(BindAs.system);
		}

		configBinder.setBean(config);
		refreshUserDNResolvingSection();

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(userDNresolvingSettings);
		mainView.addComponent(serverConnectionConfiguration);
		mainView.addComponent(remoteDataMapping);
		mainView.addComponent(groupRetrievalSettings);
		mainView.addComponent(advandcedAttrSearchSettings);
		mainView.addComponent(interactiveLoginSettings);

		return mainView;
	}

	private FormLayoutWithFixedCaptionWidth buildHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);

		CheckBox authenticationOnly = new CheckBox(
				msg.getMessage("LdapAuthenticatorEditor.authenticationOnly"));
		configBinder.forField(authenticationOnly).bind("bindOnly");
		header.addComponent(authenticationOnly);
		authenticationOnly.addValueChangeListener(e -> {
			boolean value = e.getValue();
			remoteDataMapping.setVisible(!value);
			groupRetrievalSettings.setVisible(!value);
			advandcedAttrSearchSettings.setVisible(!value);
		});

		bindAsCombo = new ComboBox<>(msg.getMessage("LdapAuthenticatorEditor.bindAs"));
		bindAsCombo.setItems(Arrays.asList(BindAs.system, BindAs.user));
		bindAsCombo.setEmptySelectionAllowed(false);
		configBinder.forField(bindAsCombo).bind("bindAs");
		header.addComponent(bindAsCombo);
		if (forType.equals(LdapCertVerificator.NAME))
		{
			bindAsCombo.setReadOnly(true);
		}

		TextField systemDN = new TextField(msg.getMessage("LdapAuthenticatorEditor.systemDN"));
		systemDN.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(systemDN).asRequired(getSystemBindRequiredValidator()).bind("systemDN");
		header.addComponent(systemDN);

		TextField systemPassword = new TextField(msg.getMessage("LdapAuthenticatorEditor.systemPassword"));
		systemPassword.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(systemPassword).asRequired(getSystemBindRequiredValidator())
				.bind("systemPassword");
		header.addComponent(systemPassword);

		bindAsCombo.addValueChangeListener(e -> {
			BindAs v = e.getValue();
			if (v == null)
				return;
			systemDN.setVisible(v.equals(BindAs.system));
			systemPassword.setVisible(v.equals(BindAs.system));
			setSystemDNAndPasswordField(systemDN, systemPassword);
			refreshUserDNResolvingSection();
		});

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
		header.addComponent(validUserFilter);

		return header;
	}

	private void refreshUserDNResolvingSection()
	{
		UserDNResolving userDNRes = userDNResolvingMode.getValue();
		BindAs bindAs = bindAsCombo.getValue();
		if (bindAs != null && userDNRes != null)
		{
			boolean visable = userDNRes.equals(UserDNResolving.ldapSearch) && !bindAs.equals(BindAs.system);
			systemDN.setVisible(visable);
			systemPassword.setVisible(visable);
			setSystemDNAndPasswordField(systemDN, systemPassword);
		}
	}

	private void setSystemDNAndPasswordField(TextField systemDN, TextField systemPassword)
	{
		LdapConfiguration bean = configBinder.getBean();
		if (bean != null)
		{
			if (bean.getSystemDN() != null)
			{
				systemDN.setValue(bean.getSystemDN());
			}
			if (bean.getSystemPassword() != null)
			{
				systemPassword.setValue(bean.getSystemPassword());
			}
		}
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

		systemDN = new TextField(msg.getMessage("LdapAuthenticatorEditor.systemDN"));
		systemDN.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(systemDN).asRequired(getLdapSearchRequiredValidator()).bind("systemDN");
		userDNResolvingLayout.addComponent(systemDN);

		systemPassword = new TextField(msg.getMessage("LdapAuthenticatorEditor.systemPassword"));
		systemPassword.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(systemPassword).asRequired(getLdapSearchRequiredValidator())
				.bind("systemPassword");
		userDNResolvingLayout.addComponent(systemPassword);

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
			refreshUserDNResolvingSection();
		});

		return new CollapsibleLayout(msg.getMessage("LdapAuthenticatorEditor.userDNResolving"),
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

		return new CollapsibleLayout(msg.getMessage("LdapAuthenticatorEditor.serverConnectionConfiguration"),
				serverConnectionLayout);
	}

	private CollapsibleLayout buildGroupRetrievalSettingsSection()
	{
		FormLayoutWithFixedCaptionWidth groupRetSettingsLayout = new FormLayoutWithFixedCaptionWidth();
		groupRetSettingsLayout.setMargin(false);

		CheckBox delegateGroupFiltering = new CheckBox(
				msg.getMessage("LdapAuthenticatorEditor.delegateGroupFiltering"));
		configBinder.forField(delegateGroupFiltering).bind("delegateGroupFiltering");
		groupRetSettingsLayout.addComponent(delegateGroupFiltering);

		TextField groupsBaseName = new TextField(msg.getMessage("LdapAuthenticatorEditor.groupsBaseName"));
		groupsBaseName.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(groupsBaseName).bind("groupsBaseName");
		groupRetSettingsLayout.addComponent(groupsBaseName);

		TextField memberOfAttribute = new TextField(
				msg.getMessage("LdapAuthenticatorEditor.memberOfAttribute"));
		memberOfAttribute.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(memberOfAttribute).bind("memberOfAttribute");
		groupRetSettingsLayout.addComponent(memberOfAttribute);

		TextField memberOfGroupAttribute = new TextField(
				msg.getMessage("LdapAuthenticatorEditor.memberOfGroupAttribute"));
		memberOfGroupAttribute.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(memberOfGroupAttribute).bind("memberOfGroupAttribute");
		groupRetSettingsLayout.addComponent(memberOfGroupAttribute);

		GridWithEditor<GroupSpecification> groupConfig = new GridWithEditor<>(msg, GroupSpecification.class);
		groupConfig.setCaption(msg.getMessage("LdapAuthenticatorEditor.groupSpecifications"));
		groupRetSettingsLayout.addComponent(groupConfig);
		groupConfig.addTextColumn(s -> s.getMatchByMemberAttribute(), (t, v) -> t.setMatchByMemberAttribute(v),
				msg.getMessage("LdapAuthenticatorEditor.groupSpecification.matchByMemberAttribute"), 20,
				false);

		groupConfig.addTextColumn(s -> s.getMemberAttribute(), (t, v) -> t.setMemberAttribute(v),
				msg.getMessage("LdapAuthenticatorEditor.groupSpecification.memberAttribute"), 20, true);

		groupConfig.addTextColumn(s -> s.getGroupNameAttribute(), (t, v) -> t.setGroupNameAttribute(v),
				msg.getMessage("LdapAuthenticatorEditor.groupSpecification.nameAttribute"), 20, false);

		groupConfig.addTextColumn(s -> s.getObjectClass(), (t, v) -> t.setObjectClass(v),
				msg.getMessage("LdapAuthenticatorEditor.groupSpecification.objectClass"), 20, true);

		groupConfig.setWidth(100, Unit.PERCENTAGE);

		configBinder.forField(groupConfig).bind("groupSpecifications");

		return new CollapsibleLayout(msg.getMessage("LdapAuthenticatorEditor.groupRetrievalSettings"),
				groupRetSettingsLayout);
	}

	private CollapsibleLayout buildAdvancedAttributeSearchSettingsSection()
	{

		FormLayoutWithFixedCaptionWidth advancedAttrSearchLayout = new FormLayoutWithFixedCaptionWidth();
		advancedAttrSearchLayout.setMargin(false);

		ChipsWithTextfield retrievalLdapAttr = new ChipsWithTextfield(msg);
		retrievalLdapAttr.setCaption(msg.getMessage("LdapAuthenticatorEditor.retrievedAttributes"));
		advancedAttrSearchLayout.addComponent(retrievalLdapAttr);
		configBinder.forField(retrievalLdapAttr).bind("retrievalLdapAttributes");

		GridWithEditor<SearchSpecification> searchConfig = new GridWithEditor<>(msg, SearchSpecification.class);
		searchConfig.setCaption(msg.getMessage("LdapAuthenticatorEditor.searchSpecifications"));
		advancedAttrSearchLayout.addComponent(searchConfig);
		searchConfig.addTextColumn(s -> s.getBaseDN(), (t, v) -> t.setBaseDN(v),
				msg.getMessage("LdapAuthenticatorEditor.searchSpecification.baseName"), 30, true);

		searchConfig.addTextColumn(s -> s.getFilter(), (t, v) -> t.setFilter(v),
				msg.getMessage("LdapAuthenticatorEditor.searchSpecification.filter"), 30, true,
				Optional.of(getFilterValidator()));
		searchConfig.addTextColumn(s -> s.getAttributes(), (t, v) -> t.setAttributes(v),
				msg.getMessage("LdapAuthenticatorEditor.searchSpecification.attributes"), 20, true);

		searchConfig.addComboColumn(s -> s.getScope(), (t, v) -> t.setScope(v), SearchScope.class,
				msg.getMessage("LdapAuthenticatorEditor.searchSpecification.scope"), 10);

		searchConfig.setWidth(100, Unit.PERCENTAGE);
		configBinder.forField(searchConfig).bind("searchSpecifications");

		TextField usernameExtractorRegexp = new TextField(
				msg.getMessage("LdapAuthenticatorEditor.usernameExtractorRegexp"));
		usernameExtractorRegexp.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		configBinder.forField(usernameExtractorRegexp).bind("usernameExtractorRegexp");
		advancedAttrSearchLayout.addComponent(usernameExtractorRegexp);

		return new CollapsibleLayout(msg.getMessage("LdapAuthenticatorEditor.advancedAttributeSearchSettings"),
				advancedAttrSearchLayout);

	}

	private CollapsibleLayout buildInteractiveLoginSettingsSection()
	{
		FormLayoutWithFixedCaptionWidth interactiveLoginSettings = new FormLayoutWithFixedCaptionWidth();
		interactiveLoginSettings.setMargin(false);

		I18nTextField retrievalName = new I18nTextField(msg);
		retrievalName.setCaption(forType.equals(LdapPasswordVerificator.NAME)
				? msg.getMessage("LdapAuthenticatorEditor.passwordName")
				: msg.getMessage("LdapAuthenticatorEditor.displayedName"));
		configBinder.forField(retrievalName).bind("retrievalName");
		interactiveLoginSettings.addComponent(retrievalName);

		CheckBox accountAssociation = new CheckBox(
				msg.getMessage("LdapAuthenticatorEditor.accountAssociation"));
		configBinder.forField(accountAssociation).bind("accountAssociation");
		interactiveLoginSettings.addComponent(accountAssociation);

		ComboBox<String> registrationForm = new ComboBox<>(
				msg.getMessage("LdapAuthenticatorEditor.registrationForm"));
		registrationForm.setItems(registrationForms);
		configBinder.forField(registrationForm).bind("registrationForm");
		interactiveLoginSettings.addComponent(registrationForm);

		return new CollapsibleLayout(msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
	}

	private Validator<String> getSystemBindRequiredValidator()
	{
		return (v, c) -> {
			if (bindAsCombo.getValue().equals(BindAs.system) && (v == null || v.isEmpty()))
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			} else
			{
				return ValidationResult.ok();
			}
		};
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

		return new AuthenticatorDefinition(getName(), forType, getConfiguration(), null);
	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
		{
			throw new FormValidationException();
		}

		LdapConfiguration conf = configBinder.getBean();
		try
		{
			conf.validateConfiguration(pkiMan);
		} catch (Exception e)
		{

			throw new FormValidationException("Invalid ldap authenticator configuration", e);
		}

		return conf.toProperties(forType, msg);
	}

}
