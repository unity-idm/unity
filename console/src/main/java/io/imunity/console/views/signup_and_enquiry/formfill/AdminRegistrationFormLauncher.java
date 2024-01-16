/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.formfill;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.dialog.Dialog;

import io.imunity.console.views.signup_and_enquiry.RegistrationRequestsChangedEvent;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.registration.AbstractRegistrationFormDialogProvider;
import io.imunity.vaadin.registration.RegistrationFormFillDialog;
import io.imunity.vaadin.registration.RegistrationRequestEditor;
import io.imunity.vaadin.registration.RequestEditorCreator;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IdentityExistsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Responsible for showing a given registration form dialog. Simplifies
 * instantiation of {@link RegistrationFormFillDialog}.
 * <p>
 * This version is intended for use in AdminUI where automatic request
 * acceptance is possible.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class AdminRegistrationFormLauncher extends AbstractRegistrationFormDialogProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AdminRegistrationFormLauncher.class);
	private final RegistrationsManagement registrationsManagement;
	private final IdPLoginController idpLoginController;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	public AdminRegistrationFormLauncher(MessageSource msg, RegistrationsManagement registrationsManagement,
			IdPLoginController idpLoginController, ObjectFactory<RequestEditorCreator> requestEditorCreatorFactory,
			NotificationPresenter notificationPresenter)
	{
		super(msg, requestEditorCreatorFactory);
		this.registrationsManagement = registrationsManagement;
		this.idpLoginController = idpLoginController;
		this.notificationPresenter = notificationPresenter;
	}

	protected void addRequest(RegistrationRequest request, boolean andAccept, RegistrationForm form,
			TriggeringMode mode) throws WrongArgumentException
	{
		String id = submitRequestCore(request, form);
		if (id == null)
			return;
		RegistrationRequestStatus status = getRequestStatus(id);

		try
		{
			if (status == RegistrationRequestStatus.pending && andAccept)
			{
				registrationsManagement.processRegistrationRequest(id, request, RegistrationRequestAction.accept, null,
						msg.getMessage("AdminFormLauncher.autoAccept"));
				WebSession.getCurrent()
						.getEventBus()
						.fireEvent(new RegistrationRequestsChangedEvent());
			}
		} catch (EngineException e)
		{
			log.error("Can not process registration request", e);
			notificationPresenter.showError("", msg.getMessage("AdminFormLauncher.errorRequestAutoAccept"));
		}
	}

	private String submitRequestCore(RegistrationRequest request, RegistrationForm form) throws WrongArgumentException
	{
		RegistrationContext context = new RegistrationContext(idpLoginController.isLoginInProgress(),
				TriggeringMode.manualAdmin);
		try
		{
			String requestId = registrationsManagement.submitRegistrationRequest(request, context);
			WebSession.getCurrent()
					.getEventBus()
					.fireEvent(new RegistrationRequestsChangedEvent());
			return requestId;
		} catch (IdentityExistsException e)
		{
			WorkflowFinalizationConfiguration config = getFinalizationHandler(form)
					.getFinalRegistrationConfigurationOnError(TriggeringState.PRESET_USER_EXISTS);
			notificationPresenter.showError(config.mainInformation,
					config.extraInformation == null ? "" : config.extraInformation);
		} catch (WrongArgumentException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.warn("Registration request submision failed", e);
			WorkflowFinalizationConfiguration config = getFinalizationHandler(form)
					.getFinalRegistrationConfigurationOnError(TriggeringState.GENERAL_ERROR);
			notificationPresenter.showError(config.mainInformation,
					config.extraInformation == null ? "" : config.extraInformation);
		}
		return null;
	}

	private RegistrationRequestStatus getRequestStatus(String requestId)
	{
		try
		{
			return registrationsManagement.getRegistrationRequest(requestId)
					.getStatus();
		} catch (Exception e)
		{
			log.error("Shouldn't happen: can't get request status, assuming rejected", e);
			return RegistrationRequestStatus.rejected;
		}
	}

	private PostFillingHandler getFinalizationHandler(RegistrationForm form)
	{
		String pageTitle = form.getPageTitle() == null ? null
				: form.getPageTitle()
						.getValue(msg);
		return new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg, pageTitle, form.getLayoutSettings()
				.getLogoURL(), true);
	}

	@Override
	protected Dialog createDialog(RegistrationForm form, RegistrationRequestEditor editor, TriggeringMode mode)
	{
		AdminFormFillDialog<RegistrationRequest> dialog = new AdminFormFillDialog<>(msg, notificationPresenter,
				msg.getMessage("AdminRegistrationFormLauncher.dialogCaption"), editor,
				new AdminFormFillDialog.Callback<RegistrationRequest>()
				{
					@Override
					public void newRequest(RegistrationRequest request, boolean autoAccept)
							throws WrongArgumentException
					{
						addRequest(request, autoAccept, form, mode);
					}

					@Override
					public void cancelled()
					{
					}
				});
		return dialog;
	}

	@Override
	public void showRegistrationDialog(String formName, RemotelyAuthenticatedPrincipal remoteContext,
			TriggeringMode mode, AsyncErrorHandler errorHandler) throws EngineException
	{
		List<RegistrationForm> forms = registrationsManagement.getForms();
		for (RegistrationForm form : forms)
		{
			if (formName.equals(form.getName()))
			{
				showRegistrationDialog(form, remoteContext, mode, errorHandler);
				return;
			}
		}
		throw new WrongArgumentException("There is no registration form " + formName);
	}
}
