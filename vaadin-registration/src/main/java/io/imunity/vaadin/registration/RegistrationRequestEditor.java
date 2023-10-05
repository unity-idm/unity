/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.registration;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationGrid;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationOption;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationSignupHandler;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationSignupResolverFactory;
import io.imunity.vaadin.endpoint.common.forms.BaseRequestEditor;
import io.imunity.vaadin.endpoint.common.forms.RegistrationLayoutsContainer;
import io.imunity.vaadin.endpoint.common.forms.URLQueryPrefillCreator;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.forms.components.CaptchaComponent;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.base.registration.FormLayoutUtils;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;
import pl.edu.icm.unity.base.registration.layout.FormElement;
import pl.edu.icm.unity.base.registration.layout.FormLayout;
import pl.edu.icm.unity.base.registration.layout.FormParameterElement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class RegistrationRequestEditor extends BaseRequestEditor<RegistrationRequest>
{
	private static final String INVITATION_EMAIL_VAR = "invitationEmail";
	
	enum Stage {FIRST, SECOND}
	
	private final RegistrationForm form;
	
	private TextField registrationCode;
	private CaptchaComponent captcha;
	private final String regCodeProvided;
	private final ResolvedInvitationParam invitation;
	private RequestEditorCreator.InvitationCodeConsumer onLocalSignupHandler;
	private FormLayout effectiveLayout;
	private Stage stage;
	private final URLQueryPrefillCreator urlQueryPrefillCreator;
	private final boolean enableRemoteRegistration;
	private final SwitchToEnquiryComponentProvider toEnquirySwitchLabelProvider;
	private final AuthenticationOptionKey authnOptionKey;
	private final RemoteRegistrationSignupResolverFactory remoteRegistrationSignupResolverFactory;
	private final AuthenticatorSupportService authnSupport;
	private final Map<String, List<String>> parameters;
	private RemoteRegistrationSignupHandler remoteRegistrationSignupHandler;

	/**
	 * Note - the two managers must be insecure, if the form is used in not-authenticated context, 
	 * what is possible for registration form.
	 */
	public RegistrationRequestEditor(MessageSource msg, RegistrationForm form,
	                                 RemotelyAuthenticatedPrincipal remotelyAuthenticated,
	                                 IdentityEditorRegistry identityEditorRegistry,
	                                 CredentialEditorRegistry credentialEditorRegistry,
	                                 AttributeHandlerRegistry attributeHandlerRegistry,
	                                 AttributeTypeManagement aTypeMan, CredentialManagement credMan,
	                                 GroupsManagement groupsMan, NotificationPresenter notificationPresenter,
	                                 String registrationCode, ResolvedInvitationParam invitation,
	                                 AuthenticatorSupportService authnSupport,
	                                 URLQueryPrefillCreator urlQueryPrefillCreator,
	                                 PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder,
	                                 SwitchToEnquiryComponentProvider toEnquirySwitchLabelProvider,
	                                 boolean enableRemoteRegistration,
	                                 AuthenticationOptionKey authnOptionKey,
	                                 VaadinLogoImageLoader logoImageLoader,
	                                 RemoteRegistrationSignupResolverFactory remoteRegistrationSignupResolverFactory,
									 Map<String, List<String>> parameters)
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, aTypeMan, credMan, groupsMan, notificationPresenter,
				policyAgreementsRepresentationBuilder, logoImageLoader);
		this.form = form;
		this.regCodeProvided = registrationCode;
		this.enableRemoteRegistration = enableRemoteRegistration;
		this.invitation = invitation;
		this.urlQueryPrefillCreator = urlQueryPrefillCreator;
		this.toEnquirySwitchLabelProvider =  toEnquirySwitchLabelProvider;
		this.authnOptionKey = authnOptionKey;
		this.authnSupport = authnSupport;
		this.remoteRegistrationSignupResolverFactory = remoteRegistrationSignupResolverFactory;
		this.parameters = parameters;
	}
	
	public void showFirstStage(RequestEditorCreator.InvitationCodeConsumer onLocalSignupHandler) throws AuthenticationException
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
				registrationCode.setInvalid(true);
				registrationCode.setErrorMessage(msg.getMessage("fieldRequired"));
				status.hasFormException = true;
			} else
				registrationCode.setInvalid(false);
		}
		
		if (invitation != null)
			ret.setRegistrationCode(regCodeProvided);
	}
	
	private void initUI()
	{
		RegistrationInvitationParam regInv = invitation == null ? null : invitation.getAsRegistration();

		RegistrationLayoutsContainer layoutContainer = createLayouts(buildVarsToFreemarkerTemplates(Optional.ofNullable(regInv)));
		remoteRegistrationSignupHandler = remoteRegistrationSignupResolverFactory.create(authnSupport, msg, form, invitation, regCodeProvided);
		PrefilledSet prefilled = new PrefilledSet();
		if (regInv != null)
		{	
			FormPrefill formPrefill = regInv.getFormPrefill();
			prefilled = new PrefilledSet(formPrefill.getIdentities(),
					formPrefill.getGroupSelections(),
					formPrefill.getAttributes(),
					formPrefill.getAllowedGroups());
		}
		prefilled = prefilled.mergeWith(urlQueryPrefillCreator.create(form, parameters));
		createControls(layoutContainer, effectiveLayout, prefilled);
	}

	void performAutomaticRemoteSignupIfNeeded()
	{
		remoteRegistrationSignupHandler.performAutomaticRemoteSignupIfNeeded();
	}

	@Override
	protected RegistrationLayoutsContainer createLayouts(Map<String, Object> params)
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		main.setWidthFull();
		add(main);
		
		addLogo(main);
		
		I18nString title = stage == Stage.FIRST ? form.getDisplayedName() : form.getTitle2ndStage();
		H1 formName = new H1(processFreeemarkerTemplate(params, title.getValue(msg)));
		formName.addClassName("u-reg-title");
		main.add(formName);
		main.setAlignItems(Alignment.CENTER);
		
		if (stage == Stage.FIRST)
		{
			getFormInformationComponent(form.getFormInformation(), params).ifPresent(main::add);
			Optional<Span> switchToEnquiryLabel = toEnquirySwitchLabelProvider
					.getSwitchToEnquiryLabel(form.getSwitchToEnquiryInfoFallbackToDefault(msg), invitation, params);
			switchToEnquiryLabel.ifPresent(main::add);
		}
		else if (stage == RegistrationRequestEditor.Stage.SECOND)
		{
			getFormInformationComponent(form.getFormInformation2ndStage(), params).ifPresent(main::add);
		}
		
		RegistrationLayoutsContainer container = new RegistrationLayoutsContainer(formWidth(), formWidthUnit());
		container.addFormLayoutToRootLayout(main);
		return container;
	}

	private Optional<Html> getFormInformationComponent(I18nString formInfo, Map<String, Object> params)
	{
		String info = formInfo == null ? null
				: processFreeemarkerTemplate(params, formInfo.getValue(msg));
		if (info != null)
		{
			Html formInformation = new Html("<div>" + info + "</div>");
			return Optional.of(formInformation);
		}
		return Optional.empty();
	}
	
	@Override
	protected boolean createControlFor(RegistrationLayoutsContainer layoutContainer, FormElement element, 
			FormElement previousAdded, FormElement next, PrefilledSet prefilled)
	{
		return switch (element.getType())
				{
					case CAPTCHA -> createCaptchaControl(layoutContainer.registrationFormLayout);
					case REG_CODE -> createRegistrationCodeControl(layoutContainer.registrationFormLayout);
					case REMOTE_SIGNUP -> createRemoteSignupButton(layoutContainer.registrationFormLayout, (FormParameterElement) element);
					case REMOTE_SIGNUP_GRID -> createRemoteSignupGrid(layoutContainer.registrationFormLayout);
					case LOCAL_SIGNUP -> createLocalSignupButton(layoutContainer.registrationFormLayout);
					default -> super.createControlFor(layoutContainer, element, previousAdded, next, prefilled);
				};
	}

	private boolean createRemoteSignupButton(VerticalLayout layout, FormParameterElement element)
	{
		List<RemoteRegistrationOption> options = remoteRegistrationSignupHandler.getOptions(element, enableRemoteRegistration);
		if (options.isEmpty())
			return false;

		for (RemoteRegistrationOption option : options)
		{
			Component signupOptionComponent = option.getComponent();
			((HasSize)signupOptionComponent).setWidth(formWidth(), formWidthUnit());
			layout.add(signupOptionComponent);
		}

		return true;
	}
	private boolean createRemoteSignupGrid(VerticalLayout registrationFormLayout)
	{
		ExternalSignupGridSpec externalSignupGridSpec = form.getExternalSignupGridSpec();
		ExternalSignupGridSpec.AuthnGridSettings gridSettings = externalSignupGridSpec.getGridSettings();
		if (gridSettings == null)
		{
			gridSettings = new ExternalSignupGridSpec.AuthnGridSettings();
		}
		RemoteRegistrationGrid grid = remoteRegistrationSignupHandler.getGrid(enableRemoteRegistration, gridSettings.height);

		if (grid.isEmpty())
			return false;

		((HasSize)grid.getComponent()).setWidth(formWidth(), formWidthUnit());
		if (gridSettings.searchable)
			registrationFormLayout.add(grid.getSearchComponent());

		registrationFormLayout.add(grid.getComponent());
		if (!enableRemoteRegistration)
			((HasEnabled)grid.getComponent()).setEnabled(false);

		return true;
	}

	private boolean createLocalSignupButton(VerticalLayout layout)
	{
		Button localSignup = new Button(msg.getMessage("RegistrationRequest.localSignup"));
		localSignup.addClassName("u-localSignUpButton");
		localSignup.addClickListener(event -> onLocalSignupHandler.accept(regCodeProvided));
		localSignup.setWidth(formWidth(), formWidthUnit());
		layout.add(localSignup);
		layout.setAlignItems(Alignment.CENTER);
		return true;
	}
	
	private boolean createCaptchaControl(VerticalLayout layout)
	{
		captcha = new CaptchaComponent(msg, form.getCaptchaLength(), form.getLayoutSettings().isCompactInputs());
		layout.add(new Hr());
		layout.add(captcha.getAsComponent());
		return true;
	}

	private boolean createRegistrationCodeControl(VerticalLayout layout)
	{
		registrationCode = new TextField(msg.getMessage("RegistrationRequest.registrationCode"));
		registrationCode.setRequiredIndicatorVisible(true);
		registrationCode.setWidthFull();
		layout.add(registrationCode);
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
