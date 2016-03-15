/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.Selection;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;

import com.vaadin.server.UserError;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

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
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setWidth(80, Unit.PERCENTAGE);
		setCompositionRoot(main);
		
		Label formName = new Label(form.getDisplayedName().getValue(msg));
		formName.addStyleName(Styles.vLabelH1.toString());
		main.addComponent(formName);
		
		String info = form.getFormInformation() == null ? null : form.getFormInformation().getValue(msg);
		if (info != null)
		{
			HtmlConfigurableLabel formInformation = new HtmlConfigurableLabel(info);
			main.addComponent(formInformation);
		}

		FormLayout mainFormLayout = new FormLayout();
		main.addComponent(mainFormLayout);
		
		setupInvitationByCode();
		
		if (form.getRegistrationCode() != null && regCodeProvided == null)
		{
			registrationCode = new TextField(msg.getMessage("RegistrationRequest.registrationCode"));
			registrationCode.setRequired(true);
			mainFormLayout.addComponent(registrationCode);
		}
		
		if (form.getIdentityParams() != null && form.getIdentityParams().size() > 0)
		{
			Map<Integer, PrefilledEntry<IdentityParam>> fromInvitation = invitation != null ? 
					invitation.getIdentities() : new HashMap<>();
			createIdentityUI(mainFormLayout, fromInvitation);
		}

		if (form.getCredentialParams() != null && form.getCredentialParams().size() > 0)
			createCredentialsUI(mainFormLayout);
		
		if (form.getAttributeParams() != null && form.getAttributeParams().size() > 0)
		{
			Map<Integer, PrefilledEntry<Attribute<?>>> fromInvitation = invitation != null ? 
					invitation.getAttributes() : new HashMap<>();
			createAttributesUI(mainFormLayout, fromInvitation);
		}
		
		if (form.getGroupParams() != null && form.getGroupParams().size() > 0)
		{
			Map<Integer, PrefilledEntry<Selection>> fromInvitation = invitation != null ? 
					invitation.getGroupSelections() : new HashMap<>();
			createGroupsUI(mainFormLayout, fromInvitation);
			mainFormLayout.addComponent(HtmlTag.br());
		}
		
		if (form.isCollectComments())
			createCommentsUI(mainFormLayout);

		if (form.getAgreements() != null && form.getAgreements().size() > 0)
		{
			createAgreementsUI(mainFormLayout);
			mainFormLayout.addComponent(HtmlTag.br());
		}
		
		if (form.getCaptchaLength() > 0)
		{
			captcha = new CaptchaComponent(msg, form.getCaptchaLength());
			mainFormLayout.addComponent(HtmlTag.br());
			mainFormLayout.addComponent(captcha.getAsComponent());
		}
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


