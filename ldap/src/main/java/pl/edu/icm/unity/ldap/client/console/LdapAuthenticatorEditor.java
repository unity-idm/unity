/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.console;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
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
import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.TooltipFactory;
import io.imunity.vaadin.elements.grid.EditableGrid;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
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
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class LdapAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private final PKIManagement pkiMan;
	private final InputTranslationProfileFieldFactory profileFieldFactory;
	private final Set<String> validators;
	private Binder<LdapConfiguration> configBinder;

	private Select<BindAs> bindAsCombo;
	private RadioButtonGroup<UserDNResolving> userDNResolvingMode;

	private TextField systemDN;
	private TextField systemPassword;

	private AccordionPanel remoteDataMapping;
	private AccordionPanel groupRetrievalSettings;
	private AccordionPanel advancedAttrSearchSettings;

	private final String forType;
	private final List<String> registrationForms;

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

		FormLayout header = buildHeaderSection();

		AccordionPanel userDResolvingSettings = buildUserDNResolvingSection();
		userDResolvingSettings.setOpened(true);
		userDResolvingSettings.setWidthFull();

		AccordionPanel serverConnectionConfiguration = buildServersConnectionConfigurationSection();
		serverConnectionConfiguration.setOpened(true);
		serverConnectionConfiguration.setWidthFull();

		remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher, configBinder,
				"translationProfile");
		remoteDataMapping.setWidthFull();

		groupRetrievalSettings = buildGroupRetrievalSettingsSection();
		groupRetrievalSettings.setWidthFull();

		advancedAttrSearchSettings = buildAdvancedAttributeSearchSettingsSection();
		advancedAttrSearchSettings.setWidthFull();

		AccordionPanel interactiveLoginSettings = buildInteractiveLoginSettingsSection();
		interactiveLoginSettings.setWidthFull();

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
		mainView.setPadding(false);
		mainView.add(header, userDResolvingSettings, serverConnectionConfiguration, remoteDataMapping,
				groupRetrievalSettings, advancedAttrSearchSettings, interactiveLoginSettings);

		return mainView;
	}

	private FormLayout buildHeaderSection()
	{
		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));

		Checkbox authenticationOnly = new Checkbox(
				msg.getMessage("LdapAuthenticatorEditor.authenticationOnly"));
		configBinder.forField(authenticationOnly).bind(LdapConfiguration::isBindOnly, LdapConfiguration::setBindOnly);
		header.addFormItem(authenticationOnly, "");
		authenticationOnly.addValueChangeListener(e ->
		{
			boolean value = e.getValue();
			remoteDataMapping.setVisible(!value);
			groupRetrievalSettings.setVisible(!value);
			advancedAttrSearchSettings.setVisible(!value);
		});

		bindAsCombo = new Select<>();
		bindAsCombo.setItems(Arrays.asList(BindAs.system, BindAs.user));
		bindAsCombo.setEmptySelectionAllowed(false);
		configBinder.forField(bindAsCombo).bind(LdapConfiguration::getBindAs, LdapConfiguration::setBindAs);
		header.addFormItem(bindAsCombo, msg.getMessage("LdapAuthenticatorEditor.bindAs"));
		if (forType.equals(LdapCertVerificator.NAME))
		{
			bindAsCombo.setReadOnly(true);
		}

		TextField systemDN = new TextField();
		systemDN.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(systemDN)
				.asRequired(getSystemBindRequiredValidator())
				.bind(LdapConfiguration::getSystemDN, LdapConfiguration::setSystemDN);
		header.addFormItem(systemDN, msg.getMessage("LdapAuthenticatorEditor.systemDN"));

		TextField systemPassword = new TextField();
		systemPassword.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(systemPassword).asRequired(getSystemBindRequiredValidator())
				.bind(LdapConfiguration::getSystemPassword, LdapConfiguration::setSystemPassword);
		header.addFormItem(systemPassword, msg.getMessage("LdapAuthenticatorEditor.systemPassword"));

		bindAsCombo.addValueChangeListener(e ->
		{
			BindAs v = e.getValue();
			if (v == null)
				return;
			systemDN.getParent().get().setVisible(v.equals(BindAs.system));
			systemPassword.getParent().get().setVisible(v.equals(BindAs.system));
			setSystemDNAndPasswordField(systemDN, systemPassword);
			refreshUserDNResolvingSection();
		});

		TextField validUserFilter = new TextField();
		validUserFilter.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(validUserFilter).withValidator((v, c) ->
		{
			try
			{
				Filter.create(v);
			} catch (LDAPException e)
			{
				return ValidationResult.error(
						msg.getMessage("LdapAuthenticatorEditor.invalidValidUserFilter"));
			}

			return ValidationResult.ok();

		}).bind(LdapConfiguration::getValidUserFilter, LdapConfiguration::setValidUserFilter);
		header.addFormItem(validUserFilter, msg.getMessage("LdapAuthenticatorEditor.validUserFilter"));

		return header;
	}

	private void refreshUserDNResolvingSection()
	{
		UserDNResolving userDNRes = userDNResolvingMode.getValue();
		BindAs bindAs = bindAsCombo.getValue();
		if (bindAs != null && userDNRes != null)
		{
			boolean visable = userDNRes.equals(UserDNResolving.ldapSearch) && !bindAs.equals(BindAs.system);
			systemDN.getParent().get().setVisible(visable);
			systemPassword.getParent().get().setVisible(visable);
			setSystemDNAndPasswordField(systemDN, systemPassword);
		}
	}

	private void setSystemDNAndPasswordField(TextField systemDN, TextField systemPassword)
	{
		LdapConfiguration bean = configBinder.getBean();
		if (bean != null)
		{
			if (bean.getSystemDN() != null)
				systemDN.setValue(bean.getSystemDN());
			if (bean.getSystemPassword() != null)
				systemPassword.setValue(bean.getSystemPassword());
		}
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
		configBinder.forField(userDNResolvingMode).bind(LdapConfiguration::getUserDNResolving,
				LdapConfiguration::setUserDNResolving);
		userDNResolvingLayout.addFormItem(userDNResolvingMode, "");

		TextField userDNtemplate = new TextField();
		userDNtemplate.setWidth(TEXT_FIELD_BIG.value());

		userDNtemplate.setPlaceholder("uid={USERNAME},dc=myorg,dc=global");

		configBinder.forField(userDNtemplate).withValidator((v, c) ->
		{
			if (v != null && v.contains("{USERNAME}")
					|| !userDNResolvingMode.getValue().equals(UserDNResolving.template))
				return ValidationResult.ok();
			else
				return ValidationResult
						.error(msg.getMessage("LdapAuthenticatorEditor.invalidUserDNtemplate"));
		}).bind(LdapConfiguration::getUserDNTemplate, LdapConfiguration::setUserDNTemplate);

		userDNResolvingLayout.addFormItem(userDNtemplate, msg.getMessage("LdapAuthenticatorEditor.userDNtemplate"))
			.add(TooltipFactory.get(msg.getMessage("LdapAuthenticatorEditor.userDNtemplate.desc")));

		systemDN = new TextField();
		configBinder.forField(systemDN).asRequired(getLdapSearchRequiredValidator()).bind(
				LdapConfiguration::getSystemDN, LdapConfiguration::setSystemDN);
		userDNResolvingLayout.addFormItem(systemDN, msg.getMessage("LdapAuthenticatorEditor.systemDN"));

		systemPassword = new TextField();
		configBinder.forField(systemPassword).asRequired(getLdapSearchRequiredValidator())
				.bind(LdapConfiguration::getSystemPassword, LdapConfiguration::setSystemPassword);
		userDNResolvingLayout.addFormItem(systemPassword, msg.getMessage("LdapAuthenticatorEditor.systemPassword"));

		TextField ldapSearchBaseName = new TextField();
		ldapSearchBaseName.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(ldapSearchBaseName).asRequired(getLdapSearchRequiredValidator())
				.bind(LdapConfiguration::getLdapSearchBaseName, LdapConfiguration::setLdapSearchBaseName);
		userDNResolvingLayout.addFormItem(ldapSearchBaseName,
				msg.getMessage("LdapAuthenticatorEditor.searchSpecification.baseName"));

		TextField ldapSearchFilter = new TextField();
		ldapSearchFilter.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(ldapSearchFilter).withValidator((v, c) ->
		{
			if (userDNResolvingMode.getValue().equals(UserDNResolving.ldapSearch))
				return getFilterValidator().apply(v, c);
			else
				return ValidationResult.ok();
		}).bind(LdapConfiguration::getLdapSearchFilter, LdapConfiguration::setLdapSearchFilter);
		userDNResolvingLayout.addFormItem(ldapSearchFilter,
				msg.getMessage("LdapAuthenticatorEditor.searchSpecification.filter"));

		Select<SearchScope> ldapSearchScope = new Select<>();
		ldapSearchScope.setItems(SearchScope.values());
		configBinder.forField(ldapSearchScope).bind(LdapConfiguration::getLdapSearchScope,
				LdapConfiguration::setLdapSearchScope);
		userDNResolvingLayout.addFormItem(ldapSearchScope,
				msg.getMessage("LdapAuthenticatorEditor.searchSpecification.scope"));

		userDNResolvingMode.addValueChangeListener(e ->
		{
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
			refreshUserDNResolvingSection();
		});

		return new AccordionPanel(msg.getMessage("LdapAuthenticatorEditor.userDNResolving"), userDNResolvingLayout);
	}

	private AccordionPanel buildServersConnectionConfigurationSection()
	{
		FormLayout serverConnectionLayout = new FormLayout();
		serverConnectionLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		serverConnectionLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		EditableGrid<ServerSpecification> serverConfig = new EditableGrid<>(msg::getMessage, ServerSpecification::new);
		serverConfig.setWidth(TEXT_FIELD_BIG.value());
		serverConfig.setHeight("20em");
		serverConnectionLayout.addFormItem(serverConfig, "");
		serverConfig.addColumn(ServerSpecification::getServer, ServerSpecification::setServer, true)
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.server"))
				.setAutoWidth(true);
		serverConfig.addIntColumn(ServerSpecification::getPort, ServerSpecification::setPort)
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.port"))
				.setAutoWidth(true);

		configBinder.forField(serverConfig).withValidator((v, c) ->
		{
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			} else
			{
				return ValidationResult.ok();
			}
		}).bind(LdapConfiguration::getServers, LdapConfiguration::setServers);

		Select<ConnectionMode> connectionMode = new Select<>();
		connectionMode.setItems(ConnectionMode.values());
		connectionMode.setEmptySelectionAllowed(false);
		configBinder.forField(connectionMode).bind(LdapConfiguration::getConnectionMode,
				LdapConfiguration::setConnectionMode);
		serverConnectionLayout.addFormItem(connectionMode, msg.getMessage("LdapAuthenticatorEditor.connectionMode"));

		IntegerField followReferrals = new IntegerField();
		followReferrals.setMin(0);
		configBinder.forField(followReferrals).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind(LdapConfiguration::getFollowReferrals, LdapConfiguration::setFollowReferrals);
		serverConnectionLayout.addFormItem(followReferrals, msg.getMessage("LdapAuthenticatorEditor.followReferrals"));

		IntegerField searchTimeLimit = new IntegerField();
		searchTimeLimit.setMin(0);
		configBinder.forField(searchTimeLimit).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind(LdapConfiguration::getSearchTimeLimit, LdapConfiguration::setSearchTimeLimit);
		serverConnectionLayout.addFormItem(searchTimeLimit, msg.getMessage("LdapAuthenticatorEditor.searchTimeLimit"));

		IntegerField socketTimeout = new IntegerField();
		socketTimeout.setMin(0);
		configBinder.forField(socketTimeout).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind(LdapConfiguration::getSocketTimeout, LdapConfiguration::setSocketTimeout);
		serverConnectionLayout.addFormItem(socketTimeout, msg.getMessage("LdapAuthenticatorEditor.socketTimeout"));

		Checkbox trustAllCerts = new Checkbox(msg.getMessage("LdapAuthenticatorEditor.trustAllCerts"));
		configBinder.forField(trustAllCerts).bind(LdapConfiguration::isTrustAllCerts,
				LdapConfiguration::setTrustAllCerts);
		serverConnectionLayout.addFormItem(trustAllCerts, "");

		Select<String> clientTrustStore = new Select<>();
		clientTrustStore.setItems(validators);
		clientTrustStore.setEmptySelectionAllowed(true);
		configBinder.forField(clientTrustStore).bind(LdapConfiguration::getClientTrustStore,
				LdapConfiguration::setClientTrustStore);
		serverConnectionLayout.addFormItem(clientTrustStore,
				msg.getMessage("LdapAuthenticatorEditor.clientTrustStore"));

		trustAllCerts.addValueChangeListener(e -> clientTrustStore.setEnabled(!e.getValue()));

		IntegerField resultEntriesLimit = new IntegerField();
		configBinder.forField(resultEntriesLimit).asRequired(msg.getMessage("notAPositiveNumber"))
				.bind(LdapConfiguration::getResultEntriesLimit, LdapConfiguration::setResultEntriesLimit);
		serverConnectionLayout.addFormItem(resultEntriesLimit,
				msg.getMessage("LdapAuthenticatorEditor.resultEntriesLimit"));

		return new AccordionPanel(msg.getMessage("LdapAuthenticatorEditor.serverConnectionConfiguration"),
				serverConnectionLayout);
	}

	private AccordionPanel buildGroupRetrievalSettingsSection()
	{
		FormLayout groupRetSettingsLayout = new FormLayout();
		groupRetSettingsLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		groupRetSettingsLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		Checkbox delegateGroupFiltering = new Checkbox(
				msg.getMessage("LdapAuthenticatorEditor.delegateGroupFiltering"));
		configBinder.forField(delegateGroupFiltering).bind(LdapConfiguration::isDelegateGroupFiltering,
				LdapConfiguration::setDelegateGroupFiltering);
		groupRetSettingsLayout.addFormItem(delegateGroupFiltering, "");

		TextField groupsBaseName = new TextField();
		groupsBaseName.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(groupsBaseName).bind(LdapConfiguration::getGroupsBaseName,
				LdapConfiguration::setGroupsBaseName);
		groupRetSettingsLayout.addFormItem(groupsBaseName, msg.getMessage("LdapAuthenticatorEditor.groupsBaseName"));

		TextField memberOfAttribute = new TextField();
		memberOfAttribute.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(memberOfAttribute).bind(LdapConfiguration::getMemberOfAttribute,
				LdapConfiguration::setMemberOfAttribute);
		groupRetSettingsLayout.addFormItem(memberOfAttribute,
				msg.getMessage("LdapAuthenticatorEditor.memberOfAttribute"));

		TextField memberOfGroupAttribute = new TextField();
		memberOfGroupAttribute.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(memberOfGroupAttribute).bind(LdapConfiguration::getMemberOfGroupAttribute,
				LdapConfiguration::setMemberOfGroupAttribute);
		groupRetSettingsLayout.addFormItem(memberOfGroupAttribute,
				msg.getMessage("LdapAuthenticatorEditor.memberOfGroupAttribute"));

		EditableGrid<GroupSpecification> groupConfig = new EditableGrid<>(msg::getMessage, GroupSpecification::new);
		groupConfig.setWidthFull();
		groupConfig.setHeight("20em");

		groupRetSettingsLayout.addFormItem(groupConfig, msg.getMessage("LdapAuthenticatorEditor.groupSpecifications"));

		groupConfig.addColumn(GroupSpecification::getMatchByMemberAttribute,
						GroupSpecification::setMatchByMemberAttribute, false)
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.groupSpecification.matchByMemberAttribute"))
				.setFlexGrow(2)
				.setAutoWidth(true);
		groupConfig.addColumn(GroupSpecification::getMemberAttribute, GroupSpecification::setMemberAttribute, true)
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.groupSpecification.memberAttribute"))
				.setFlexGrow(1)
				.setAutoWidth(true);
		groupConfig.addColumn(GroupSpecification::getGroupNameAttribute, GroupSpecification::setGroupNameAttribute, false)
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.groupSpecification.nameAttribute"))
				.setAutoWidth(true);
		groupConfig.addColumn(GroupSpecification::getObjectClass, GroupSpecification::setObjectClass, true)
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.groupSpecification.objectClass"))
				.setAutoWidth(true);

		configBinder.forField(groupConfig).bind(LdapConfiguration::getGroupSpecifications,
				LdapConfiguration::setGroupSpecifications);

		return new AccordionPanel(msg.getMessage("LdapAuthenticatorEditor.groupRetrievalSettings"),
				groupRetSettingsLayout);

	}

	private AccordionPanel buildAdvancedAttributeSearchSettingsSection()
	{
		FormLayout advancedAttrSearchLayout = new FormLayout();
		advancedAttrSearchLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		advancedAttrSearchLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		MultiSelectComboBox<String> retrievalLdapAttr = new CustomValuesMultiSelectComboBox();
		retrievalLdapAttr.setPlaceholder(msg.getMessage("typeAndConfirm"));
		retrievalLdapAttr.setWidth(TEXT_FIELD_BIG.value());
		advancedAttrSearchLayout.addFormItem(retrievalLdapAttr,
				msg.getMessage("LdapAuthenticatorEditor.retrievedAttributes"));
		configBinder.forField(retrievalLdapAttr)
				.withConverter(List::copyOf, HashSet::new)
				.bind(LdapConfiguration::getRetrievalLdapAttributes, LdapConfiguration::setRetrievalLdapAttributes);

		EditableGrid<SearchSpecification> searchConfig = new EditableGrid<>(msg::getMessage, SearchSpecification::new);
		searchConfig.setWidthFull();
		searchConfig.setHeight("20em");
		advancedAttrSearchLayout.addFormItem(searchConfig,
				msg.getMessage("LdapAuthenticatorEditor.searchSpecifications"));

		searchConfig.addColumn(SearchSpecification::getBaseDN, SearchSpecification::setBaseDN, true)
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.searchSpecification.baseName"))
				.setAutoWidth(true);
		searchConfig.addColumn(SearchSpecification::getFilter, SearchSpecification::setFilter, getFilterValidator())
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.searchSpecification.filter"))
				.setAutoWidth(true);
		searchConfig.addColumn(SearchSpecification::getAttributes, SearchSpecification::setAttributes, true)
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.searchSpecification.attributes"))
				.setAutoWidth(true);
		searchConfig.addComboBoxColumn(
						searchSpecification -> searchSpecification.getScope().name(),
						(searchSpecification, scope) -> searchSpecification.setScope(SearchScope.valueOf(scope)),
						Stream.of(SearchScope.values()).map(Enum::name).toList())
				.setHeader(msg.getMessage("LdapAuthenticatorEditor.searchSpecification.scope"))
				.setAutoWidth(true);

		configBinder.forField(searchConfig).bind(LdapConfiguration::getSearchSpecifications,
				LdapConfiguration::setSearchSpecifications);

		TextField usernameExtractorRegexp = new TextField();
		usernameExtractorRegexp.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(usernameExtractorRegexp).bind(LdapConfiguration::getUsernameExtractorRegexp,
				LdapConfiguration::setUsernameExtractorRegexp);
		advancedAttrSearchLayout.addFormItem(usernameExtractorRegexp,
				msg.getMessage("LdapAuthenticatorEditor.usernameExtractorRegexp"));

		return new AccordionPanel(msg.getMessage("LdapAuthenticatorEditor.advancedAttributeSearchSettings"),
				advancedAttrSearchLayout);
	}

	private AccordionPanel buildInteractiveLoginSettingsSection()
	{
		FormLayout interactiveLoginSettings = new FormLayout();
		interactiveLoginSettings.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		interactiveLoginSettings.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		LocalizedTextFieldDetails retrievalName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(),
				msg.getLocale());
		retrievalName.setWidth(TEXT_FIELD_MEDIUM.value());
		configBinder.forField(retrievalName)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind(LdapConfiguration::getRetrievalName, LdapConfiguration::setRetrievalName);
		interactiveLoginSettings.addFormItem(retrievalName, forType.equals(LdapPasswordVerificator.NAME)
				? msg.getMessage("LdapAuthenticatorEditor.passwordName")
				: msg.getMessage("LdapAuthenticatorEditor.displayedName"));

		Checkbox accountAssociation = new Checkbox(
				msg.getMessage("LdapAuthenticatorEditor.accountAssociation"));
		configBinder.forField(accountAssociation).bind(LdapConfiguration::isAccountAssociation,
				LdapConfiguration::setAccountAssociation);
		interactiveLoginSettings.addFormItem(accountAssociation, "");

		Select<String> registrationForm = new Select<>();
		registrationForm.setItems(registrationForms);
		registrationForm.setWidth(TEXT_FIELD_MEDIUM.value());
		registrationForm.setEmptySelectionAllowed(true);
		registrationForm.setEmptySelectionCaption("");
		configBinder.forField(registrationForm).bind(LdapConfiguration::getRegistrationForm,
				LdapConfiguration::setRegistrationForm);
		interactiveLoginSettings.addFormItem(registrationForm, msg.getMessage("LdapAuthenticatorEditor.registrationForm"));

		return new AccordionPanel(msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
	}

	private Validator<String> getSystemBindRequiredValidator()
	{
		return (v, c) ->
		{
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
		return (v, c) ->
		{
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
		return (v, c) ->
		{

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
