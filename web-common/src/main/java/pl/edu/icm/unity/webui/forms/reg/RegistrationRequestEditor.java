/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.Authenticator;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.registration.FormLayoutUtils;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLocalSignupElement;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;
import pl.edu.icm.unity.webui.forms.RegistrationLayoutsContainer;

/**
 * Generates a UI based on a given registration form. User can fill the form and a request is returned.
 * The class verifies if the data obtained from an upstream IdP is complete wrt requirements of the form.
 * <p>
 * Objects of this class should be typically created using {@link RequestEditorCreator}, so that the
 * registration code is collected appropriately.
 * @author K. Benedyczak
 */
public class RegistrationRequestEditor extends BaseRequestEditor<RegistrationRequest>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RegistrationRequestEditor.class);
	private RegistrationForm form;
	
	private TextField registrationCode;
	private CaptchaComponent captcha;
	private String regCodeProvided;
	private InvitationWithCode invitation;
	private InvitationManagement invitationMan;
	private AuthenticatorSupportManagement authnSupport;
	private SignUpAuthNController signUpAuthNController;
	private Map<AuthenticationOptionKey, SignUpAuthNOption> signupOptions;
	private Runnable onLocalSignupHandler;
	private FormLayout effectiveLayout;

	/**
	 * Note - the two managers must be insecure, if the form is used in not-authenticated context, 
	 * what is possible for registration form.
	 *  
	 * @param msg
	 * @param form
	 * @param remotelyAuthenticated
	 * @param identityEditorRegistry
	 * @param credentialEditorRegistry
	 * @param attributeHandlerRegistry
	 * @param aTypeMan
	 * @param signUpAuthNController 
	 * @param authnMan
	 * @throws EngineException
	 */
	public RegistrationRequestEditor(UnityMessageSource msg, RegistrationForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributeTypeManagement aTypeMan, CredentialManagement credMan,
			GroupsManagement groupsMan, 
			String registrationCode, InvitationManagement invitationMan, 
			AuthenticatorSupportManagement authnSupport, 
			SignUpAuthNController signUpAuthNController,
			FormLayout layout,
			Runnable onLocalSignupHandler) throws Exception
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, aTypeMan, credMan, groupsMan);
		this.form = form;
		this.regCodeProvided = registrationCode;
		this.invitationMan = invitationMan;
		this.signUpAuthNController = signUpAuthNController;
		this.authnSupport = authnSupport;
		this.onLocalSignupHandler = onLocalSignupHandler;
		this.effectiveLayout = layout;
		initUI();
	}
	
	@Override
	public RegistrationRequest getRequest(boolean withCredentials) throws FormValidationException
	{
		if (FormLayoutUtils.isLayoutWithLocalSignup(effectiveLayout))
		{
			throw new FormValidationException(msg.getMessage("RegistrationRequest.continueRegistration"));
		}
		
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
			throw new FormValidationException();
		
		return ret;
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
	
	private void initUI() throws EngineException
	{
		RegistrationLayoutsContainer layoutContainer = createLayouts();
		
		setupInvitationByCode();
		
		resolveRemoteSignupOptions();
		
		createControls(layoutContainer, effectiveLayout, invitation);
		
		finalizeLayoutInitialization(layoutContainer);
	}
	
	private void resolveRemoteSignupOptions()
	{
		if (!form.getExternalSignupSpec().isEnabled())
			return;
		
		signupOptions = Maps.newHashMap();
		Set<String> authnOptions = form.getExternalSignupSpec().getSpecs().stream()
			.map(AuthenticationOptionKey::getAuthenticatorKey)
			.collect(Collectors.toSet());
		List<AuthenticationFlow> flows = authnSupport.resolveAndGetAuthenticationFlows(Lists.newArrayList(authnOptions));
		Set<AuthenticationOptionKey> formSignupSpec = form.getExternalSignupSpec().getSpecs().stream().collect(Collectors.toSet());
		for (AuthenticationFlow flow : flows)
		{
			for (Authenticator authenticator : flow.getFirstFactorAuthenticators())
			{
				VaadinAuthentication vaadinAuthenticator = (VaadinAuthentication) authenticator.getRetrieval();
				String authenticatorKey = vaadinAuthenticator.getAuthenticatorId();
				Collection<VaadinAuthenticationUI> optionUIInstances = vaadinAuthenticator.createUIInstance(Context.REGISTRATION);
				for (VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
				{
					String optionKey = vaadinAuthenticationUI.getId();
					AuthenticationOptionKey authnOption = new AuthenticationOptionKey(authenticatorKey, optionKey);
					if (formSignupSpec.contains(authnOption))
					{
						SignUpAuthNOption signupAuthNOption = new SignUpAuthNOption(flow, vaadinAuthenticationUI);
						setupExpectedIdentity(vaadinAuthenticationUI);
						signupOptions.put(authnOption, signupAuthNOption);
					}
				}
			}
		}
	}

	private void setupExpectedIdentity(VaadinAuthenticationUI vaadinAuthenticationUI)
	{
		if (invitation != null && invitation.getExpectedIdentity() != null)
			vaadinAuthenticationUI.setExpectedIdentity(invitation.getExpectedIdentity());
	}
	
	@Override
	protected boolean createControlFor(RegistrationLayoutsContainer layoutContainer, FormElement element, 
			FormElement previousAdded, InvitationWithCode invitation) throws EngineException
	{
		switch (element.getType())
		{
		case CAPTCHA:
			return createCaptchaControl(layoutContainer.registrationFormLayout, (BasicFormElement) element);
		case REG_CODE:
			return createRegistrationCodeControl(layoutContainer.registrationFormLayout, (BasicFormElement) element);
		case REMOTE_SIGNUP:
			return createRemoteSignupButton(layoutContainer.mainLayout, (FormParameterElement) element);
		case LOCAL_SIGNUP:
			return createLocalSignupButton(layoutContainer.mainLayout, (FormLocalSignupElement) element);
		default:
			return super.createControlFor(layoutContainer, element, previousAdded, invitation);
		}
	}

	private boolean createRemoteSignupButton(AbstractOrderedLayout layout, FormParameterElement element)
	{
		if (signUpAuthNController == null)
			return false;
		
		int index = element.getIndex();
		AuthenticationOptionKey spec =  form.getExternalSignupSpec().getSpecs().get(index);
		SignUpAuthNOption option = signupOptions.get(spec);
		option.authenticatorUI.setAuthenticationCallback(signUpAuthNController.buildCallback(option));
		Component signupOptionComponent = option.authenticatorUI.getComponent();
		signupOptionComponent.setWidth(form.getLayoutSettings().getColumnWidth(), 
				Unit.valueOf(form.getLayoutSettings().getColumnWidthUnit()));
		layout.addComponent(signupOptionComponent);
		layout.setComponentAlignment(signupOptionComponent, Alignment.MIDDLE_CENTER);
		return true;
	}
	
	private boolean createLocalSignupButton(AbstractOrderedLayout layout, FormLocalSignupElement element)
	{
		Button localSignup = new Button(msg.getMessage("RegistrationRequest.localSignup"));
		localSignup.addStyleName("u-localSignUpButton");
		localSignup.addClickListener(event -> onLocalSignupHandler.run());
		localSignup.setWidth(form.getLayoutSettings().getColumnWidth(), 
				Unit.valueOf(form.getLayoutSettings().getColumnWidthUnit()));
		layout.addComponent(localSignup);
		layout.setComponentAlignment(localSignup, Alignment.MIDDLE_CENTER);
		return true;
	}
	
	private boolean createCaptchaControl(Layout layout, BasicFormElement element)
	{
		captcha = new CaptchaComponent(msg, form.getCaptchaLength());
		layout.addComponent(HtmlTag.br());
		layout.addComponent(captcha.getAsComponent());
		return true;
	}

	private boolean createRegistrationCodeControl(Layout layout, BasicFormElement element)
	{
		registrationCode = new TextField(msg.getMessage("RegistrationRequest.registrationCode"));
		registrationCode.setRequiredIndicatorVisible(true);
		layout.addComponent(registrationCode);
		return true;
	}
	
	private void setupInvitationByCode()
	{
		if (regCodeProvided != null)
			invitation = getInvitation(regCodeProvided);

		if (invitation != null && !invitation.getFormId().equals(form.getName()))
			throw new IllegalStateException(msg.getMessage("RegistrationRequest.errorCodeOfOtherForm"));
		if (form.isByInvitationOnly() && regCodeProvided == null)
			throw new IllegalStateException(msg.getMessage("RegistrationRequest.errorMissingCode"));
		if (form.isByInvitationOnly() &&  invitation == null)
			throw new IllegalStateException(msg.getMessage("RegistrationRequest.errorWrongCode"));
		if (form.isByInvitationOnly() &&  invitation.isExpired())
			throw new IllegalStateException(msg.getMessage("RegistrationRequest.errorExpiredCode"));
	}
	
	private InvitationWithCode getInvitation(String code)
	{
		try
		{
			return invitationMan.getInvitation(code);
		} catch (IllegalArgumentException e)
		{
			//ok
			return null;
		} catch (EngineException e)
		{
			log.warn("Error trying to check invitation with user provided code", e);
			return null;
		}
	}
}


