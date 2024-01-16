/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.registration;

import com.vaadin.flow.component.dialog.Dialog;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IdentityExistsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.*;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.util.List;


/**
 * Responsible for showing a given registration form dialog. This version is intended for general use.
 */
@PrototypeComponent
@Primary
public class InsecureRegistrationFormLauncher extends AbstractRegistrationFormDialogProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InsecureRegistrationFormLauncher.class);
	private final RegistrationsManagement registrationsManagement;
	private final IdPLoginController idpLoginController;
	private final EventsBus bus;
	private final AutoLoginAfterSignUpProcessor autoLoginProcessor;
	private final VaadinLogoImageLoader imageAccessService;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	public InsecureRegistrationFormLauncher(MessageSource msg, IdPLoginController idpLoginController,
	                                        ObjectFactory<RequestEditorCreator> requestEditorCreatorFatory,
	                                        @Qualifier("insecure") RegistrationsManagement registrationsManagement,
	                                        AutoLoginAfterSignUpProcessor autoLoginProcessor,
	                                        VaadinLogoImageLoader imageAccessService, NotificationPresenter notificationPresenter)
	{
		super(msg, requestEditorCreatorFatory);
		this.idpLoginController = idpLoginController;
		this.registrationsManagement = registrationsManagement;
		this.bus = WebSession.getCurrent().getEventBus();
		this.autoLoginProcessor = autoLoginProcessor;
		this.imageAccessService = imageAccessService;
		this.notificationPresenter = notificationPresenter;
	}

	private WorkflowFinalizationConfiguration addRequest(RegistrationRequest request,
	                                                     RegistrationRequestEditor editor, RegistrationContext context) throws WrongArgumentException
	{
		RegistrationForm form = editor.getForm();
		try
		{
			String requestId = registrationsManagement.submitRegistrationRequest(request, context);
			bus.fireEvent(new RegistrationRequestsChangedEvent());
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
	
	@Override
	public void showRegistrationDialog(String formName, RemotelyAuthenticatedPrincipal remoteContext, 
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
	protected Dialog createDialog(RegistrationForm form, RegistrationRequestEditor editor, TriggeringMode mode)
	{
		RegistrationContext context = new RegistrationContext(
				idpLoginController.isLoginInProgress(), mode);
		boolean isSimplifiedFinalization = isRemoteLoginWhenUnknownUser(mode);
		return new RegistrationFormFillDialog(msg, imageAccessService,
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
				}, idpLoginController, notificationPresenter, isSimplifiedFinalization);
	}
}
