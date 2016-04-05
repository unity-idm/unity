/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import org.apache.log4j.Logger;

import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;

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
	private RegistrationsManagement registrationsMan;
	
	private TextField registrationCode;
	private CaptchaComponent captcha;
	private String regCodeProvided;
	private InvitationWithCode invitation;

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
	 * @param attrsMan
	 * @param authnMan
	 * @throws EngineException
	 */
	public RegistrationRequestEditor(UnityMessageSource msg, RegistrationForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, AuthenticationManagement authnMan,
			GroupsManagement groupsMan, RegistrationsManagement registrationsMan,
			String registrationCode) throws Exception
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, attrsMan, authnMan, groupsMan);
		this.form = form;
		this.registrationsMan = registrationsMan;
		this.regCodeProvided = registrationCode;
		
		initUI();
	}
	
	@Override
	public RegistrationRequest getRequest() throws FormValidationException
	{
		RegistrationRequest ret = new RegistrationRequest();
		FormErrorStatus status = new FormErrorStatus();

		super.fillRequest(ret, status);
		
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
		FormLayout mainFormLayout = createMainFormLayout();
		
		setupInvitationByCode();
		
		createControls(mainFormLayout, invitation);
	}
	
	@Override
	protected boolean createControlFor(AbstractOrderedLayout layout, FormElement element, 
			FormElement previousAdded, InvitationWithCode invitation) throws EngineException
	{
		switch (element.getType())
		{
		case RegistrationForm.CAPTCHA:
			return createCaptchaControl(layout, (BasicFormElement) element);
		case RegistrationForm.REG_CODE:
			return createRegistrationCodeControl(layout, (BasicFormElement) element);
		default:
			return super.createControlFor(layout, element, previousAdded, invitation);
		}
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
		registrationCode.setRequired(true);
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
			return registrationsMan.getInvitation(code);
		} catch (WrongArgumentException e)
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


