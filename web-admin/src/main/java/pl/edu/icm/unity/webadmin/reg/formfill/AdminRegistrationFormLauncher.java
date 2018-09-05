/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formfill;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.forms.PostFormFillingHandler;
import pl.edu.icm.unity.webui.forms.reg.AbstraceRegistrationFormDialogProvider;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormFillDialog;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestEditor;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreator;



/**
 * Responsible for showing a given registration form dialog. Simplifies instantiation of
 * {@link RegistrationFormFillDialog}.
 * <p> This version is intended for use in AdminUI where automatic request acceptance is possible.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class AdminRegistrationFormLauncher extends AbstraceRegistrationFormDialogProvider
{
	protected RegistrationsManagement registrationsManagement;
	protected CredentialEditorRegistry credentialEditorRegistry;
	protected AttributeHandlerRegistry attributeHandlerRegistry;
	protected AttributesManagement attrsMan;
	protected CredentialManagement authnMan;
	protected GroupsManagement groupsMan;
	
	protected EventsBus bus;
	private IdPLoginController idpLoginController;
	
	@Autowired
	public AdminRegistrationFormLauncher(UnityMessageSource msg,
			RegistrationsManagement registrationsManagement,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, CredentialManagement authnMan,
			GroupsManagement groupsMan, IdPLoginController idpLoginController,
			ObjectFactory<RequestEditorCreator> requestEditorCreatorFactory)
	{
		super(msg, requestEditorCreatorFactory);
		this.registrationsManagement = registrationsManagement;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attrsMan = attrsMan;
		this.authnMan = authnMan;
		this.groupsMan = groupsMan;
		this.idpLoginController = idpLoginController;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	protected boolean addRequest(RegistrationRequest request, boolean andAccept, RegistrationForm form, 
			TriggeringMode mode) throws WrongArgumentException
	{
		RegistrationContext context = new RegistrationContext(!andAccept, 
				idpLoginController.isLoginInProgress(), mode);
		String id;
		try
		{
			id = registrationsManagement.submitRegistrationRequest(request, context);
			bus.fireEvent(new RegistrationRequestChangedEvent(id));
		}  catch (WrongArgumentException e)
		{
			throw e;
		} catch (EngineException e)
		{
			new PostFormFillingHandler(idpLoginController, form, msg, 
					registrationsManagement.getFormAutomationSupport(form)).submissionError(e, context);
			return false;
		}

		try
		{							
			if (andAccept)
			{
				registrationsManagement.processRegistrationRequest(id, request, 
						RegistrationRequestAction.accept, null, 
						msg.getMessage("AdminFormLauncher.autoAccept"));
				bus.fireEvent(new RegistrationRequestChangedEvent(id));
			}	
			new PostFormFillingHandler(idpLoginController, form, msg, 
					registrationsManagement.getFormAutomationSupport(form), false).
				submittedRegistrationRequest(id, registrationsManagement, request, context);
			
			return true;
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"AdminFormLauncher.errorRequestAutoAccept"), e);
			return true;
		}
	}
	
	@Override
	protected AbstractDialog createDialog(RegistrationForm form, RegistrationRequestEditor editor, TriggeringMode mode)
	{
		AdminFormFillDialog<RegistrationRequest> dialog = new AdminFormFillDialog<>(msg, 
				msg.getMessage("AdminRegistrationFormLauncher.dialogCaption"), 
				editor, new AdminFormFillDialog.Callback<RegistrationRequest>()
				{
					@Override
					public boolean newRequest(RegistrationRequest request, boolean autoAccept) 
							throws WrongArgumentException
					{
						return addRequest(request, autoAccept, form, mode);
					}

					@Override
					public void cancelled()
					{
						RegistrationContext context = new RegistrationContext(false, 
								idpLoginController.isLoginInProgress(), mode);
						new PostFormFillingHandler(idpLoginController, form, msg, 
								registrationsManagement.getFormAutomationSupport(form)).
							cancelled(false, context);
					}
				});
		return dialog;
	}
}
