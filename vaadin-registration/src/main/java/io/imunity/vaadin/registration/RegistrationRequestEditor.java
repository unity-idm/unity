/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.registration;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.BaseRequestEditor;
import io.imunity.vaadin.endpoint.common.forms.RegistrationLayoutsContainer;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.forms.components.CaptchaComponent;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.registration.FormLayoutUtils;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;
import pl.edu.icm.unity.webui.forms.URLQueryPrefillCreator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RegistrationRequestEditor extends BaseRequestEditor<RegistrationRequest>
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
	private final SwitchToEnquiryComponentProvider toEnquirySwitchLabelProvider;
	private final AuthenticationOptionKey authnOptionKey;
	/**
	 * Note - the two managers must be insecure, if the form is used in not-authenticated context, 
	 * what is possible for registration form.
	 * @param toEnquirySwitchLabelProvider 
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
	                                 VaadinLogoImageLoader logoImageLoader)
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, aTypeMan, credMan, groupsMan, notificationPresenter,
				policyAgreementsRepresentationBuilder, logoImageLoader);
		this.form = form;
		this.regCodeProvided = registrationCode;
		this.invitation = invitation;
		this.urlQueryPrefillCreator = urlQueryPrefillCreator;
		this.toEnquirySwitchLabelProvider =  toEnquirySwitchLabelProvider;
		this.authnOptionKey = authnOptionKey;
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
			String info = form.getFormInformation() == null ? null
					: processFreeemarkerTemplate(params, form.getFormInformation().getValue(msg));
			if (info != null)
			{
				main.add(new Html(info));
			}

			Optional<Label> switchToEnquiryLabel = toEnquirySwitchLabelProvider
					.getSwitchToEnquiryLabel(form.getSwitchToEnquiryInfoFallbackToDefault(msg), invitation, params);
			switchToEnquiryLabel.ifPresent(main::add);
		}
		
		RegistrationLayoutsContainer container = new RegistrationLayoutsContainer(formWidth(), formWidthUnit());
		container.addFormLayoutToRootLayout(main);
		return container;
	}
	
	@Override
	protected boolean createControlFor(RegistrationLayoutsContainer layoutContainer, FormElement element, 
			FormElement previousAdded, FormElement next, PrefilledSet prefilled)
	{
		return switch (element.getType())
				{
					case CAPTCHA -> createCaptchaControl(layoutContainer.registrationFormLayout);
					case REG_CODE -> createRegistrationCodeControl(layoutContainer.registrationFormLayout);
					case LOCAL_SIGNUP -> createLocalSignupButton(layoutContainer.registrationFormLayout);
					default -> super.createControlFor(layoutContainer, element, previousAdded, next, prefilled);
				};
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
