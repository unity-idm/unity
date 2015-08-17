/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.server.Page;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;



/**
 * Responsible for showing a given registration form dialog. Wrapper over {@link RegistrationRequestEditorDialog}
 * simplifying its instantiation.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RegistrationFormLauncher
{
	protected UnityMessageSource msg;
	protected RegistrationsManagement registrationsManagement;
	protected IdentityEditorRegistry identityEditorRegistry;
	protected CredentialEditorRegistry credentialEditorRegistry;
	protected AttributeHandlerRegistry attributeHandlerRegistry;
	protected AttributesManagement attrsMan;
	protected AuthenticationManagement authnMan;
	protected GroupsManagement groupsMan;
	
	protected boolean addAutoAccept;
	protected EventsBus bus;
	
	@Autowired
	public RegistrationFormLauncher(UnityMessageSource msg,
			RegistrationsManagement registrationsManagement,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, AuthenticationManagement authnMan,
			GroupsManagement groupsMan)
	{
		super();
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attrsMan = attrsMan;
		this.authnMan = authnMan;
		this.groupsMan = groupsMan;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	public void setAddAutoAccept(boolean addAutoAccept)
	{
		this.addAutoAccept = addAutoAccept;
	}

	protected boolean addRequest(RegistrationRequest request, boolean andAccept, RegistrationForm form)
	{
		String id;
		try
		{
			id = registrationsManagement.submitRegistrationRequest(request, !andAccept);
			bus.fireEvent(new RegistrationRequestChangedEvent(id));
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("RegistrationFormsChooserComponent.errorRequestSubmit"), e);
			return false;
		}

		try
		{							
			if (andAccept && addAutoAccept)
			{
				registrationsManagement.processRegistrationRequest(id, request, 
						RegistrationRequestAction.accept, null, 
						msg.getMessage("RegistrationFormsChooserComponent.autoAccept"));
				bus.fireEvent(new RegistrationRequestChangedEvent(id));
				NotificationPopup.showSuccess(msg, msg.getMessage("RegistrationFormsChooserComponent.requestSubmitted"), 
						msg.getMessage("RegistrationFormsChooserComponent.requestSubmittedInfoWithAccept"));
			} else
			{
				invokePostRegistrationAction(form, id, msg, registrationsManagement);
			}	
			
			return true;
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"RegistrationFormsChooserComponent.errorRequestAutoAccept"), e);
			return true;
		}
	}
	
	/**
	 * Invokes proper redirection or shows an information message depending on request status and form settings.
	 * @param form
	 * @param requestId
	 * @param msg
	 * @param registrationsManagement
	 * @throws EngineException
	 */
	public static void invokePostRegistrationAction(RegistrationForm form, String requestId, 
			UnityMessageSource msg, RegistrationsManagement registrationsManagement) throws EngineException
	{
		for (RegistrationRequestState r : registrationsManagement.getRegistrationRequests())
		{
			if (r.getRequestId().equals(requestId)
					&& r.getStatus() == RegistrationRequestStatus.accepted)
			{
				String redirect = form.getRedirectAfterSubmitAndAccept(); 
				if (redirect != null)
				{
					Page.getCurrent().open(redirect, null);
				} else
				{
					NotificationPopup.showNotice(msg,
						msg.getMessage("RegistrationFormsChooserComponent.requestSubmitted"),
						msg.getMessage("RegistrationFormsChooserComponent.requestSubmittedInfoWithAccept"));
				}
				return;
			}
		}
		String redirect = form.getRedirectAfterSubmit(); 
		if (redirect != null)
		{
			Page.getCurrent().open(redirect, null);
		} else
		{
			NotificationPopup.showNotice(msg, msg.getMessage("RegistrationFormsChooserComponent.requestSubmitted"),
				msg.getMessage("RegistrationFormsChooserComponent.requestSubmittedInfoNoAccept"));
		}
	}
	
	protected void handleFailure(EngineException e, RegistrationForm form)
	{
		
	}

	public RegistrationRequestEditorDialog getDialog(String formName, RemotelyAuthenticatedContext remoteContext) 
			throws EngineException
	{
		List<RegistrationForm> forms = registrationsManagement.getForms();
		for (RegistrationForm form: forms)
		{
			if (formName.equals(form.getName()))
				return getDialog(form, remoteContext);
		}
		throw new WrongArgumentException("There is no registration form " + formName);
	}
	
	public RegistrationRequestEditorDialog getDialog(final RegistrationForm form, 
			RemotelyAuthenticatedContext remoteContext) throws EngineException
	{
			RegistrationRequestEditor editor = new RegistrationRequestEditor(msg, form, 
					remoteContext, identityEditorRegistry, 
					credentialEditorRegistry, 
					attributeHandlerRegistry, attrsMan, authnMan, groupsMan);
			RegistrationRequestEditorDialog dialog = new RegistrationRequestEditorDialog(msg, 
					msg.getMessage("RegistrationFormsChooserComponent.dialogCaption"), 
					editor, addAutoAccept, new RegistrationRequestEditorDialog.Callback()
					{
						@Override
						public boolean newRequest(RegistrationRequest request, boolean autoAccept)
						{
							return addRequest(request, autoAccept, form);
						}
					});
			return dialog;
	}
}
