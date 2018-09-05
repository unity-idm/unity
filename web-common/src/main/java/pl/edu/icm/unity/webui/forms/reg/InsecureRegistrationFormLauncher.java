/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.webui.AsyncErrorHandler;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.forms.PostFormFillingHandler;



/**
 * Responsible for showing a given registration form dialog. This version is intended for general use.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class InsecureRegistrationFormLauncher extends AbstraceRegistrationFormDialogProvider
{
	private RegistrationsManagement registrationsManagement;
	private IdPLoginController idpLoginController;
	private EventsBus bus;
	
	@Autowired
	public InsecureRegistrationFormLauncher(UnityMessageSource msg, IdPLoginController idpLoginController,
			ObjectFactory<RequestEditorCreator> requestEditorCreatorFatory, 
			@Qualifier("insecure") RegistrationsManagement registrationsManagement)
	{
		super(msg, requestEditorCreatorFatory);
		this.idpLoginController = idpLoginController;
		this.registrationsManagement = registrationsManagement;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	protected boolean addRequest(RegistrationRequest request, RegistrationForm form, RegistrationContext context) 
			throws WrongArgumentException
	{
		String id;
		try
		{
			id = registrationsManagement.submitRegistrationRequest(request, context);
			bus.fireEvent(new RegistrationRequestChangedEvent(id));
		} catch (WrongArgumentException e)
		{
			throw e;
		} catch (Exception e)
		{
			new PostFormFillingHandler(idpLoginController, form, msg, 
					registrationsManagement.getFormAutomationSupport(form)).
						submissionError(e, context);
			return true;
		}

		new PostFormFillingHandler(idpLoginController, form, msg, 
				registrationsManagement.getFormAutomationSupport(form)).
					submittedRegistrationRequest(id, registrationsManagement,
				request, context);
		return true;
	}
	
	public void showRegistrationDialog(String formName, RemotelyAuthenticatedContext remoteContext, 
			TriggeringMode mode, AsyncErrorHandler errorHandler) throws EngineException
	{
		List<RegistrationForm> forms = registrationsManagement.getForms();
		for (RegistrationForm form: forms)
		{
			if (formName.equals(form.getName()))
			{
				showRegistrationDialog(form, remoteContext, mode, errorHandler);
				return;
			}
		}
		throw new WrongArgumentException("There is no registration form " + formName);
	}
	
	@Override
	protected AbstractDialog createDialog(RegistrationForm form, RegistrationRequestEditor editor, TriggeringMode mode)
	{
		RegistrationContext context = new RegistrationContext(true, 
				idpLoginController.isLoginInProgress(), mode);
		RegistrationFormFillDialog dialog = new RegistrationFormFillDialog(msg, 
				msg.getMessage("RegistrationFormsChooserComponent.dialogCaption"), 
				editor, new RegistrationFormFillDialog.Callback()
				{
					@Override
					public boolean newRequest(RegistrationRequest request) throws WrongArgumentException
					{
						return addRequest(request, form, context);
					}
						@Override
					public void cancelled()
					{
						new PostFormFillingHandler(idpLoginController, form, msg, 
								registrationsManagement.getFormAutomationSupport(form)).
							cancelled(false, context);
					}
				});
		return dialog;
	}
}
