/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.vaadin.risto.stepper.IntStepper;

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
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.tooltip.TooltipExtension;
import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.endpoint.EndpointPathValidator;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.SigningAlgorithms;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.types.basic.IdentityType;
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
	private static Set<String> SCOPES_BLOCKED_TO_EDIT = Collections
			.unmodifiableSet(Set.of(OIDCScopeValue.OPENID.toString(), OIDCScopeValue.OFFLINE_ACCESS.toString()));

	private MessageSource msg;
	private Binder<DefaultServiceDefinition> oauthWebAuthzBinder;
	private Binder<DefaultServiceDefinition> oauthTokenBinder;
	private Binder<OAuthServiceConfiguration> configBinder;
	private Set<String> credentials;
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
	private GridWithEditorInDetails<OAuthScope> scopesGrid;
	private OutputTranslationProfileFieldFactory profileFieldFactory;
	private SubViewSwitcher subViewSwitcher;
	private TextField name;
	private boolean editMode;

	OAuthEditorGeneralTab(MessageSource msg, String serverPrefix, Set<String> serverContextPaths,
			SubViewSwitcher subViewSwitcher, OutputTranslationProfileFieldFactory profileFieldFactory, boolean editMode,
			Set<String> credentials, Collection<IdentityType> identityTypes, List<String> attrTypes,
			List<String> usedEndpointsPaths)
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
		main.addComponent(buildHeaderSection());
		main.addComponent(buildScopesSection());
		main.addComponent(buildAdvancedSection());
		main.addComponent(
				profileFieldFactory.getWrappedFieldInstance(subViewSwitcher, configBinder, "translationProfile"));

		setCompositionRoot(main);
	}

	private CollapsibleLayout buildAdvancedSection()
	{
		FormLayoutWithFixedCaptionWidth advancedLayout = new FormLayoutWithFixedCaptionWidth();
		advancedLayout.setMargin(false);

		ComboBox<String> idForSub = new ComboBox<>();
		idForSub.setCaption(msg.getMessage("OAuthEditorGeneralTab.identityTypeForSubject"));
		idForSub.setItems(idTypes.stream().map(t -> t.getName()));
		idForSub.setEmptySelectionAllowed(false);
		configBinder.forField(idForSub).bind("identityTypeForSubject");
		advancedLayout.addComponent(idForSub);

		CheckBox allowForWildcardsInAllowedURI = new CheckBox(
				msg.getMessage("OAuthEditorGeneralTab.allowForWildcardsInAllowedURI"));
		configBinder.forField(allowForWildcardsInAllowedURI).bind("allowForWildcardsInAllowedURI");
		advancedLayout.addComponent(allowForWildcardsInAllowedURI);

		CheckBox allowForUnauthenticatedRevocation = new CheckBox(
				msg.getMessage("OAuthEditorGeneralTab.allowForUnauthenticatedRevocation"));
		configBinder.forField(allowForUnauthenticatedRevocation).bind("allowForUnauthenticatedRevocation");
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
			metaPath.addClickListener(e ->
			{
				Page.getCurrent().open(metaPath.getCaption(), "_blank", false);
			});
		}

		name = new TextField();
		name.setCaption(msg.getMessage("ServiceEditorBase.name"));
		name.setReadOnly(editMode);
		oauthWebAuthzBinder.forField(name).asRequired().bind("name");
		mainGeneralLayout.addComponent(name);

		TextField tokenContextPath = new TextField();

		TextField webAuthzContextPath = new TextField();
		webAuthzContextPath.setRequiredIndicatorVisible(true);
		webAuthzContextPath.setCaption(msg.getMessage("OAuthEditorGeneralTab.usersAuthnPath"));
		webAuthzContextPath.setReadOnly(editMode);
		webAuthzContextPath.setPlaceholder("/oauth");
		oauthWebAuthzBinder.forField(webAuthzContextPath).withValidator((v, c) ->
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
				userAuthnEndpointPath.setValue(serverPrefix + v + OAuthAuthzWebEndpoint.OAUTH_CONSUMER_SERVLET_PATH);
			}
			return r;

		}).bind("address");
		mainGeneralLayout.addComponent(webAuthzContextPath);
		tokenContextPath.setRequiredIndicatorVisible(true);
		tokenContextPath.setCaption(msg.getMessage("OAuthEditorGeneralTab.clientTokenPath"));
		tokenContextPath.setPlaceholder("/oauth-token");
		tokenContextPath.setReadOnly(editMode);
		oauthTokenBinder.forField(tokenContextPath).withValidator((v, c) ->
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

		}).bind("address");
		mainGeneralLayout.addComponent(tokenContextPath);

		I18nTextField displayedName = new I18nTextField(msg);
		displayedName.setCaption(msg.getMessage("ServiceEditorBase.displayedName"));
		oauthWebAuthzBinder.forField(displayedName).bind("displayedName");
		mainGeneralLayout.addComponent(displayedName);

		TextField description = new DescriptionTextField(msg);
		oauthWebAuthzBinder.forField(description).bind("description");
		mainGeneralLayout.addComponent(description);

		TextField issuerURI = new TextField();
		issuerURI.setPlaceholder(msg.getMessage("OAuthEditorGeneralTab.issuerURIPlaceholder"));
		issuerURI.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		issuerURI.setCaption(msg.getMessage("OAuthEditorGeneralTab.issuerURI"));
		configBinder.forField(issuerURI).asRequired().bind("issuerURI");
		mainGeneralLayout.addComponent(issuerURI);

		IntStepper idTokenExp = new IntStepper();
		idTokenExp.setWidth(5, Unit.EM);
		idTokenExp.setCaption(msg.getMessage("OAuthEditorGeneralTab.idTokenExpiration"));
		configBinder.forField(idTokenExp).asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("idTokenExpiration");
		mainGeneralLayout.addComponent(idTokenExp);

		IntStepper codeTokenExp = new IntStepper();
		codeTokenExp.setWidth(5, Unit.EM);
		codeTokenExp.setCaption(msg.getMessage("OAuthEditorGeneralTab.codeTokenExpiration"));
		configBinder.forField(codeTokenExp).asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("codeTokenExpiration");
		mainGeneralLayout.addComponent(codeTokenExp);

		IntStepper accessTokenExp = new IntStepper();
		accessTokenExp.setWidth(5, Unit.EM);
		accessTokenExp.setCaption(msg.getMessage("OAuthEditorGeneralTab.accessTokenExpiration"));
		configBinder.forField(accessTokenExp).asRequired(msg.getMessage("notAPositiveNumber"))
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

		configBinder.forField(refreshTokenIssuePolicy).bind("refreshTokenIssuePolicy");
		mainGeneralLayout.addComponent(refreshTokenIssuePolicy);
		refreshTokenIssuePolicy.addValueChangeListener(e ->
		{
			refreshTokenExp.setEnabled(!e.getValue().equals(RefreshTokenIssuePolicy.NEVER));
			refreshScope(e.getValue().equals(RefreshTokenIssuePolicy.OFFLINE_SCOPE_BASED),
					OIDCScopeValue.OFFLINE_ACCESS,
					msg.getMessage("OAuthEditorGeneralTab.defaultOfflineAccessScopeDesc"));

		});

		refreshTokenExp.setWidth(5, Unit.EM);
		refreshTokenExp.setCaption(msg.getMessage("OAuthEditorGeneralTab.refreshTokenExpiration"));
		configBinder.forField(refreshTokenExp).asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("refreshTokenExpiration");
		refreshTokenExp.setEnabled(false);
		mainGeneralLayout.addComponent(refreshTokenExp);

		IntStepper extendAccessTokenValidity = new IntStepper();

		CheckBox supportExtendAccessTokenValidity = new CheckBox(
				msg.getMessage("OAuthEditorGeneralTab.supportExtendTokenValidity"));
		configBinder.forField(supportExtendAccessTokenValidity).bind("supportExtendTokenValidity");
		mainGeneralLayout.addComponent(supportExtendAccessTokenValidity);
		supportExtendAccessTokenValidity.addValueChangeListener(e ->
		{
			extendAccessTokenValidity.setEnabled(e.getValue());
		});

		extendAccessTokenValidity.setWidth(5, Unit.EM);
		extendAccessTokenValidity.setCaption(msg.getMessage("OAuthEditorGeneralTab.maxExtendAccessTokenValidity"));
		configBinder.forField(extendAccessTokenValidity).asRequired((v, c) ->
		{
			if (supportExtendAccessTokenValidity.getValue())
			{
				return new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null).apply(v, c);
			}
			return ValidationResult.ok();
		}).bind("maxExtendAccessTokenValidity");

		extendAccessTokenValidity.setEnabled(false);
		mainGeneralLayout.addComponent(extendAccessTokenValidity);

		CheckBox skipConsentScreen = new CheckBox(msg.getMessage("OAuthEditorGeneralTab.skipConsentScreen"));
		configBinder.forField(skipConsentScreen).bind("skipConsentScreen");
		mainGeneralLayout.addComponent(skipConsentScreen);

		accessTokenFormat = createAccessTokenFormatCombo();
		mainGeneralLayout.addComponent(accessTokenFormat);

		openIDConnect = new CheckBox(msg.getMessage("OAuthEditorGeneralTab.openIDConnect"));
		configBinder.forField(openIDConnect).bind("openIDConnect");
		mainGeneralLayout.addComponent(openIDConnect);

		signingAlg = new ComboBox<>();
		signingAlg.setCaption(msg.getMessage("OAuthEditorGeneralTab.signingAlgorithm"));
		signingAlg.setEmptySelectionAllowed(false);
		signingAlg.setItems(SigningAlgorithms.values());
		configBinder.forField(signingAlg).bind("signingAlg");
		mainGeneralLayout.addComponent(signingAlg);
		signingAlg.addValueChangeListener(e ->
		{
			refreshSigningControls();
		});

		credential = new ComboBox<>();
		credential.setCaption(msg.getMessage("OAuthEditorGeneralTab.signingCredential"));
		credential.setEmptySelectionAllowed(false);
		credential.setItems(credentials);
		configBinder.forField(credential).asRequired((v, c) ->
		{
			if (credential.isEnabled() && (v == null || v.isEmpty())
					&& !Family.HMAC_SHA.contains(JWSAlgorithm.parse(signingAlg.getValue().toString())))
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}

			return ValidationResult.ok();

		}).bind("credential");

		credential.setEnabled(false);
		mainGeneralLayout.addComponent(credential);

		signingSecret = new TextField();
		signingSecret.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		signingSecret.setCaption(msg.getMessage("OAuthEditorGeneralTab.signingSecret"));
		configBinder.forField(signingSecret).asRequired((v, c) ->
		{
			JWSAlgorithm alg = JWSAlgorithm.parse(signingAlg.getValue().toString());

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

		}).bind("signingSecret");
		signingSecret.setEnabled(false);
		mainGeneralLayout.addComponent(signingSecret);

		openIDConnect.addValueChangeListener(e ->
		{
			refreshSigningControls();
			refreshScope(e.getValue(), OIDCScopeValue.OPENID,
					msg.getMessage("OAuthEditorGeneralTab.defaultOpenidScopeDesc"));
		});

		return main;
	}

	private void refreshScope(boolean add, OIDCScopeValue value, String message)
	{
		if (add)
		{
			OAuthScope openidScope = configBinder.getBean().getScopes().stream()
					.filter(s -> s.getName().equals(value.toString())).findFirst().orElse(null);
			if (openidScope == null)
			{
				openidScope = new OAuthScope();
				openidScope.setName(value.toString());
				openidScope.setDescription(message);
				scopesGrid.addElement(openidScope);
			}
		} else
		{

			List<OAuthScope> openidScopes = configBinder.getBean().getScopes().stream()
					.filter(s -> s.getName().equals(value.toString())).collect(Collectors.toList());
			if (!openidScopes.isEmpty())
			{
				openidScopes.forEach(s -> scopesGrid.removeElement(s));
			}
		}
	}

	private ComboBox<AccessTokenFormat> createAccessTokenFormatCombo()
	{
		ComboBox<AccessTokenFormat> combo = new ComboBox<>();
		combo.setCaption(msg.getMessage("OAuthEditorGeneralTab.accessTokenFormat"));
		combo.setEmptySelectionAllowed(false);
		combo.setItems(AccessTokenFormat.values());
		configBinder.forField(combo).bind("accessTokenFormat");
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

		scopesGrid = new GridWithEditorInDetails<>(msg, OAuthScope.class, () -> new ScopeEditor(msg, attrTypes),
				s -> false, s -> s != null && s.getName() != null && (SCOPES_BLOCKED_TO_EDIT.contains(s.getName())),
				true);
		scopesGrid.addGotoEditColumn(s -> s.getName(), msg.getMessage("OAuthEditorGeneralTab.scopeName"), 10);
		scopesGrid.addTextColumn(s -> s.getDescription(), msg.getMessage("OAuthEditorGeneralTab.scopeDescription"), 10);
		scopesGrid.addTextColumn(s -> s.getAttributes() != null ? String.join(",", s.getAttributes()) : "",
				msg.getMessage("OAuthEditorGeneralTab.scopeAttributes"), 10);
		scopesGrid.setMinHeightByRow(7);
		configBinder.forField(scopesGrid).bind("scopes");
		scopesLayout.addComponent(scopesGrid);

		return new CollapsibleLayout(msg.getMessage("OAuthEditorGeneralTab.scopes"), scopesLayout);
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
			if (alg.toString().startsWith("HS"))
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

	public class ScopeEditor extends CustomComponent implements EmbeddedEditor<OAuthScope>
	{
		private Binder<OAuthScope> binder;
		private TextField name;
		private ChipsWithFreeText attributes;
		private boolean blockedEdit = false;

		public ScopeEditor(MessageSource msg, List<String> attrTypes)
		{
			binder = new Binder<>(OAuthScope.class);
			name = new TextField();
			name.setCaption(msg.getMessage("OAuthEditorGeneralTab.scopeName") + ":");
			binder.forField(name).asRequired().withValidator(new NoSpaceValidator(msg))
					.withValidator((value, context) ->
					{
						if (!blockedEdit && value != null && SCOPES_BLOCKED_TO_EDIT.contains(value))
						{
							return ValidationResult.error(msg.getMessage("OAuthEditorGeneralTab.scopeBlockedToAdd", value));
						}
						return ValidationResult.ok();
					}	
					).bind("name");

			TextField desc = new TextField();
			desc.setCaption(msg.getMessage("OAuthEditorGeneralTab.scopeDescription") + ":");
			binder.forField(desc).bind("description");

			attributes = new ChipsWithFreeText(msg);
			attributes.setCaption(msg.getMessage("OAuthEditorGeneralTab.scopeAttributes") + ":");
			attributes.setItems(attrTypes);
			binder.forField(attributes).bind("attributes");
			FormLayout main = new FormLayout();
			main.setMargin(false);
			main.addComponent(name);
			main.addComponent(desc);
			main.addComponent(attributes);
			setCompositionRoot(main);
			setSizeFull();
		}

		@Override
		public OAuthScope getValue() throws FormValidationException
		{
			if (binder.validate().hasErrors())
			{
				throw new FormValidationException();
			}

			return binder.getBean();
		}

		@Override
		public void setValue(OAuthScope value)
		{
			binder.setBean(value);
			boolean block = value != null && value.getName() != null && SCOPES_BLOCKED_TO_EDIT.contains(value.getName());
			name.setEnabled(!block);
			attributes.setEnabled(!block);
			blockedEdit = block;
		}
	}

}
