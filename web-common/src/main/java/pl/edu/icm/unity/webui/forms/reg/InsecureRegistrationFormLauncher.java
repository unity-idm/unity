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
import pl.edu.icm.unity.webui.forms.PostFormFillingHandler;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreator.RequestEditorCreatedCallback;



/**
 * Responsible for showing a given registration form dialog. This version is intended for general use.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class InsecureRegistrationFormLauncher implements RegistrationFormDialogProvider
{
	@Autowired
	protected UnityMessageSource msg;
	
	@Autowired 
	@Qualifier("insecure")
	protected RegistrationsManagement registrationsManagement;
	
	@Autowired
	private IdPLoginController idpLoginController;
	
	@Autowired
	private ObjectFactory<RequestEditorCreator> requestEditorCreatorFatory;
	
	protected EventsBus bus;
	
	public InsecureRegistrationFormLauncher()
	{
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
	public void showRegistrationDialog(final RegistrationForm form, 
			RemotelyAuthenticatedContext remoteContext, TriggeringMode mode,
			AsyncErrorHandler errorHandler)
	{
		RegistrationContext context = new RegistrationContext(true, 
				idpLoginController.isLoginInProgress(), mode);
		RequestEditorCreator editorCreator = requestEditorCreatorFatory.getObject().init(form, remoteContext);
		editorCreator.invoke(new RequestEditorCreatedCallback()
		{
			@Override
			public void onCreationError(Exception e)
			{
				errorHandler.onError(e);
			}
			
			@Override
			public void onCreated(RegistrationRequestEditor editor)
			{
				showDialog(form, context, editor);
			}

			@Override
			public void onCancel()
			{
				//nop
			}
		});
	}
	
	private void showDialog(RegistrationForm form, RegistrationContext context, RegistrationRequestEditor editor)
	{
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
		dialog.show();
	}
}
