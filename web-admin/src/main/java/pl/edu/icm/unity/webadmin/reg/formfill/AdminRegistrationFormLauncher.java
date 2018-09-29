/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formfill;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IdentityExistsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
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
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AdminRegistrationFormLauncher.class);
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
				registrationsManagement.processRegistrationRequest(id, request, 
						RegistrationRequestAction.accept, null, 
						msg.getMessage("AdminFormLauncher.autoAccept"));
				bus.fireEvent(new RegistrationRequestChangedEvent(id));
			}	
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"AdminFormLauncher.errorRequestAutoAccept"), e);
		}
	}
	
	private String submitRequestCore(RegistrationRequest request, RegistrationForm form) throws WrongArgumentException
	{
		RegistrationContext context = new RegistrationContext(
				idpLoginController.isLoginInProgress(), TriggeringMode.manualAdmin);
		try
		{
			String requestId = registrationsManagement.submitRegistrationRequest(request, context);
			bus.fireEvent(new RegistrationRequestChangedEvent(requestId));
			return requestId;
		} catch (IdentityExistsException e)
		{
			WorkflowFinalizationConfiguration config = getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.PRESET_USER_EXISTS);
			NotificationPopup.showError(config.mainInformation, 
					config.extraInformation == null ? "" : config.extraInformation);
		} catch (WrongArgumentException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.warn("Registration request submision failed", e);
			WorkflowFinalizationConfiguration config =  getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.GENERAL_ERROR);
			NotificationPopup.showError(config.mainInformation, 
					config.extraInformation == null ? "" : config.extraInformation);
		}
		return null;
	}
	
	private RegistrationRequestStatus getRequestStatus(String requestId) 
	{
		try
		{
			return registrationsManagement.getRegistrationRequest(requestId).getStatus();
		} catch (Exception e)
		{
			log.error("Shouldn't happen: can't get request status, assuming rejected", e);
			return RegistrationRequestStatus.rejected;
		}
	}
	
	private PostFillingHandler getFinalizationHandler(RegistrationForm form)
	{
		String pageTitle = form.getPageTitle() == null ? null : form.getPageTitle().getValue(msg);
		return new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), true);
	}
	
	@Override
	protected AbstractDialog createDialog(RegistrationForm form, RegistrationRequestEditor editor, TriggeringMode mode)
	{
		AdminFormFillDialog<RegistrationRequest> dialog = new AdminFormFillDialog<>(msg, 
				msg.getMessage("AdminRegistrationFormLauncher.dialogCaption"), 
				editor, new AdminFormFillDialog.Callback<RegistrationRequest>()
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
}
