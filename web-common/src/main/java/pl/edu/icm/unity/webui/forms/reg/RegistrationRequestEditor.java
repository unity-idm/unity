/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec.AuthnGridSettings;
import pl.edu.icm.unity.types.registration.FormLayoutUtils;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationCapable;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.column.AuthNOption;
import pl.edu.icm.unity.webui.authn.column.AuthNPanelFactory;
import pl.edu.icm.unity.webui.authn.column.AuthnsGridWidget;
import pl.edu.icm.unity.webui.authn.column.FirstFactorAuthNPanel;
import pl.edu.icm.unity.webui.authn.column.SearchComponent;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistryV8;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistryV8;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementRepresentationBuilderV8;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.RegistrationLayoutsContainer;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;
import pl.edu.icm.unity.webui.forms.URLQueryPrefillCreator;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreatorV8.InvitationCodeConsumer;

/**
 * Generates a UI based on a given registration form. User can fill the form and a request is returned.
 * The class verifies if the data obtained from an upstream IdP is complete wrt requirements of the form.
 * <p>
 * Objects of this class should be typically created using {@link RequestEditorCreatorV8}, so that the
 * registration code is collected appropriately.
 * @author K. Benedyczak
 */
public class RegistrationRequestEditor extends BaseRequestEditor<RegistrationRequest>
{
	private static final String INVITATION_EMAIL_VAR = "invitationEmail";
	
	enum Stage {FIRST, SECOND}
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RegistrationRequestEditor.class);
	private RegistrationForm form;
	
	private TextField registrationCode;
	private CaptchaComponent captcha;
	private String regCodeProvided;
	private ResolvedInvitationParam invitation;
	private AuthenticatorSupportService authnSupport;
	private Map<AuthenticationOptionKey, AuthNOption> externalSignupOptions;
	private InvitationCodeConsumer onLocalSignupHandler;
	private FormLayout effectiveLayout;
	private Stage stage;
	private RegistrationLayoutsContainer layoutContainer;
	private URLQueryPrefillCreator urlQueryPrefillCreator;
	private final boolean enableRemoteRegistration;
	private final SwitchToEnquiryComponentProviderV8 toEnquirySwitchLabelProvider;
	private final AuthenticationOptionKey authnOptionKey;

	/**
	 * Note - the two managers must be insecure, if the form is used in not-authenticated context, 
	 * what is possible for registration form.
	 * @param toEnquirySwitchLabelProvider 
	 */
	public RegistrationRequestEditor(MessageSource msg, RegistrationForm form,
			RemotelyAuthenticatedPrincipal remotelyAuthenticated,
			IdentityEditorRegistryV8 identityEditorRegistry,
			CredentialEditorRegistryV8 credentialEditorRegistry,
			AttributeHandlerRegistryV8 attributeHandlerRegistry,
			AttributeTypeManagement aTypeMan, CredentialManagement credMan,
			GroupsManagement groupsMan, ImageAccessService imageAccessService,
			String registrationCode, ResolvedInvitationParam invitation, 
			AuthenticatorSupportService authnSupport, 
			URLQueryPrefillCreator urlQueryPrefillCreator, 
			PolicyAgreementRepresentationBuilderV8 policyAgreementsRepresentationBuilder,
			SwitchToEnquiryComponentProviderV8 toEnquirySwitchLabelProvider,
			boolean enableRemoteRegistration,
			AuthenticationOptionKey authnOptionKey)
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, aTypeMan, credMan, groupsMan, imageAccessService,
				policyAgreementsRepresentationBuilder);
		this.form = form;
		this.regCodeProvided = registrationCode;
		this.invitation = invitation;
		this.authnSupport = authnSupport;
		this.urlQueryPrefillCreator = urlQueryPrefillCreator;
		this.enableRemoteRegistration = enableRemoteRegistration;
		this.toEnquirySwitchLabelProvider =  toEnquirySwitchLabelProvider;
		this.authnOptionKey = authnOptionKey;
	}
	
	public void showFirstStage(InvitationCodeConsumer onLocalSignupHandler) throws AuthenticationException
	{
		this.effectiveLayout = form.getEffectivePrimaryFormLayout(msg);
		this.onLocalSignupHandler = onLocalSignupHandler;
		this.stage = Stage.FIRST;
		if (form.isLocalSignupEnabled()) //when we have only remote signup enabled, validation must be deferred to 2nd stage
			validateMandatoryRemoteInput(); 
		initUI();
	}
	
	public void showSecondStage(boolean withCredentials) throws AuthenticationException
	{
		this.effectiveLayout = withCredentials ? form.getEffectiveSecondaryFormLayout(msg) 
				: form.getEffectiveSecondaryFormLayoutWithoutCredentials(msg);
		this.stage = Stage.SECOND;
		validateMandatoryRemoteInput();
		initUI();
	}
	
	public AuthenticationOptionKey getAuthnOptionKey()
	{
		return authnOptionKey;
	}

	@Override
	public RegistrationRequest getRequest(boolean withCredentials) throws FormValidationException
	{
		//defensive check: if we have local signup button then submission makes no sense - 
		//we need to go to 2nd stage first. 
		if (FormLayoutUtils.hasLocalSignupButton(effectiveLayout))
			throw new FormValidationException(msg.getMessage("RegistrationRequest.continueRegistration"));
		
		RegistrationRequest ret = new RegistrationRequest();
		FormErrorStatus status = new FormErrorStatus();

		super.fillRequest(ret, status, withCredentials);
		
		setRequestCode(ret, status);
		if (captcha != null)
		{
			try
			{
				captcha.verify();
			} catch (WrongArgumentException e)
			{
				status.hasFormException = true;
			}
		}
		
		if (status.hasFormException)
			throw new FormValidationException(status.errorMsg);
		
		return ret;
	}
	
	/**
	 * @return true if the editor can be submitted without the subsequent stage
	 */
	public boolean isSubmissionPossible()
	{
		return (stage == Stage.FIRST && form.isLocalSignupEnabled() && !FormLayoutUtils.hasLocalSignupButton(effectiveLayout)) 
				|| stage == Stage.SECOND;
	}

	Stage getStage()
	{
		return stage;
	}
	
	private void setRequestCode(RegistrationRequest ret, FormErrorStatus status)
	{
		if (form.getRegistrationCode() != null && regCodeProvided == null)
		{
			ret.setRegistrationCode(registrationCode.getValue());
			if (registrationCode.getValue().isEmpty())
			{
				registrationCode.setComponentError(new UserError(msg.getMessage("fieldRequired")));
				status.hasFormException = true;
			} else
				registrationCode.setComponentError(null);
		}
		
		if (invitation != null)
			ret.setRegistrationCode(regCodeProvided);
	}
	

	void focusFirst()
	{
		focusFirst(layoutContainer.registrationFormLayout);
	}
	
	private void initUI()
	{
		
		RegistrationInvitationParam regInv = invitation == null ? null : invitation.getAsRegistration();
		
		layoutContainer = createLayouts(buildVarsToFreemarkerTemplates(Optional.ofNullable(regInv)));

		resolveRemoteSignupOptions();
		PrefilledSet prefilled = new PrefilledSet();
		if (regInv != null)
		{	
			FormPrefill formPrefill = regInv.getFormPrefill();
			prefilled = new PrefilledSet(formPrefill.getIdentities(),
					formPrefill.getGroupSelections(),
					formPrefill.getAttributes(),
					formPrefill.getAllowedGroups());
		}
		prefilled = prefilled.mergeWith(urlQueryPrefillCreator.create(form));
		createControls(layoutContainer, effectiveLayout, prefilled);
	}
	
	boolean performAutomaticRemoteSignupIfNeeded()
	{
		if (isAutomatedAuthenticationDesired() && externalSignupOptions.size() > 0)
		{
			VaadinServletRequest httpRequest = (VaadinServletRequest) VaadinRequest.getCurrent();
			String requestedAuthnOption = httpRequest.getParameter(PreferredAuthenticationHelper.IDP_SELECT_PARAM);
			if (externalSignupOptions.size() > 1 && requestedAuthnOption == null)
			{
				log.warn("There are more multiple remote signup options are installed, "
						+ "and automated signup was requested without specifying (with " 
						+  "{}) which one should be used. Automatic signup is skipped.", 
						PreferredAuthenticationHelper.IDP_SELECT_PARAM);
				return false;
			}
			AuthNOption authnOption = requestedAuthnOption != null ? 
					externalSignupOptions.get(AuthenticationOptionKey.valueOf(requestedAuthnOption)) : 
					externalSignupOptions.values().iterator().next();
			if (authnOption == null)
			{
				log.warn("Remote signup option {} specified for auto signup is invalid. "
						+ "Automatic signup is skipped.", requestedAuthnOption);
				return false;
			}
			if (authnOption.authenticator instanceof ProxyAuthenticationCapable)
			{
				ProxyAuthenticationCapable proxyAuthn = (ProxyAuthenticationCapable) authnOption.authenticator;
				proxyAuthn.triggerAutomatedUIAuthentication(authnOption.authenticatorUI);
				return true;
			} else
			{
				log.warn("Automatic signup was requested but the selected remote authenticator "
						+ "is not capable of automatic triggering");
				return false;
			}
		}
		return false;
	}
	
	private boolean isAutomatedAuthenticationDesired()
	{
		VaadinServletRequest httpRequest = (VaadinServletRequest) VaadinRequest.getCurrent();
		String autoLogin = httpRequest.getParameter(ProxyAuthenticationFilter.TRIGGERING_PARAM);
		if (autoLogin != null && Boolean.parseBoolean(autoLogin))
			return true;
		return false;
	}

	@Override
	protected RegistrationLayoutsContainer createLayouts(Map<String, Object> params)
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		main.setWidth(100, Unit.PERCENTAGE);
		setCompositionRoot(main);
		
		String logoUri = form.getLayoutSettings().getLogoURL();
		Optional<Resource> logoRes = imageAccessService.getConfiguredImageResourceFromNullableUri(logoUri);
		if (logoRes.isPresent())
		{
			Image image = new Image(null, logoRes.get());
			image.addStyleName("u-signup-logo");
			main.addComponent(image);
			main.setComponentAlignment(image, Alignment.TOP_CENTER);	
		}
		
		I18nString title = stage == Stage.FIRST ? form.getDisplayedName() : form.getTitle2ndStage();
		Label formName = new Label(processFreeemarkerTemplate(params, title.getValue(msg)));
		formName.addStyleName(Styles.vLabelH1.toString());
		formName.addStyleName("u-reg-title");
		main.addComponent(formName);
		main.setComponentAlignment(formName, Alignment.MIDDLE_CENTER);
		
		if (stage == Stage.FIRST)
		{
			getFormInformationComponent(form.getFormInformation(), params).ifPresent(formInformation -> {
				main.addComponent(formInformation);
				main.setComponentAlignment(formInformation, Alignment.MIDDLE_CENTER);
			});
			

			Optional<Label> switchToEnquiryLabel = toEnquirySwitchLabelProvider
					.getSwitchToEnquiryLabel(form.getSwitchToEnquiryInfoFallbackToDefault(msg), invitation, params);
			if (switchToEnquiryLabel.isPresent())
			{
				main.addComponent(switchToEnquiryLabel.get());
				main.setComponentAlignment(switchToEnquiryLabel.get(), Alignment.MIDDLE_CENTER);
			}
		} else if (stage == Stage.SECOND)
		{
			getFormInformationComponent(form.getFormInformation2ndStage(), params).ifPresent(formInformation -> {
				main.addComponent(formInformation);
				main.setComponentAlignment(formInformation, Alignment.MIDDLE_CENTER);
			});
			
		}
		
		RegistrationLayoutsContainer container = new RegistrationLayoutsContainer(formWidth(), formWidthUnit());
		container.addFormLayoutToRootLayout(main);
		return container;
	}
	
	private Optional<HtmlConfigurableLabel> getFormInformationComponent(I18nString formInfo, Map<String, Object> params)
	{
		String info = formInfo == null ? null
				: processFreeemarkerTemplate(params, formInfo.getValue(msg));
		if (info != null)
		{
			HtmlConfigurableLabel formInformation = new HtmlConfigurableLabel(info);
			formInformation.addStyleName("u-reg-info");
			formInformation.setWidth(formWidth(), formWidthUnit());
			return Optional.of(formInformation);
		}
		return Optional.empty();
	}
	
	private void resolveRemoteSignupOptions()
	{
		externalSignupOptions = new HashMap<>();
		if (!form.getExternalSignupSpec().isEnabled())
			return;
		
		Set<String> authnOptions = form.getExternalSignupSpec().getSpecs().stream()
			.map(a -> a.authenticatorKey)
			.collect(Collectors.toSet());
		List<AuthenticationFlow> flows = authnSupport.resolveAuthenticationFlows(Lists.newArrayList(authnOptions),
				VaadinAuthentication.NAME);
		Set<AuthenticationOptionsSelector> formSignupSpec = form.getExternalSignupSpec().getSpecs().stream()
				.collect(Collectors.toSet());
		for (AuthenticationFlow flow : flows)
		{
			for (AuthenticatorInstance authenticator : flow.getFirstFactorAuthenticators())
			{
				VaadinAuthentication vaadinAuthenticator = (VaadinAuthentication) authenticator.getRetrieval();
				String authenticatorKey = vaadinAuthenticator.getAuthenticatorId();
				AuthenticatorStepContext context = new AuthenticatorStepContext(
						InvocationContext.getCurrent().getRealm(), flow, null, FactorOrder.FIRST);
				Collection<VaadinAuthenticationUI> optionUIInstances = 
						vaadinAuthenticator.createUIInstance(Context.REGISTRATION, context);
				for (VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
				{
					String optionKey = vaadinAuthenticationUI.getId();
					AuthenticationOptionKey authnOption = new AuthenticationOptionKey(authenticatorKey, optionKey);
					if (formSignupSpec.stream().anyMatch(selector -> selector.matchesAuthnOption(authnOption)))
					{
						AuthNOption signupAuthNOption = new AuthNOption(flow, 
								vaadinAuthenticator,  vaadinAuthenticationUI);
						setupExpectedIdentity(vaadinAuthenticationUI);
						externalSignupOptions.put(authnOption, signupAuthNOption);
					}
				}
			}
		}
	}

	private void setupExpectedIdentity(VaadinAuthenticationUI vaadinAuthenticationUI)
	{
		if (invitation == null)
			return;
		if (invitation.getAsRegistration().getExpectedIdentity() != null)
			vaadinAuthenticationUI.setExpectedIdentity(invitation.getAsRegistration().getExpectedIdentity());
	}
	
	@Override
	protected boolean createControlFor(RegistrationLayoutsContainer layoutContainer, FormElement element, 
			FormElement previousAdded, FormElement next, PrefilledSet prefilled)
	{
		switch (element.getType())
		{
		case CAPTCHA:
			return createCaptchaControl(layoutContainer.registrationFormLayout);
		case REG_CODE:
			return createRegistrationCodeControl(layoutContainer.registrationFormLayout);
		case REMOTE_SIGNUP:
			return createRemoteSignupButton(layoutContainer.registrationFormLayout, (FormParameterElement) element);
		case REMOTE_SIGNUP_GRID:
			return createRemoteSignupGrid(layoutContainer.registrationFormLayout);
		case LOCAL_SIGNUP:
			return createLocalSignupButton(layoutContainer.registrationFormLayout);
		default:
			return super.createControlFor(layoutContainer, element, previousAdded, next, prefilled);
		}
	}

	private boolean createRemoteSignupGrid(VerticalLayout registrationFormLayout)
	{			
		ExternalSignupGridSpec externalSignupGridSpec = form.getExternalSignupGridSpec();
		AuthnGridSettings gridSettings = externalSignupGridSpec.getGridSettings();
		if (gridSettings == null)
		{
			gridSettings = new AuthnGridSettings();
		}
		
		List<AuthNOption> options = new ArrayList<>();
		for (AuthenticationOptionsSelector spec : externalSignupGridSpec.getSpecs())
		{
			List<AuthNOption> signupOptions = getSignupOptions(spec);
			if (signupOptions.isEmpty())
			{
				log.debug("Ignoring not available remote sign up options: {}", spec.toStringEncodedSelector());
			}
			
			options.addAll(signupOptions);
		}
		
		if (options.isEmpty())
		{
			log.debug("All signup options are not available, skipping add remote sigup grid");
			return false;
		}
		
		AuthnsGridWidget grid = new AuthnsGridWidget(options, msg, new RegGridAuthnPanelFactory(), gridSettings.height);
		grid.setWidth(formWidth(), formWidthUnit());
		SearchComponent search = new SearchComponent(msg, grid::filter);
		if (gridSettings.searchable)
		{
			registrationFormLayout.addComponent(search);
			registrationFormLayout.setComponentAlignment(search, Alignment.MIDDLE_RIGHT);
		}
		
		registrationFormLayout.addComponent(grid);
		registrationFormLayout.setComponentAlignment(grid, Alignment.MIDDLE_CENTER);
		if (!enableRemoteRegistration)
			grid.setEnabled(false);
	
		return true;		
	}

	private boolean createRemoteSignupButton(AbstractOrderedLayout layout, FormParameterElement element)
	{
		int index = element.getIndex();
		AuthenticationOptionsSelector spec = form.getExternalSignupSpec().getSpecs().get(index);

		List<AuthNOption> options = getSignupOptions(spec);
		if (options.isEmpty())
		{
			log.debug("Ignoring not available remote sign up option {}", spec.toStringEncodedSelector());
			return false;
		}

		for (AuthNOption option : options)
		{
			Component signupOptionComponent = option.authenticatorUI.getComponent();
			signupOptionComponent.setWidth(formWidth(), formWidthUnit());
			layout.addComponent(signupOptionComponent);
			layout.setComponentAlignment(signupOptionComponent, Alignment.MIDDLE_CENTER);

			if (!enableRemoteRegistration)
			{
				signupOptionComponent.setEnabled(false);
			} else
			{
				option.authenticatorUI.setAuthenticationCallback(new SignUpAuthnCallback(
						form, regCodeProvided, 
						new AuthenticationOptionKey(option.authenticator.getAuthenticatorId(), 
								option.authenticatorUI.getId())));
			}
		}

		return true;
	}

	private List<AuthNOption> getSignupOptions(AuthenticationOptionsSelector spec)
	{
		return externalSignupOptions.entrySet().stream()
					.filter(e -> spec.matchesAuthnOption(e.getKey()))
					.map(e -> e.getValue()).collect(Collectors.toList());
	}
	
	private boolean createLocalSignupButton(AbstractOrderedLayout layout)
	{
		Button localSignup = new Button(msg.getMessage("RegistrationRequest.localSignup"));
		localSignup.addStyleName("u-localSignUpButton");
		localSignup.addClickListener(event -> onLocalSignupHandler.accept(regCodeProvided));
		localSignup.setWidth(formWidth(), formWidthUnit());
		layout.addComponent(localSignup);
		layout.setComponentAlignment(localSignup, Alignment.MIDDLE_CENTER);
		return true;
	}
	
	private boolean createCaptchaControl(Layout layout)
	{
		captcha = new CaptchaComponent(msg, form.getCaptchaLength(), form.getLayoutSettings().isCompactInputs());
		layout.addComponent(HtmlTag.br());
		layout.addComponent(captcha.getAsComponent());
		return true;
	}

	private boolean createRegistrationCodeControl(Layout layout)
	{
		registrationCode = new TextField(msg.getMessage("RegistrationRequest.registrationCode"));
		registrationCode.setRequiredIndicatorVisible(true);
		layout.addComponent(registrationCode);
		return true;
	}
	
	@Override
	protected boolean isPolicyAgreementsIsFiltered(PolicyAgreementConfiguration toCheck)
	{
		return false;
	}
	
	public RegistrationForm getForm()
	{
		return form;
	}
	
	RemotelyAuthenticatedPrincipal getRemoteAuthnContext()
	{
		return remotelyAuthenticated;
	}

	private class RegGridAuthnPanelFactory implements AuthNPanelFactory
	{
		@Override
		public FirstFactorAuthNPanel createRegularAuthnPanel(AuthNOption authnOption)
		{
			return null;
		}

		@Override
		public FirstFactorAuthNPanel createGridCompatibleAuthnPanel(AuthNOption authnOption)
		{
			AuthenticationOptionKey optionId = new AuthenticationOptionKey(
					authnOption.authenticator.getAuthenticatorId(),
					authnOption.authenticatorUI.getId());

			FirstFactorAuthNPanel authNPanel = new FirstFactorAuthNPanel(null, null, true,
					authnOption.authenticatorUI, optionId);

			if (enableRemoteRegistration)
				authnOption.authenticatorUI.setAuthenticationCallback(
						new SignUpAuthnCallback(form, regCodeProvided, optionId));
			return authNPanel;
		}
	}
	
	protected Map<String, Object> buildVarsToFreemarkerTemplates(Optional<RegistrationInvitationParam> invitation)
	{
		Map<String, Object> ret = new HashMap<>();
		if (invitation.isPresent())
		{
			ret.putAll(invitation.get().getFormPrefill().getMessageParamsWithCustomVarObject(
								MessageTemplateDefinition.CUSTOM_VAR_PREFIX));
			ret.put(INVITATION_EMAIL_VAR, invitation.get().getContactAddress());	
		}
		
		return ret;
	}
	
	
}
