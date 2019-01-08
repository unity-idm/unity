/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IdentityExistsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.AsyncErrorHandler;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.AbstractDialog;



/**
 * Responsible for showing a given registration form dialog. This version is intended for general use.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class InsecureRegistrationFormLauncher extends AbstraceRegistrationFormDialogProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InsecureRegistrationFormLauncher.class);
	private RegistrationsManagement registrationsManagement;
	private IdPLoginController idpLoginController;
	private EventsBus bus;
	private AutoLoginAfterSignUpProcessor autoLoginProcessor;
	
	@Autowired
	public InsecureRegistrationFormLauncher(UnityMessageSource msg, IdPLoginController idpLoginController,
			ObjectFactory<RequestEditorCreator> requestEditorCreatorFatory, 
			@Qualifier("insecure") RegistrationsManagement registrationsManagement,
			AutoLoginAfterSignUpProcessor autoLoginProcessor)
	{
		super(msg, requestEditorCreatorFatory);
		this.idpLoginController = idpLoginController;
		this.registrationsManagement = registrationsManagement;
		this.bus = WebSession.getCurrent().getEventBus();
		this.autoLoginProcessor = autoLoginProcessor;
	}

	private WorkflowFinalizationConfiguration addRequest(RegistrationRequest request, 
			RegistrationRequestEditor editor, RegistrationContext context) throws WrongArgumentException
	{
		RegistrationForm form = editor.getForm();
		try
		{
			String requestId = registrationsManagement.submitRegistrationRequest(request, context);
			bus.fireEvent(new RegistrationRequestChangedEvent(requestId));
			RegistrationRequestState requestState = getRequestStatus(requestId);
			
			boolean isAutoLogin = autoLoginProcessor.signInIfPossible(editor, requestState);
			
			RegistrationRequestStatus effectiveStateForFinalization = requestState == null 
					? RegistrationRequestStatus.rejected 
					: requestState.getStatus();
			WorkflowFinalizationConfiguration finalization = getFinalizationHandler(form)
					.getFinalRegistrationConfigurationPostSubmit(requestId, effectiveStateForFinalization);
			finalization.setAutoLoginAfterSignUp(isAutoLogin);
			return finalization;
		} catch (IdentityExistsException e)
		{
			return getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.PRESET_USER_EXISTS);
		} catch (WrongArgumentException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.warn("Registration request submision failed", e);
			return getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.GENERAL_ERROR);
		}
	}
	
	private RegistrationRequestState getRequestStatus(String requestId) 
	{
		try
		{
			return registrationsManagement.getRegistrationRequest(requestId);
		} catch (Exception e)
		{
			log.error("Shouldn't happen: can't get request status, assuming rejected", e);
			return null;
		}
	}
	
	private PostFillingHandler getFinalizationHandler(RegistrationForm form)
	{
		String pageTitle = form.getPageTitle() == null ? null : form.getPageTitle().getValue(msg);
		return new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), true);
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
		RegistrationContext context = new RegistrationContext(
				idpLoginController.isLoginInProgress(), mode);
		boolean isSimplifiedFinalization = isRemoteLoginWhenUnknownUser(mode);
		RegistrationFormFillDialog dialog = new RegistrationFormFillDialog(msg, 
				msg.getMessage("RegistrationFormsChooserComponent.dialogCaption"), 
				editor, new RegistrationFormFillDialog.Callback()
				{
					@Override
					public WorkflowFinalizationConfiguration newRequest(RegistrationRequest request) throws WrongArgumentException
					{
						return addRequest(request, editor, context);
					}
					
					@Override
					public void cancelled()
					{
					}
				}, idpLoginController, isSimplifiedFinalization);
		return dialog;
	}
}
