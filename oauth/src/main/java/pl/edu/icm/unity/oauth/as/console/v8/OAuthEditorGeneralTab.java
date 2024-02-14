/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.v8;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.vaadin.risto.stepper.IntStepper;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSAlgorithm.Family;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.vaadin.data.Binder;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid.Column;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.tooltip.TooltipExtension;
import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.endpoint.EndpointPathValidator;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.SigningAlgorithms;
import pl.edu.icm.unity.oauth.as.OAuthScope;
import pl.edu.icm.unity.oauth.as.OAuthSystemScopeProvider;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithEditorInDetails;
import pl.edu.icm.unity.webui.common.GridWithEditorInDetails.EmbeddedEditor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.chips.ChipsWithFreeText;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.validators.NoSpaceValidator;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.widgets.DescriptionTextField;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;

/**
 * OAuth service editor general tab
 * 
 * @author P.Piernik
 *
 */
class OAuthEditorGeneralTab extends CustomComponent implements EditorTab
{
	private MessageSource msg;
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
	private CheckBox openIDConnect;
	private ComboBox<String> credential;
	private ComboBox<SigningAlgorithms> signingAlg;
	private ComboBox<AccessTokenFormat> accessTokenFormat;
	private TextField signingSecret;
	private GridWithEditorInDetails<OAuthScopeBean> scopesGrid;
	private OutputTranslationProfileFieldFactory profileFieldFactory;
	private SubViewSwitcher subViewSwitcher;
	private TextField name;
	private boolean editMode;
	private List<OAuthScope> systemScopes;
	private GridWithEditorInDetails<TrustedUpstreamASBean> trustedUpstreamAsGrid;
	private Set<String> validators;

	OAuthEditorGeneralTab(MessageSource msg, String serverPrefix, Set<String> serverContextPaths,
			SubViewSwitcher subViewSwitcher, OutputTranslationProfileFieldFactory profileFieldFactory, boolean editMode,
			Set<String> credentials, Collection<IdentityType> identityTypes, List<String> attrTypes,
			List<String> usedEndpointsPaths, List<OAuthScope> systemScopes, Set<String> validators, Set<String> certificates)
	{
		this.msg = msg;

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
	}

	void initUI(Binder<DefaultServiceDefinition> oauthWebAuthzBinder, Binder<DefaultServiceDefinition> oauthTokenBinder,
			Binder<OAuthServiceConfiguration> configBinder)
	{
		this.oauthTokenBinder = oauthTokenBinder;
		this.oauthWebAuthzBinder = oauthWebAuthzBinder;
		this.configBinder = configBinder;
		setCaption(msg.getMessage("ServiceEditorBase.general"));
		setIcon(Images.cogs.getResource());

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);

		CollapsibleLayout buildScopesSection = buildScopesSection();
		main.addComponent(buildHeaderSection());
		main.addComponent(buildScopesSection);
		main.addComponent(buildAdvancedSection());
		main.addComponent(
				profileFieldFactory.getWrappedFieldInstance(subViewSwitcher, configBinder, "translationProfile"));
		main.addComponent(buildTrustedUpstremsSection());

		setCompositionRoot(main);
	}

	private CollapsibleLayout buildAdvancedSection()
	{
		FormLayoutWithFixedCaptionWidth advancedLayout = new FormLayoutWithFixedCaptionWidth();
		advancedLayout.setMargin(false);

		ComboBox<String> idForSub = new ComboBox<>();
		idForSub.setCaption(msg.getMessage("OAuthEditorGeneralTab.identityTypeForSubject"));
		idForSub.setItems(idTypes.stream()
				.map(t -> t.getName()));
		idForSub.setEmptySelectionAllowed(false);
		configBinder.forField(idForSub)
				.bind("identityTypeForSubject");
		advancedLayout.addComponent(idForSub);

		CheckBox allowForWildcardsInAllowedURI = new CheckBox(
				msg.getMessage("OAuthEditorGeneralTab.allowForWildcardsInAllowedURI"));
		configBinder.forField(allowForWildcardsInAllowedURI)
				.bind("allowForWildcardsInAllowedURI");
		advancedLayout.addComponent(allowForWildcardsInAllowedURI);

		CheckBox allowForUnauthenticatedRevocation = new CheckBox(
				msg.getMessage("OAuthEditorGeneralTab.allowForUnauthenticatedRevocation"));
		configBinder.forField(allowForUnauthenticatedRevocation)
				.bind("allowForUnauthenticatedRevocation");
		advancedLayout.addComponent(allowForUnauthenticatedRevocation);

		return new CollapsibleLayout(msg.getMessage("OAuthEditorGeneralTab.advanced"), advancedLayout);
	}

	private Component buildHeaderSection()
	{
		HorizontalLayout main = new HorizontalLayout();
		main.setMargin(new MarginInfo(true, false));

		FormLayoutWithFixedCaptionWidth mainGeneralLayout = new FormLayoutWithFixedCaptionWidth();
		main.addComponent(mainGeneralLayout);

		HorizontalLayout infoLayout = new HorizontalLayout();
		infoLayout.setMargin(new MarginInfo(false, true, false, true));
		infoLayout.setStyleName("u-marginLeftMinus30");
		infoLayout.addStyleName("u-border");
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		infoLayout.addComponent(wrapper);
		wrapper.addComponent(new Label(msg.getMessage("OAuthEditorGeneralTab.importantURLs")));
		FormLayout infoLayoutWrapper = new FormLayout();
		infoLayoutWrapper.setSpacing(false);
		infoLayoutWrapper.setMargin(false);
		wrapper.addComponent(infoLayoutWrapper);

		Label userAuthnEndpointPath = new Label();
		userAuthnEndpointPath.setCaption(msg.getMessage("OAuthEditorGeneralTab.userAuthnEndpointPath"));
		infoLayoutWrapper.addComponent(userAuthnEndpointPath);
		main.addComponent(infoLayout);

		Label tokenEndpointPath = new Label();
		tokenEndpointPath.setCaption(msg.getMessage("OAuthEditorGeneralTab.tokenEndpointPath"));
		infoLayoutWrapper.addComponent(tokenEndpointPath);

		Button metaPath = new Button();

		if (editMode)
		{
			HorizontalLayout l = new HorizontalLayout();
			l.setCaption(msg.getMessage("OAuthEditorGeneralTab.metadataLink"));
			metaPath.setStyleName(Styles.vButtonLink.toString());
			l.addComponent(metaPath);
			infoLayoutWrapper.addComponent(l);
			metaPath.addClickListener(e -> Page.getCurrent()
					.open(metaPath.getCaption(), "_blank", false));
		}

		name = new TextField();
		name.setCaption(msg.getMessage("ServiceEditorBase.name"));
		name.setReadOnly(editMode);
		oauthWebAuthzBinder.forField(name)
				.asRequired()
				.bind("name");
		mainGeneralLayout.addComponent(name);

		TextField tokenContextPath = new TextField();

		TextField webAuthzContextPath = new TextField();
		webAuthzContextPath.setRequiredIndicatorVisible(true);
		webAuthzContextPath.setCaption(msg.getMessage("OAuthEditorGeneralTab.usersAuthnPath"));
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
						userAuthnEndpointPath.setValue("");
					} else
					{
						userAuthnEndpointPath
								.setValue(serverPrefix + v + OAuthAuthzWebEndpoint.OAUTH_CONSUMER_SERVLET_PATH);
					}
					return r;

				})
				.bind("address");
		mainGeneralLayout.addComponent(webAuthzContextPath);
		tokenContextPath.setRequiredIndicatorVisible(true);
		tokenContextPath.setCaption(msg.getMessage("OAuthEditorGeneralTab.clientTokenPath"));
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
						tokenEndpointPath.setValue("");
					} else
					{
						tokenEndpointPath.setValue(serverPrefix + v + OAuthTokenEndpoint.TOKEN_PATH);
						metaPath.setCaption(serverPrefix + v + "/.well-known/openid-configuration");
					}
					return r;

				})
				.bind("address");
		mainGeneralLayout.addComponent(tokenContextPath);

		I18nTextField displayedName = new I18nTextField(msg);
		displayedName.setCaption(msg.getMessage("ServiceEditorBase.displayedName"));
		oauthWebAuthzBinder.forField(displayedName)
				.bind("displayedName");
		mainGeneralLayout.addComponent(displayedName);

		TextField description = new DescriptionTextField(msg);
		oauthWebAuthzBinder.forField(description)
				.bind("description");
		mainGeneralLayout.addComponent(description);

		TextField issuerURI = new TextField();
		issuerURI.setPlaceholder(msg.getMessage("OAuthEditorGeneralTab.issuerURIPlaceholder"));
		issuerURI.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		issuerURI.setCaption(msg.getMessage("OAuthEditorGeneralTab.issuerURI"));
		configBinder.forField(issuerURI)
				.asRequired()
				.bind("issuerURI");
		mainGeneralLayout.addComponent(issuerURI);

		IntStepper idTokenExp = new IntStepper();
		idTokenExp.setWidth(5, Unit.EM);
		idTokenExp.setCaption(msg.getMessage("OAuthEditorGeneralTab.idTokenExpiration"));
		configBinder.forField(idTokenExp)
				.asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("idTokenExpiration");
		mainGeneralLayout.addComponent(idTokenExp);

		IntStepper codeTokenExp = new IntStepper();
		codeTokenExp.setWidth(5, Unit.EM);
		codeTokenExp.setCaption(msg.getMessage("OAuthEditorGeneralTab.codeTokenExpiration"));
		configBinder.forField(codeTokenExp)
				.asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("codeTokenExpiration");
		mainGeneralLayout.addComponent(codeTokenExp);

		IntStepper accessTokenExp = new IntStepper();
		accessTokenExp.setWidth(5, Unit.EM);
		accessTokenExp.setCaption(msg.getMessage("OAuthEditorGeneralTab.accessTokenExpiration"));
		configBinder.forField(accessTokenExp)
				.asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("accessTokenExpiration");
		mainGeneralLayout.addComponent(accessTokenExp);

		IntStepper refreshTokenExp = new IntStepper();
		EnumComboBox<RefreshTokenIssuePolicy> refreshTokenIssuePolicy = new EnumComboBox<>(msg,
				"OAuthEditorGeneralTab.refreshTokenIssuePolicy.", RefreshTokenIssuePolicy.class,
				RefreshTokenIssuePolicy.NEVER);
		refreshTokenExp.setDescription(msg.getMessage("OAuthEditorGeneralTab.refreshTokenExpirationDescription"));
		TooltipExtension.tooltip(refreshTokenExp);
		refreshTokenIssuePolicy.setCaption(msg.getMessage("OAuthEditorGeneralTab.refreshTokenIssuePolicy"));
		refreshTokenIssuePolicy.setEmptySelectionAllowed(false);

		configBinder.forField(refreshTokenIssuePolicy)
				.bind("refreshTokenIssuePolicy");
		mainGeneralLayout.addComponent(refreshTokenIssuePolicy);
		refreshTokenIssuePolicy.addValueChangeListener(e ->
		{
			refreshTokenExp.setEnabled(!e.getValue()
					.equals(RefreshTokenIssuePolicy.NEVER));
			refreshScope(e.getValue()
					.equals(RefreshTokenIssuePolicy.OFFLINE_SCOPE_BASED), OIDCScopeValue.OFFLINE_ACCESS);

		});

		refreshTokenExp.setWidth(5, Unit.EM);
		refreshTokenExp.setCaption(msg.getMessage("OAuthEditorGeneralTab.refreshTokenExpiration"));
		configBinder.forField(refreshTokenExp)
				.asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("refreshTokenExpiration");
		refreshTokenExp.setEnabled(false);
		mainGeneralLayout.addComponent(refreshTokenExp);

		CheckBox refreshTokenRotationForPublicClients = new CheckBox(
				msg.getMessage("OAuthEditorGeneralTab.refreshTokenRotationForPublicClients"));
		configBinder.forField(refreshTokenRotationForPublicClients)
				.bind("refreshTokenRotationForPublicClients");
		mainGeneralLayout.addComponent(refreshTokenRotationForPublicClients);

		IntStepper extendAccessTokenValidity = new IntStepper();

		CheckBox supportExtendAccessTokenValidity = new CheckBox(
				msg.getMessage("OAuthEditorGeneralTab.supportExtendTokenValidity"));
		configBinder.forField(supportExtendAccessTokenValidity)
				.bind("supportExtendTokenValidity");
		mainGeneralLayout.addComponent(supportExtendAccessTokenValidity);
		supportExtendAccessTokenValidity
				.addValueChangeListener(e -> extendAccessTokenValidity.setEnabled(e.getValue()));

		extendAccessTokenValidity.setWidth(5, Unit.EM);
		extendAccessTokenValidity.setCaption(msg.getMessage("OAuthEditorGeneralTab.maxExtendAccessTokenValidity"));
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
		mainGeneralLayout.addComponent(extendAccessTokenValidity);

		CheckBox skipConsentScreen = new CheckBox(msg.getMessage("OAuthEditorGeneralTab.skipConsentScreen"));
		configBinder.forField(skipConsentScreen)
				.bind("skipConsentScreen");
		mainGeneralLayout.addComponent(skipConsentScreen);

		accessTokenFormat = createAccessTokenFormatCombo();
		mainGeneralLayout.addComponent(accessTokenFormat);

		openIDConnect = new CheckBox(msg.getMessage("OAuthEditorGeneralTab.openIDConnect"));
		configBinder.forField(openIDConnect)
				.bind("openIDConnect");
		mainGeneralLayout.addComponent(openIDConnect);

		signingAlg = new ComboBox<>();
		signingAlg.setCaption(msg.getMessage("OAuthEditorGeneralTab.signingAlgorithm"));
		signingAlg.setEmptySelectionAllowed(false);
		signingAlg.setItems(SigningAlgorithms.values());
		configBinder.forField(signingAlg)
				.bind("signingAlg");
		mainGeneralLayout.addComponent(signingAlg);
		signingAlg.addValueChangeListener(e -> refreshSigningControls());

		credential = new ComboBox<>();
		credential.setCaption(msg.getMessage("OAuthEditorGeneralTab.signingCredential"));
		credential.setEmptySelectionAllowed(false);
		credential.setItems(credentials);
		configBinder.forField(credential)
				.asRequired((v, c) ->
				{
					if (credential.isEnabled() && (v == null || v.isEmpty())
							&& !Family.HMAC_SHA.contains(JWSAlgorithm.parse(signingAlg.getValue()
									.toString())))
					{
						return ValidationResult.error(msg.getMessage("fieldRequired"));
					}

					return ValidationResult.ok();

				})
				.bind("credential");

		credential.setEnabled(false);
		mainGeneralLayout.addComponent(credential);

		signingSecret = new TextField();
		signingSecret.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		signingSecret.setCaption(msg.getMessage("OAuthEditorGeneralTab.signingSecret"));
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
		mainGeneralLayout.addComponent(signingSecret);

		openIDConnect.addValueChangeListener(e ->
		{
			refreshSigningControls();
			refreshScope(e.getValue(), OIDCScopeValue.OPENID);
		});

		
		return main;
	}

	private void refreshScope(boolean add, OIDCScopeValue value)
	{
		Optional<OAuthScopeBean> scope = configBinder.getBean()
				.getScopes()
				.stream()
				.filter(s -> s.getName()
						.equals(value.getValue()))
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
		combo.setCaption(msg.getMessage("OAuthEditorGeneralTab.accessTokenFormat"));
		combo.setEmptySelectionAllowed(false);
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

	private CollapsibleLayout buildScopesSection()
	{
		VerticalLayout scopesLayout = new VerticalLayout();
		scopesLayout.setMargin(false);

		List<String> systemScopesNames = systemScopes.stream()
				.map(s -> s.name)
				.collect(Collectors.toList());
		scopesGrid = new GridWithEditorInDetails<>(msg, OAuthScopeBean.class,
				() -> new ScopeEditor(msg, attrTypes, systemScopesNames), s -> false,
				s -> s != null && s.getName() != null && systemScopesNames.contains(s.getName()), false);
		Column<OAuthScopeBean, Component> addGotoEditColumn = scopesGrid
				.addGotoEditColumn(s -> s.getName(), msg.getMessage("OAuthEditorGeneralTab.scopeName"), 10)
				.setWidth(220);
		addGotoEditColumn.setResizable(true);
		addGotoEditColumn.setId("name");
		scopesGrid.addCheckboxColumn(s -> s.isEnabled(), msg.getMessage("OAuthEditorGeneralTab.scopeEnabled"), 10)
				.setResizable(true)
				.setWidth(60);
		scopesGrid.addTextColumn(s -> s.getDescription(), msg.getMessage("OAuthEditorGeneralTab.scopeDescription"), 30)
				.setResizable(true);
		scopesGrid
				.addTextColumn(s -> s.getAttributes() != null ? String.join(",", s.getAttributes()) : "",
						msg.getMessage("OAuthEditorGeneralTab.scopeAttributes"), 30)
				.setResizable(true);
		scopesGrid.setMinHeightByRow(12);
		addGotoEditColumn.setComparator((s1, s2) -> compareScopes(systemScopesNames, s1, s2));
		configBinder.forField(scopesGrid)
				.bind("scopes");
		scopesLayout.addComponent(scopesGrid);
		scopesGrid.addValueChangeListener(e ->
		{
			scopesGrid.sort(addGotoEditColumn.getId());
		});
		return new CollapsibleLayout(msg.getMessage("OAuthEditorGeneralTab.scopes"), scopesLayout);
	}
	
	private CollapsibleLayout buildTrustedUpstremsSection()
	{
		VerticalLayout upstreamsLayout = new VerticalLayout();
		upstreamsLayout.setMargin(false);

		trustedUpstreamAsGrid = new GridWithEditorInDetails<>(msg, TrustedUpstreamASBean.class,
				() -> new TrustedUpstreamEditor(msg, certificates, validators), s -> false, false);
		trustedUpstreamAsGrid
				.addTextColumn(
						s -> !Strings.isNullOrEmpty(s.getMetadataURL()) ? s.getMetadataURL()
								: s.getIntrospectionEndpointURL(),
						msg.getMessage("OAuthEditorGeneralTab.trustedUpstreamASesURL"), 30)
				.setResizable(true);
		trustedUpstreamAsGrid.setMinHeightByRow(12);
		
		configBinder.forField(trustedUpstreamAsGrid)
				.bind("trustedUpstreamAS");
		upstreamsLayout.addComponent(trustedUpstreamAsGrid);

		return new CollapsibleLayout(msg.getMessage("OAuthEditorGeneralTab.trustedUpstreamASes"), upstreamsLayout);
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

	}

	public void addNameValueChangeListener(ValueChangeListener<String> valueChangeListener)
	{
		name.addValueChangeListener(valueChangeListener);
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.GENERAL.toString();
	}

	@Override
	public CustomComponent getComponent()
	{
		return this;
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

	public class ScopeEditor extends CustomComponent implements EmbeddedEditor<OAuthScopeBean>
	{
		private Binder<OAuthScopeBean> binder;
		private TextField name;
		private ChipsWithFreeText attributes;
		private boolean blockedEdit = false;
		private CheckBox enable;
		private List<String> systemScopes;

		public ScopeEditor(MessageSource msg, List<String> attrTypes, List<String> systemScopes)
		{
			this.systemScopes = systemScopes;

			binder = new Binder<>(OAuthScopeBean.class);
			name = new TextField();
			name.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
			name.setCaption(msg.getMessage("OAuthEditorGeneralTab.scopeName") + ":");
			binder.forField(name)
					.asRequired()
					.withValidator(new NoSpaceValidator(msg))
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
			enable = new CheckBox();
			enable.setCaption(msg.getMessage("OAuthEditorGeneralTab.scopeEnabled"));
			binder.forField(enable)
					.bind("enabled");

			TextField desc = new TextField();
			desc.setWidth(100, Unit.PERCENTAGE);

			desc.setCaption(msg.getMessage("OAuthEditorGeneralTab.scopeDescription") + ":");
			binder.forField(desc)
					.bind("description");

			attributes = new ChipsWithFreeText(msg);
			attributes.setCaption(msg.getMessage("OAuthEditorGeneralTab.scopeAttributes") + ":");
			attributes.setItems(attrTypes);
			binder.forField(attributes)
					.bind("attributes");
			FormLayout main = new FormLayout();
			main.setMargin(new MarginInfo(false, true, false, false));
			main.addComponent(name);
			main.addComponent(enable);
			main.addComponent(desc);
			main.addComponent(attributes);
			setCompositionRoot(main);
			setSizeFull();
		}

		@Override
		public OAuthScopeBean getValue() throws FormValidationException
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
			name.setReadOnly(fullBlock);
			attributes.setEnabled(!fullBlock);
			blockedEdit = fullBlock || enableDisableblock;
		}
	}

	public static class TrustedUpstreamEditor extends CustomComponent implements EmbeddedEditor<TrustedUpstreamASBean>
	{
		private enum Mode
		{
			manual, metadata
		};

		private Binder<TrustedUpstreamASBean> binder;
		private TextField clientId;
		private TextField clientSecret;

		private TextField issuerURI;
		private TextField endpointURL;
		private TextField metadataURL;
		private ComboBox<String> certificate;
		private ComboBox<Mode> mode;
		private ComboBox<ServerHostnameCheckingMode> clientHostnameChecking;
		private ComboBox<String> clientTrustStore; 
		public TrustedUpstreamEditor(MessageSource msg, Set<String> certificates, Set<String> validators)
		{
			binder = new Binder<>(TrustedUpstreamASBean.class);
			clientId = new TextField();

			clientId.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
			clientId.setCaption(msg.getMessage("TrustedUpstreamEditor.clientId") + ":");
			binder.forField(clientId)
					.asRequired()
					.withValidator(new NoSpaceValidator(msg))
					.bind("clientId");

			clientSecret = new TextField();
			clientSecret.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
			clientSecret.setCaption(msg.getMessage("TrustedUpstreamEditor.clientSecret") + ":");
			binder.forField(clientSecret)
					.asRequired()
					.bind("clientSecret");

			certificate = new ComboBox<>();
			certificate.setCaption(msg.getMessage("TrustedUpstreamEditor.certificate") + ":");
			certificate.setEmptySelectionAllowed(false);
			certificate.setItems(certificates);
			certificate.setVisible(false);
			binder.forField(certificate)
					.asRequired((v, c) ->
					{
						if (certificate.isVisible() && (v == null || v.isEmpty()))
						{
							return ValidationResult.error(msg.getMessage("fieldRequired"));
						}

						return ValidationResult.ok();
					})
					.bind("certificate");

			issuerURI = new TextField();
			issuerURI.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
			issuerURI.setCaption(msg.getMessage("TrustedUpstreamEditor.issuerURI") + ":");
			issuerURI.setVisible(false);

			binder.forField(issuerURI)
					.asRequired((v, c) ->
					{
						if (issuerURI.isVisible() && (v == null || v.isEmpty()))
						{
							return ValidationResult.error(msg.getMessage("fieldRequired"));
						}

						return ValidationResult.ok();
					})
					.bind("issuerURI");

			endpointURL = new TextField();
			endpointURL.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
			endpointURL.setCaption(msg.getMessage("TrustedUpstreamEditor.endpointURL") + ":");
			endpointURL.setVisible(false);
			binder.forField(endpointURL)
					.asRequired((v, c) ->
					{
						if (endpointURL.isVisible() && (v == null || v.isEmpty()))
						{
							return ValidationResult.error(msg.getMessage("fieldRequired"));
						}

						return ValidationResult.ok();
					})
					.bind("introspectionEndpointURL");

			clientHostnameChecking = new ComboBox<>(
					msg.getMessage("TrustedUpstreamEditor.clientHostnameChecking")+ ":");
			clientHostnameChecking.setItems(ServerHostnameCheckingMode.values());
			clientHostnameChecking.setEmptySelectionAllowed(false);
			binder.forField(clientHostnameChecking).bind("clientHostnameChecking");

			clientTrustStore = new ComboBox<>(
					msg.getMessage("TrustedUpstreamEditor.clientTrustStore")+ ":");
			clientTrustStore.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
			clientTrustStore.setItems(validators);
			clientTrustStore.setEmptySelectionCaption(msg.getMessage("TrustStore.default"));
			binder.forField(clientTrustStore).bind("clientTrustStore");
				
			metadataURL = new TextField();
			metadataURL.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
			metadataURL.setCaption(msg.getMessage("TrustedUpstreamEditor.metadataURL") + ":");
			binder.forField(metadataURL)
					.asRequired((v, c) ->
					{
						if (metadataURL.isVisible() && (v == null || v.isEmpty()))
						{
							return ValidationResult.error(msg.getMessage("fieldRequired"));
						}

						return ValidationResult.ok();
					})
					.bind("metadataURL");
			mode = new EnumComboBox<>(msg, "TrustedUpstreamEditor.mode.", Mode.class, Mode.metadata);
			mode.setCaption(msg.getMessage("TrustedUpstreamEditor.mode") + ":");
			mode.addValueChangeListener(v ->
			{
				metadataURL.setVisible(v.getValue()
						.equals(Mode.metadata));
				issuerURI.setVisible(v.getValue()
						.equals(Mode.manual));
				endpointURL.setVisible(v.getValue()
						.equals(Mode.manual));
				certificate.setVisible(v.getValue()
						.equals(Mode.manual));
				metadataURL.clear();
				issuerURI.clear();
				endpointURL.clear();
				certificate.clear();
			});

			FormLayout main = FormLayoutWithFixedCaptionWidth.withMediumCaptions();
			main.setMargin(false);
			main.addComponent(clientId);
			main.addComponent(clientSecret);
			main.addComponent(clientTrustStore);
			main.addComponent(clientHostnameChecking);
			main.addComponent(mode);
			main.addComponent(metadataURL);
			main.addComponent(issuerURI);
			main.addComponent(endpointURL);
			main.addComponent(certificate);

			setCompositionRoot(main);
			setSizeFull();
		}

		@Override
		public TrustedUpstreamASBean getValue() throws FormValidationException
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
