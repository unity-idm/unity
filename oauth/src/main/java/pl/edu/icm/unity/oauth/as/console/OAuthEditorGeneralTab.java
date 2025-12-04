/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;
import static io.imunity.vaadin.elements.CssClassNames.IDP_INFO_LAYOUT;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSAlgorithm.Family;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.console.utils.tprofile.OutputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.elements.EnumComboBox;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.NoSpaceValidator;
import io.imunity.vaadin.elements.grid.GridWithEditorInDetails;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.endpoint.EndpointPathValidator;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.SigningAlgorithms;
import pl.edu.icm.unity.oauth.as.OAuthScopeDefinition;
import pl.edu.icm.unity.oauth.as.OAuthSystemScopeProvider;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;

/**
 * OAuth service editor general tab
 * 
 * @author P.Piernik
 *
 */
class OAuthEditorGeneralTab extends VerticalLayout implements ServiceEditorBase.EditorTab
{
	private final MessageSource msg;
	private final PKIManagement pkiManagement;
	private Binder<DefaultServiceDefinition> oauthWebAuthzBinder;
	private Binder<DefaultServiceDefinition> oauthTokenBinder;
	private Binder<OAuthServiceConfiguration> configBinder;
	private Set<String> credentials;
	private Set<String> certificates; 
	private Collection<IdentityType> idTypes;
	private List<String> attrTypes;
	private List<String> usedEndpointsPaths;
	private String serverPrefix;
	private Set<String> serverContextPaths;
	private Checkbox openIDConnect;
	private Checkbox tokenExchangeSupport;
	private ComboBox<String> credential;
	private ComboBox<SigningAlgorithms> signingAlg;
	private ComboBox<AccessTokenFormat> accessTokenFormat;
	private TextField signingSecret;
	private GridWithEditorInDetails<OAuthScopeBean> scopesGrid;
	private GridWithEditorInDetails<AuthorizationScriptBean> scriptsGrid;
	private OutputTranslationProfileFieldFactory profileFieldFactory;
	private SubViewSwitcher subViewSwitcher;
	private TextField name;
	private boolean editMode;
	private List<OAuthScopeDefinition> systemScopes;
	private GridWithEditorInDetails<TrustedUpstreamASBean> trustedUpstreamAsGrid;
	private Set<String> validators;
	private final HtmlTooltipFactory htmlTooltipFactory;

	OAuthEditorGeneralTab(MessageSource msg, PKIManagement pkiManagement, HtmlTooltipFactory htmlTooltipFactory, String serverPrefix, Set<String> serverContextPaths,
			SubViewSwitcher subViewSwitcher, OutputTranslationProfileFieldFactory profileFieldFactory, boolean editMode,
			Set<String> credentials, Collection<IdentityType> identityTypes, List<String> attrTypes,
			List<String> usedEndpointsPaths, List<OAuthScopeDefinition> systemScopes, Set<String> validators, Set<String> certificates)
	{
		this.msg = msg;
		this.pkiManagement = pkiManagement;
		
		this.editMode = editMode;
		this.credentials = credentials;
		this.idTypes = identityTypes;
		this.attrTypes = attrTypes;
		this.subViewSwitcher = subViewSwitcher;
		this.profileFieldFactory = profileFieldFactory;
		this.usedEndpointsPaths = usedEndpointsPaths;
		this.serverPrefix = serverPrefix;
		this.serverContextPaths = serverContextPaths;
		this.systemScopes = List.copyOf(systemScopes);
		this.validators = validators;
		this.certificates = certificates;
		this.htmlTooltipFactory = htmlTooltipFactory;
	}

	void initUI(Binder<DefaultServiceDefinition> oauthWebAuthzBinder, Binder<DefaultServiceDefinition> oauthTokenBinder,
			Binder<OAuthServiceConfiguration> configBinder)
	{
		this.oauthTokenBinder = oauthTokenBinder;
		this.oauthWebAuthzBinder = oauthWebAuthzBinder;
		this.configBinder = configBinder;

		setPadding(false);
		getStyle().set("overflow-x", "hidden");

		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);

		AccordionPanel scopesPanel = buildScopesSection();
		AccordionPanel scriptsAccordionPanel = buildScriptsSection();

		main.add(buildHeaderSection());
		main.add(scopesPanel);
		main.add(scriptsAccordionPanel);
		main.add(buildAdvancedSection());
		main.add(
				profileFieldFactory.getWrappedFieldInstance(subViewSwitcher, configBinder, "translationProfile"));
		main.add(buildTrustedUpstremsSection());

		add(main);
	}

	private AccordionPanel buildAdvancedSection()
	{
		FormLayout advancedLayout = new FormLayout();
		advancedLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		advancedLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		ComboBox<String> idForSub = new ComboBox<>();
		idForSub.setItems(idTypes.stream()
				.map(IdentityType::getName).toList());
		configBinder.forField(idForSub)
				.bind("identityTypeForSubject");
		advancedLayout.addFormItem(idForSub, msg.getMessage("OAuthEditorGeneralTab.identityTypeForSubject"));

		Checkbox allowForWildcardsInAllowedURI = new Checkbox(
				msg.getMessage("OAuthEditorGeneralTab.allowForWildcardsInAllowedURI"));
		configBinder.forField(allowForWildcardsInAllowedURI)
				.bind("allowForWildcardsInAllowedURI");
		advancedLayout.addFormItem(allowForWildcardsInAllowedURI, "");

		Checkbox allowForUnauthenticatedRevocation = new Checkbox(
				msg.getMessage("OAuthEditorGeneralTab.allowForUnauthenticatedRevocation"));
		configBinder.forField(allowForUnauthenticatedRevocation)
				.bind("allowForUnauthenticatedRevocation");
		advancedLayout.addFormItem(allowForUnauthenticatedRevocation, "");

		AccordionPanel accordionPanel = new AccordionPanel(msg.getMessage("OAuthEditorGeneralTab.advanced"),
				advancedLayout);
		accordionPanel.setWidthFull();
		return accordionPanel;
	}

	private Component buildHeaderSection()
	{
		HorizontalLayout main = new HorizontalLayout();

		FormLayout mainGeneralLayout = new FormLayout();
		mainGeneralLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		mainGeneralLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		main.add(mainGeneralLayout);

		HorizontalLayout infoLayout = new HorizontalLayout();
		infoLayout.addClassName(IDP_INFO_LAYOUT.getName());
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setPadding(false);
		wrapper.setSpacing(false);
		infoLayout.add(wrapper);
		wrapper.add(new Span(msg.getMessage("OAuthEditorGeneralTab.importantURLs")));
		FormLayout infoLayoutWrapper = new FormLayout();
		infoLayoutWrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		wrapper.add(infoLayoutWrapper);

		Span userAuthnEndpointPath = new Span();
		infoLayoutWrapper.addFormItem(userAuthnEndpointPath, msg.getMessage("OAuthEditorGeneralTab.userAuthnEndpointPath"));
		main.add(infoLayout);

		Span tokenEndpointPath = new Span();
		infoLayoutWrapper.addFormItem(tokenEndpointPath, msg.getMessage("OAuthEditorGeneralTab.tokenEndpointPath"));

		Button metaPath = new Button();
		metaPath.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

		if (editMode)
		{
			infoLayoutWrapper.addFormItem(metaPath, msg.getMessage("OAuthEditorGeneralTab.metadataLink"));
			metaPath.addClickListener(e -> UI.getCurrent().getPage()
					.open(metaPath.getText(), "_blank"));
		}

		name = new TextField();
		name.setReadOnly(editMode);
		oauthWebAuthzBinder.forField(name)
				.asRequired()
				.bind("name");
		mainGeneralLayout.addFormItem(name, msg.getMessage("ServiceEditorBase.name"));

		TextField tokenContextPath = new TextField();

		TextField webAuthzContextPath = new TextField();
		webAuthzContextPath.setRequiredIndicatorVisible(true);
		webAuthzContextPath.setReadOnly(editMode);
		webAuthzContextPath.setPlaceholder("/oauth");
		oauthWebAuthzBinder.forField(webAuthzContextPath)
				.withValidator((v, c) ->
				{

					ValidationResult r;
					if (editMode)
					{
						r = validatePathForEdit(v);
					} else
					{
						r = validatePathForAdd(v, tokenContextPath.getValue());
					}

					if (r.isError())
					{
						userAuthnEndpointPath.setText("");
					} else
					{
						userAuthnEndpointPath
								.setText(serverPrefix + v + OAuthAuthzWebEndpoint.OAUTH_CONSUMER_SERVLET_PATH);
					}
					return r;

				})
				.bind("address");
		mainGeneralLayout.addFormItem(webAuthzContextPath, msg.getMessage("OAuthEditorGeneralTab.usersAuthnPath"));
		tokenContextPath.setRequiredIndicatorVisible(true);
		tokenContextPath.setPlaceholder("/oauth-token");
		tokenContextPath.setReadOnly(editMode);
		oauthTokenBinder.forField(tokenContextPath)
				.withValidator((v, c) ->
				{
					ValidationResult r;
					if (editMode)
					{
						r = validatePathForEdit(v);
					} else
					{
						r = validatePathForAdd(v, webAuthzContextPath.getValue());
					}

					if (r.isError())
					{
						tokenEndpointPath.setText("");
					} else
					{
						tokenEndpointPath.setText(serverPrefix + v + OAuthTokenEndpoint.TOKEN_PATH);
						metaPath.setText(serverPrefix + v + "/.well-known/openid-configuration");
					}
					return r;

				})
				.bind("address");
		mainGeneralLayout.addFormItem(tokenContextPath, msg.getMessage("OAuthEditorGeneralTab.clientTokenPath"));

		LocalizedTextFieldDetails displayedName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		oauthWebAuthzBinder.forField(displayedName)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind(DefaultServiceDefinition::getDisplayedName, DefaultServiceDefinition::setDisplayedName);
		displayedName.setWidth(TEXT_FIELD_BIG.value());
		mainGeneralLayout.addFormItem(displayedName, msg.getMessage("ServiceEditorBase.displayedName"));

		TextField description = new TextField();
		description.setWidth(TEXT_FIELD_BIG.value());
		oauthWebAuthzBinder.forField(description)
				.bind("description");
		mainGeneralLayout.addFormItem(description, msg.getMessage("ServiceEditorBase.description"));

		TextField issuerURI = new TextField();
		issuerURI.setPlaceholder(msg.getMessage("OAuthEditorGeneralTab.issuerURIPlaceholder"));
		issuerURI.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(issuerURI)
				.asRequired()
				.bind("issuerURI");
		mainGeneralLayout.addFormItem(issuerURI, msg.getMessage("OAuthEditorGeneralTab.issuerURI"));

		IntegerField idTokenExp = new IntegerField();
		idTokenExp.setStepButtonsVisible(true);
		configBinder.forField(idTokenExp)
				.asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("idTokenExpiration");
		mainGeneralLayout.addFormItem(idTokenExp, msg.getMessage("OAuthEditorGeneralTab.idTokenExpiration"));

		IntegerField codeTokenExp = new IntegerField();
		codeTokenExp.setStepButtonsVisible(true);
		configBinder.forField(codeTokenExp)
				.asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("codeTokenExpiration");
		mainGeneralLayout.addFormItem(codeTokenExp, msg.getMessage("OAuthEditorGeneralTab.codeTokenExpiration"));

		IntegerField accessTokenExp = new IntegerField();
		accessTokenExp.setStepButtonsVisible(true);
		configBinder.forField(accessTokenExp)
				.asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("accessTokenExpiration");
		mainGeneralLayout.addFormItem(accessTokenExp, msg.getMessage("OAuthEditorGeneralTab.accessTokenExpiration"));

		IntegerField refreshTokenExp = new IntegerField();
		refreshTokenExp.setStepButtonsVisible(true);
		EnumComboBox<RefreshTokenIssuePolicy> refreshTokenIssuePolicy = new EnumComboBox<>(msg::getMessage,
				"OAuthEditorGeneralTab.refreshTokenIssuePolicy.", RefreshTokenIssuePolicy.class,
				RefreshTokenIssuePolicy.NEVER);

		configBinder.forField(refreshTokenIssuePolicy)
				.bind("refreshTokenIssuePolicy");
		mainGeneralLayout.addFormItem(refreshTokenIssuePolicy, msg.getMessage("OAuthEditorGeneralTab.refreshTokenIssuePolicy"));
		refreshTokenIssuePolicy.addValueChangeListener(e ->
		{
			refreshTokenExp.setEnabled(!e.getValue()
					.equals(RefreshTokenIssuePolicy.NEVER));
			refreshScope(e.getValue()
					.equals(RefreshTokenIssuePolicy.OFFLINE_SCOPE_BASED), OIDCScopeValue.OFFLINE_ACCESS.getValue());

		});

		configBinder.forField(refreshTokenExp)
				.asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("refreshTokenExpiration");
		refreshTokenExp.setEnabled(false);
		mainGeneralLayout.addFormItem(refreshTokenExp, msg.getMessage("OAuthEditorGeneralTab.refreshTokenExpiration"))
				.add(htmlTooltipFactory.get(msg.getMessage("OAuthEditorGeneralTab.refreshTokenExpirationDescription")));

		Checkbox refreshTokenRotationForPublicClients = new Checkbox(
				msg.getMessage("OAuthEditorGeneralTab.refreshTokenRotationForPublicClients"));
		configBinder.forField(refreshTokenRotationForPublicClients)
				.bind("refreshTokenRotationForPublicClients");
		mainGeneralLayout.addFormItem(refreshTokenRotationForPublicClients, "");

		IntegerField extendAccessTokenValidity = new IntegerField();
		extendAccessTokenValidity.setStepButtonsVisible(true);

		Checkbox supportExtendAccessTokenValidity = new Checkbox(
				msg.getMessage("OAuthEditorGeneralTab.supportExtendTokenValidity"));
		configBinder.forField(supportExtendAccessTokenValidity)
				.bind("supportExtendTokenValidity");
		mainGeneralLayout.addFormItem(supportExtendAccessTokenValidity, "");
		supportExtendAccessTokenValidity
				.addValueChangeListener(e -> extendAccessTokenValidity.setEnabled(e.getValue()));

		configBinder.forField(extendAccessTokenValidity)
				.asRequired((v, c) ->
				{
					if (supportExtendAccessTokenValidity.getValue())
					{
						return new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null).apply(v, c);
					}
					return ValidationResult.ok();
				})
				.bind("maxExtendAccessTokenValidity");

		extendAccessTokenValidity.setEnabled(false);
		mainGeneralLayout.addFormItem(extendAccessTokenValidity, msg.getMessage("OAuthEditorGeneralTab.maxExtendAccessTokenValidity"));

		Checkbox skipConsentScreen = new Checkbox(msg.getMessage("OAuthEditorGeneralTab.skipConsentScreen"));
		configBinder.forField(skipConsentScreen)
				.bind("skipConsentScreen");
		mainGeneralLayout.addFormItem(skipConsentScreen, "");

		accessTokenFormat = createAccessTokenFormatCombo();
		mainGeneralLayout.addFormItem(accessTokenFormat, msg.getMessage("OAuthEditorGeneralTab.accessTokenFormat"));

		openIDConnect = new Checkbox(msg.getMessage("OAuthEditorGeneralTab.openIDConnect"));
		configBinder.forField(openIDConnect)
				.bind("openIDConnect");
		mainGeneralLayout.addFormItem(openIDConnect, "");

		signingAlg = new ComboBox<>();
		signingAlg.setItems(SigningAlgorithms.values());
		configBinder.forField(signingAlg)
				.bind("signingAlg");
		mainGeneralLayout.addFormItem(signingAlg, msg.getMessage("OAuthEditorGeneralTab.signingAlgorithm"));
		signingAlg.addValueChangeListener(e -> refreshSigningControls());

		credential = new ComboBox<>();
		credential.setItems(credentials);
		configBinder.forField(credential)
				.asRequired((v, c) ->
					validateCredential(v, credential.isEnabled(), signingAlg.getValue() != null ? signingAlg.getValue().toString() : null))
				.bind("credential");

		credential.setEnabled(false);
		mainGeneralLayout.addFormItem(credential, msg.getMessage("OAuthEditorGeneralTab.signingCredential"));

		signingSecret = new TextField();
		signingSecret.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(signingSecret)
				.asRequired((v, c) ->
				{
					JWSAlgorithm alg = JWSAlgorithm.parse(signingAlg.getValue()
							.toString());

					if (signingSecret.isEnabled() && Family.HMAC_SHA.contains(alg))
					{
						if (v == null || v.isEmpty())
						{
							return ValidationResult.error(msg.getMessage("fieldRequired"));
						} else
						{
							if (v.getBytes(StandardCharsets.UTF_8).length * 8 < getBitsLenghtForAlg(alg))
							{
								return ValidationResult.error(msg.getMessage("toShortValue"));
							}
						}
					}

					return ValidationResult.ok();

				})
				.bind("signingSecret");
		signingSecret.setEnabled(false);
		mainGeneralLayout.addFormItem(signingSecret, msg.getMessage("OAuthEditorGeneralTab.signingSecret"));

		tokenExchangeSupport = new Checkbox(msg.getMessage("OAuthEditorGeneralTab.tokenExchangeSupport"));
		configBinder.forField(tokenExchangeSupport)
				.bind("tokenExchangeSupport");
		mainGeneralLayout.addFormItem(tokenExchangeSupport, "")
				.add(htmlTooltipFactory.get(msg.getMessage("OAuthEditorGeneralTab.tokenExchangeSupportDescription")));		
		
		openIDConnect.addValueChangeListener(e ->
		{
			refreshSigningControls();
			refreshScope(e.getValue(), OIDCScopeValue.OPENID.getValue());
		});
		
		tokenExchangeSupport.addValueChangeListener(e ->
		{
			refreshScope(e.getValue(), OAuthSystemScopeProvider.TOKEN_EXCHANGE_SCOPE);
		});

		
		return main;
	}

	private PrivateKey getPrivateKey(String cred) throws EngineException
	{
		return pkiManagement.getCredential(cred).getKey();	
	}
	
	private ValidationResult validateCredential(String credential, boolean isEnabled,  String signingAlg)
	{
		if (signingAlg == null)
		{
			return ValidationResult.ok();
		}
		
		if (isEnabled && (credential == null || credential.isEmpty())
				&& !Family.HMAC_SHA.contains(JWSAlgorithm.parse(signingAlg)))
		{
			return ValidationResult.error(msg.getMessage("fieldRequired"));
		}	
		
		PrivateKey pk;
		try
		{
			pk = getPrivateKey(credential);
		} catch (EngineException e1)
		{
			return ValidationResult.error(msg.getMessage("OAuthEditorGeneralTab.credentialError"));
		}
		if (pk == null)
		{
			return ValidationResult.error(msg.getMessage("OAuthEditorGeneralTab.credentialError"));
		}
		
		if (!(pk instanceof RSAPrivateKey) && Family.RSA.contains(JWSAlgorithm.parse(signingAlg)))
		{
			return ValidationResult.error(msg.getMessage("OAuthEditorGeneralTab.privateKeyError", "RSA", "RS"));
		}
		
		if (!(pk instanceof ECPrivateKey) && Family.EC.contains(JWSAlgorithm.parse(signingAlg)))
		{
			return ValidationResult.error(msg.getMessage("OAuthEditorGeneralTab.privateKeyError", "EC", "ES"));
		}
		

		return ValidationResult.ok();
	}
	
	private void refreshScope(boolean add, String value)
	{
		Optional<OAuthScopeBean> scope = configBinder.getBean()
				.getScopes()
				.stream()
				.filter(s -> s.getName()
						.equals(value))
				.findFirst();
		if (scope.isPresent())
		{
			OAuthScopeBean newScope = new OAuthScopeBean();
			newScope.setAttributes(scope.get()
					.getAttributes());
			newScope.setEnabled(add);
			newScope.setAttributes(scope.get()
					.getAttributes());
			newScope.setDescription(scope.get()
					.getDescription());
			newScope.setName(scope.get()
					.getName());
			scopesGrid.replaceElement(scope.get(), newScope);
		}
	}

	private ComboBox<AccessTokenFormat> createAccessTokenFormatCombo()
	{
		ComboBox<AccessTokenFormat> combo = new ComboBox<>();
		combo.setItems(AccessTokenFormat.values());
		configBinder.forField(combo)
				.bind("accessTokenFormat");
		combo.addValueChangeListener(e ->
		{
			refreshSigningControls();
		});
		return combo;
	}

	private int getBitsLenghtForAlg(JWSAlgorithm alg)
	{
		if (alg.equals(JWSAlgorithm.HS256))
			return 256;
		else if (alg.equals(JWSAlgorithm.HS384))
			return 384;
		return 512;
	}

	private AccordionPanel buildScopesSection()
	{
		List<String> systemScopesNames = systemScopes.stream()
				.map(s -> s.name)
				.collect(Collectors.toList());
		scopesGrid = new GridWithEditorInDetails<>(msg::getMessage, OAuthScopeBean.class,
				() -> new ScopeEditor(msg, attrTypes, systemScopesNames), s -> false,
				s -> s != null && s.getName() != null && systemScopesNames.contains(s.getName()), false);

		Grid.Column<OAuthScopeBean> addGotoEditColumn = scopesGrid
				.addGotoEditColumn(OAuthScopeBean::getName)
				.setHeader(msg.getMessage("OAuthEditorGeneralTab.scopeName"))
				.setResizable(true);
		addGotoEditColumn.setId("name");
		scopesGrid.addCheckboxColumn(OAuthScopeBean::isEnabled)
				.setHeader(msg.getMessage("OAuthEditorGeneralTab.scopeEnabled"))
				.setResizable(true)
				.setAutoWidth(true)
				.setFlexGrow(0);
		scopesGrid.addCheckboxColumn(OAuthScopeBean::isPattern)
				.setHeader(msg.getMessage("OAuthEditorGeneralTab.scopeIsPattern"))
				.setResizable(true)
				.setAutoWidth(true)
				.setFlexGrow(0);
		scopesGrid.addTextColumn(OAuthScopeBean::getDescription)
				.setHeader(msg.getMessage("OAuthEditorGeneralTab.scopeDescription"))
				.setResizable(true)
				.setWidth("30em");
		scopesGrid
				.addTextColumn(s -> s.getAttributes() != null ? String.join(",", s.getAttributes()) : "")
				.setHeader(msg.getMessage("OAuthEditorGeneralTab.scopeAttributes"))
				.setResizable(true)
				.setAutoWidth(true);
		addGotoEditColumn.setComparator((s1, s2) -> compareScopes(systemScopesNames, s1, s2));
		configBinder.forField(scopesGrid)
				.bind("scopes");
		scopesGrid.addValueChangeListener(e -> scopesGrid.sort(addGotoEditColumn));
		scopesGrid.setWidthFull();
		AccordionPanel accordionPanel = new AccordionPanel(msg.getMessage("OAuthEditorGeneralTab.scopes"),
				scopesGrid);
		accordionPanel.setWidthFull();
		return accordionPanel;
	}
	
	private AccordionPanel buildScriptsSection()
	{

		scriptsGrid = new GridWithEditorInDetails<>(msg::getMessage, AuthorizationScriptBean.class, () -> new ScriptEditor(msg, htmlTooltipFactory),
				s -> false, s -> false, false);

		Grid.Column<AuthorizationScriptBean> addGotoEditColumn = scriptsGrid.addGotoEditColumn(AuthorizationScriptBean::getScope)
				.setHeader(msg.getMessage("OAuthEditorGeneralTab.scriptTriggeringScope"))
				.setResizable(true);
		scriptsGrid.addTextColumn(AuthorizationScriptBean::getPath)
				.setHeader(msg.getMessage("OAuthEditorGeneralTab.scriptPath"))
				.setResizable(true);

		configBinder.forField(scriptsGrid)
				.bind("authorizationScripts");
		scriptsGrid.addValueChangeListener(e -> scriptsGrid.sort(addGotoEditColumn));
		scriptsGrid.setWidthFull();
		HorizontalLayout label = new HorizontalLayout(
				new NativeLabel(msg.getMessage("OAuthEditorGeneralTab.authorizationScripts")),
				htmlTooltipFactory.get(msg.getMessage("OAuthEditorGeneralTab.authorizationScriptsDescription")));
		label.setWidthFull();

		AccordionPanel accordionPanel = new AccordionPanel(label, scriptsGrid);
		accordionPanel.setWidthFull();

		return accordionPanel;
	}

	private AccordionPanel buildTrustedUpstremsSection()
	{
		trustedUpstreamAsGrid = new GridWithEditorInDetails<>(msg::getMessage, TrustedUpstreamASBean.class,
				() -> new TrustedUpstreamEditor(msg, certificates, validators), s -> false, false);
		trustedUpstreamAsGrid.setWidthFull();
		trustedUpstreamAsGrid
				.addTextColumn(
						s -> !Strings.isNullOrEmpty(s.getMetadataURL()) ? s.getMetadataURL()
								: s.getIntrospectionEndpointURL()
						)
				.setHeader(msg.getMessage("OAuthEditorGeneralTab.trustedUpstreamASesURL"))
				.setResizable(true)
				.setAutoWidth(true);

		configBinder.forField(trustedUpstreamAsGrid)
				.bind("trustedUpstreamAS");

		AccordionPanel accordionPanel = new AccordionPanel(msg.getMessage("OAuthEditorGeneralTab.trustedUpstreamASes"),
				trustedUpstreamAsGrid);
		accordionPanel.setWidthFull();
		return accordionPanel;
	}

	private int compareScopes(List<String> systemScopesNames, OAuthScopeBean s1, OAuthScopeBean s2)
	{
		if (s1.getName() == null || s2.getName() == null)
			return 1;

		if (systemScopesNames.contains(s1.getName()))
		{
			if (systemScopesNames.contains(s2.getName()))
			{
				return s1.getName()
						.compareTo(s2.getName());
			} else
			{
				return -1;
			}
		} else
		{

			if (!systemScopesNames.contains(s2.getName()))
			{
				return s1.getName()
						.compareTo(s2.getName());
			} else
			{
				return 1;
			}
		}

	}

	private void refreshSigningControls()
	{
		OAuthServiceConfiguration config = configBinder.getBean();
		SigningAlgorithms alg = config.getSigningAlg();
		boolean openid = config.isOpenIDConnect();
		boolean jwtAT = config.getAccessTokenFormat() == AccessTokenFormat.JWT
				|| config.getAccessTokenFormat() == AccessTokenFormat.AS_REQUESTED;

		if (openid || jwtAT)
		{
			signingAlg.setEnabled(true);
			if (alg.toString()
					.startsWith("HS"))
			{
				signingSecret.setEnabled(true);
				credential.setEnabled(false);
			} else
			{
				signingSecret.setEnabled(false);
				credential.setEnabled(true);
			}
		} else
		{
			signingSecret.setEnabled(false);
			credential.setEnabled(false);
			signingAlg.setEnabled(false);
		}
		
		configBinder.getBinding("credential").get().validate();

	}

	public void addNameValueChangeListener(Consumer<String> valueChangeListener)
	{
		name.addValueChangeListener(event ->  valueChangeListener.accept(event.getValue()));
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.COGS;
	}

	@Override
	public String getType()
	{
		return ServiceEditorComponent.ServiceEditorTab.GENERAL.toString();
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("ServiceEditorBase.general");
	}

	Set<String> getScopes()
	{
		return scopesGrid.getValue()
				.stream()
				.map(s -> s.getName())
				.collect(ImmutableSet.toImmutableSet());
	}

	private ValidationResult validatePathForAdd(String path, String path2)
	{
		if (path == null || path.isEmpty())
		{
			return ValidationResult.error(msg.getMessage("fieldRequired"));
		}

		if (usedEndpointsPaths.contains(path) || (path2 != null && path2.equals(path)))
		{
			return ValidationResult.error(msg.getMessage("ServiceEditorBase.usedContextPath"));
		}

		try
		{
			EndpointPathValidator.validateEndpointPath(path, serverContextPaths);

		} catch (WrongArgumentException e)
		{
			return ValidationResult.error(msg.getMessage("ServiceEditorBase.invalidContextPath"));
		}

		return ValidationResult.ok();
	}

	private ValidationResult validatePathForEdit(String path)
	{
		try
		{
			EndpointPathValidator.validateEndpointPath(path);

		} catch (WrongArgumentException e)
		{
			return ValidationResult.error(msg.getMessage("ServiceEditorBase.invalidContextPath"));
		}

		return ValidationResult.ok();
	}

	public static class ScopeEditor extends CustomField<OAuthScopeBean> implements GridWithEditorInDetails.EmbeddedEditor<OAuthScopeBean>
	{
		private final Binder<OAuthScopeBean> binder;
		private final TextField name;
		private final MultiSelectComboBox<String> attributes;
		private boolean blockedEdit = false;
		private final Checkbox enable;
		private final Checkbox pattern;
		private final List<String> systemScopes;

		public ScopeEditor(MessageSource msg, List<String> attrTypes, List<String> systemScopes)
		{
			this.systemScopes = systemScopes;

			binder = new Binder<>(OAuthScopeBean.class);
			name = new TextField();
			name.setWidth(TEXT_FIELD_BIG.value());
			binder.forField(name)
					.asRequired(msg.getMessage("fieldRequired"))
					.withValidator(new NoSpaceValidator(msg::getMessage))
					.withValidator((value, context) ->
					{
						if (!blockedEdit && value != null && systemScopes.contains(value))
						{
							return ValidationResult
									.error(msg.getMessage("OAuthEditorGeneralTab.scopeBlockedToAdd", value));
						}
						return ValidationResult.ok();
					})
					.bind("name");
			enable = new Checkbox(msg.getMessage("OAuthEditorGeneralTab.scopeEnabled"));
			pattern = new Checkbox(msg.getMessage("OAuthEditorGeneralTab.scopeIsPattern"));

			binder.forField(enable)
					.bind("enabled");
			binder.forField(pattern)
					.bind("pattern");

			TextField desc = new TextField();
			desc.setWidthFull();
			binder.forField(desc)
					.bind("description");

			attributes = new CustomValuesMultiSelectComboBox();
			attributes.setWidth(TEXT_FIELD_MEDIUM.value());
			attributes.setPlaceholder(msg.getMessage("typeOrSelect"));
			attributes.setAutoExpand(MultiSelectComboBox.AutoExpandMode.BOTH);
			attributes.setItems(attrTypes);
			binder.forField(attributes)
					.withConverter(List::copyOf, HashSet::new)
					.bind(OAuthScopeBean::getAttributes, OAuthScopeBean::setAttributes);
			FormLayout main = new FormLayout();
			main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
			main.addFormItem(name, msg.getMessage("OAuthEditorGeneralTab.scopeName") + ":");
			main.addFormItem(enable, "");
			main.addFormItem(pattern, "");
			main.addFormItem(desc, msg.getMessage("OAuthEditorGeneralTab.scopeDescription") + ":");
			main.addFormItem(attributes, msg.getMessage("OAuthEditorGeneralTab.scopeAttributes") + ":");
			add(main);
			setSizeFull();
		}

		@Override
		public OAuthScopeBean getValidValue() throws FormValidationException
		{
			if (binder.validate()
					.hasErrors())
			{
				throw new FormValidationException();
			}

			return binder.getBean();
		}

		@Override
		public void setValue(OAuthScopeBean value)
		{
			binder.setBean(value.clone());
			boolean enableDisableblock = value != null && value.getName() != null
					&& OAuthSystemScopeProvider.getScopeNames()
							.contains(value.getName());
			boolean fullBlock = value != null && value.getName() != null && systemScopes.contains(value.getName());
			enable.setReadOnly(enableDisableblock);
			pattern.setReadOnly(fullBlock);
			name.setReadOnly(fullBlock);
			attributes.setEnabled(!fullBlock);
			blockedEdit = fullBlock || enableDisableblock;
		}

		@Override
		protected OAuthScopeBean generateModelValue()
		{
			return null;
		}

		@Override
		protected void setPresentationValue(OAuthScopeBean oAuthScopeBean)
		{

		}
	}
	
	public static class ScriptEditor extends CustomField<AuthorizationScriptBean> implements GridWithEditorInDetails.EmbeddedEditor<AuthorizationScriptBean>
	{		
		private final Binder<AuthorizationScriptBean> binder;
		private final TextField scope;
		private final TextField path;

		
		public ScriptEditor(MessageSource msg, HtmlTooltipFactory htmlTooltipFactory)
		{
			binder = new Binder<>(AuthorizationScriptBean.class);
			scope = new TextField();
			scope.setWidth(TEXT_FIELD_BIG.value());
	
			binder.forField(scope)
					.asRequired(msg.getMessage("fieldRequired"))
					.withValidator(new NoSpaceValidator(msg::getMessage))
					.bind("scope");
			
			path = new TextField();
			path.setWidth(TEXT_FIELD_BIG.value());
			binder.forField(path)
					.asRequired(msg.getMessage("fieldRequired"))
					.withValidator(new NoSpaceValidator(msg::getMessage))
					.bind("path");
			
			FormLayout main = new FormLayout();
			main.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
			main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
			main.addFormItem(scope, msg.getMessage("OAuthEditorGeneralTab.scriptTriggeringScope") + ":")
					.add(htmlTooltipFactory
							.get(msg.getMessage("OAuthEditorGeneralTab.scriptTriggeringScopeDescription")));
			main.addFormItem(path, msg.getMessage("OAuthEditorGeneralTab.scriptPath") + ":");

			add(main);
			setSizeFull();
		}
		
		@Override
		public void setValue(AuthorizationScriptBean value)
		{
			binder.setBean(new AuthorizationScriptBean(value.getScope(), value.getPath()));
		}
		
		@Override
		public AuthorizationScriptBean getValidValue() throws FormValidationException
		{
			if (binder.validate()
					.hasErrors())
			{
				throw new FormValidationException();
			}

			return binder.getBean();
		}
		

		@Override
		protected AuthorizationScriptBean generateModelValue()
		{
			return null;
		}

		@Override
		protected void setPresentationValue(AuthorizationScriptBean newPresentationValue)
		{
			
		}
		
	}

	public static class TrustedUpstreamEditor extends CustomField<TrustedUpstreamASBean> implements GridWithEditorInDetails.EmbeddedEditor<TrustedUpstreamASBean>
	{
		private enum Mode
		{
			manual, metadata
		}

		private final Binder<TrustedUpstreamASBean> binder;
		private final TextField issuerURI;
		private final TextField endpointURL;
		private final TextField metadataURL;
		private final Select<String> certificate;
		private final ComboBox<Mode> mode;

		public TrustedUpstreamEditor(MessageSource msg, Set<String> certificates, Set<String> validators)
		{
			binder = new Binder<>(TrustedUpstreamASBean.class);
			TextField clientId = new TextField();

			clientId.setWidth(TEXT_FIELD_BIG.value());
			binder.forField(clientId)
					.asRequired(msg.getMessage("fieldRequired"))
					.withValidator(new NoSpaceValidator(msg::getMessage))
					.bind("clientId");

			TextField clientSecret = new TextField();
			clientSecret.setWidth(TEXT_FIELD_BIG.value());
			binder.forField(clientSecret)
					.asRequired(msg.getMessage("fieldRequired"))
					.bind("clientSecret");

			certificate = new Select<>();
			certificate.setEmptySelectionAllowed(false);
			certificate.setItems(certificates);
			binder.forField(certificate)
					.asRequired((v, c) ->
					{
						if (certificate.getParent().get().isVisible() && (v == null || v.isEmpty()))
						{
							return ValidationResult.error(msg.getMessage("fieldRequired"));
						}

						return ValidationResult.ok();
					})
					.bind("certificate");

			issuerURI = new TextField();
			issuerURI.setWidth(TEXT_FIELD_BIG.value());

			binder.forField(issuerURI)
					.asRequired((v, c) ->
					{
						if (issuerURI.getParent().get().isVisible() && (v == null || v.isEmpty()))
						{
							return ValidationResult.error(msg.getMessage("fieldRequired"));
						}

						return ValidationResult.ok();
					})
					.bind("issuerURI");

			endpointURL = new TextField();
			endpointURL.setWidth(TEXT_FIELD_BIG.value());
			binder.forField(endpointURL)
					.asRequired((v, c) ->
					{
						if (endpointURL.getParent().get().isVisible() && (v == null || v.isEmpty()))
						{
							return ValidationResult.error(msg.getMessage("fieldRequired"));
						}

						return ValidationResult.ok();
					})
					.bind("introspectionEndpointURL");

			Select<ServerHostnameCheckingMode> clientHostnameChecking = new Select<>();
			clientHostnameChecking.setItems(ServerHostnameCheckingMode.values());
			clientHostnameChecking.setEmptySelectionAllowed(false);
			binder.forField(clientHostnameChecking).bind("clientHostnameChecking");

			Select<String> clientTrustStore = new Select<>();
			clientTrustStore.setWidth(TEXT_FIELD_BIG.value());
			clientTrustStore.setItems(validators);
			clientTrustStore.setEmptySelectionCaption(msg.getMessage("TrustStore.default"));
			binder.forField(clientTrustStore).bind("clientTrustStore");
				
			metadataURL = new TextField();
			metadataURL.setWidth(TEXT_FIELD_BIG.value());
			binder.forField(metadataURL)
					.asRequired((v, c) ->
					{
						if (metadataURL.getParent().get().isVisible() && (v == null || v.isEmpty()))
						{
							return ValidationResult.error(msg.getMessage("fieldRequired"));
						}

						return ValidationResult.ok();
					})
					.bind("metadataURL");
			mode = new EnumComboBox<>(msg::getMessage, "TrustedUpstreamEditor.mode.", Mode.class, Mode.metadata);
			mode.addValueChangeListener(v ->
			{
				metadataURL.getParent().get().setVisible(v.getValue()
						.equals(Mode.metadata));
				issuerURI.getParent().get().setVisible(v.getValue()
						.equals(Mode.manual));
				endpointURL.getParent().get().setVisible(v.getValue()
						.equals(Mode.manual));
				certificate.getParent().get().setVisible(v.getValue()
						.equals(Mode.manual));
				metadataURL.clear();
				issuerURI.clear();
				endpointURL.clear();
				certificate.clear();
			});

			FormLayout main = new FormLayout();
			main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
			main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
			main.addFormItem(clientId, msg.getMessage("TrustedUpstreamEditor.clientId") + ":");
			main.addFormItem(clientSecret, msg.getMessage("TrustedUpstreamEditor.clientSecret") + ":");
			main.addFormItem(clientTrustStore, msg.getMessage("TrustedUpstreamEditor.clientTrustStore")+ ":");
			main.addFormItem(clientHostnameChecking, msg.getMessage("TrustedUpstreamEditor.clientHostnameChecking")+ ":");
			main.addFormItem(mode, msg.getMessage("TrustedUpstreamEditor.mode") + ":");
			main.addFormItem(metadataURL, msg.getMessage("TrustedUpstreamEditor.metadataURL") + ":");
			main.addFormItem(issuerURI, msg.getMessage("TrustedUpstreamEditor.issuerURI") + ":")
					.setVisible(false);
			main.addFormItem(endpointURL, msg.getMessage("TrustedUpstreamEditor.endpointURL") + ":")
					.setVisible(false);
			main.addFormItem(certificate, msg.getMessage("TrustedUpstreamEditor.certificate") + ":")
					.setVisible(false);

			add(main);
			setSizeFull();
		}

		@Override
		protected TrustedUpstreamASBean generateModelValue()
		{
			return binder.getBean();
		}

		@Override
		protected void setPresentationValue(TrustedUpstreamASBean trustedUpstreamASBean)
		{
			binder.setBean(trustedUpstreamASBean);
		}

		@Override
		public TrustedUpstreamASBean getValidValue() throws FormValidationException
		{
			if (binder.validate()
					.hasErrors())
			{
				throw new FormValidationException();
			}

			return binder.getBean();
		}

		@Override
		public void setValue(TrustedUpstreamASBean value)
		{
			if (value != null && Strings.isNullOrEmpty(value.getMetadataURL()))
			{
				mode.setValue(Mode.manual);
			}

			binder.setBean(value.clone());
		}
	}
}
