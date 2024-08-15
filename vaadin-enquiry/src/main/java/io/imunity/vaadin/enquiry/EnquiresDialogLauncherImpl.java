/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.enquiry;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.EnquiresDialogLauncher;
import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;

import java.util.List;
import java.util.Optional;

/**
 * Used to establish enquires to be filled and shows dialogs to fill them.
 */
@Component
class EnquiresDialogLauncherImpl implements EnquiresDialogLauncher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiresDialogLauncherImpl.class);
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final EnquiryResponseEditorController enquiryController;
	private final VaadinWebLogoutHandler authnProcessor;
	
	EnquiresDialogLauncherImpl(MessageSource msg,
	                                  NotificationPresenter notificationPresenter,
	                                  EnquiryResponseEditorController enquiryController,
	                                  VaadinWebLogoutHandler authnProcessor)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		this.enquiryController = enquiryController;
		this.authnProcessor = authnProcessor;
	}

	@Override
	public void showEnquiryDialogIfNeeded(Runnable gotoNextUI)
	{
		List<EnquiryForm> formsToFill = enquiryController.getRegularFormsToFill();
		if (!formsToFill.isEmpty())
			showEnquiryDialog(0, formsToFill, gotoNextUI);
		else
			gotoNextUI.run();
	}
	
	private void showEnquiryDialog(int currentFormIndex, List<EnquiryForm> formsToFill, Runnable gotoNextUI)
	{
		EnquiryForm enquiry = formsToFill.get(currentFormIndex);
		EnquiryResponseEditor editor;
		try
		{
			editor = enquiryController.getEditorInstanceForAuthenticatedUser(enquiry, 
					RemotelyAuthenticatedPrincipal.getLocalContext());
		} catch (Exception e)
		{
			log.error("Can't create an editor for enquiry form " + enquiry.getName(), e);
			return;
		}
		EnquiryFormFillDialog dialog = new EnquiryFormFillDialog(msg, notificationPresenter, editor, 
				new CallbackImpl(currentFormIndex, formsToFill, gotoNextUI), enquiry.getType());
		dialog.open();
	}
	
	private class CallbackImpl implements EnquiryFormFillDialog.Callback
	{
		private int currentFormIndex;
		private final List<EnquiryForm> formsToFill;
		private final Runnable gotoNextUI;
		
		public CallbackImpl(int currentFormIndex, List<EnquiryForm> formsToFill, Runnable gotoNextUI)
		{
			this.currentFormIndex = currentFormIndex;
			this.formsToFill = formsToFill;
			this.gotoNextUI = gotoNextUI;
		}

		@Override
		public void newRequest(EnquiryResponse request) throws WrongArgumentException
		{
			WorkflowFinalizationConfiguration submitted = enquiryController.submitted(request, formsToFill.get(currentFormIndex), 
					TriggeringMode.manualAtLogin, Optional.empty());
			//auto redirect is ignored when in dialog
			if (!submitted.autoRedirect && !submitted.success)
			{
				notificationPresenter.showError(submitted.mainInformation,
						submitted.extraInformation == null ? "" : submitted.extraInformation);
			}
			showNextIfNeeded();
		}

		@Override
		public void cancelled()
		{
			enquiryController.cancelled(formsToFill.get(currentFormIndex), 
					TriggeringMode.manualAtLogin, false);
			if (formsToFill.get(currentFormIndex).getType() == EnquiryType.REQUESTED_MANDATORY)
			{
				authnProcessor.logout(true);
			} else
			{
				showNextIfNeeded();
			}
		}

		@Override
		public void ignored()
		{
			enquiryController.markFormAsIgnored(formsToFill.get(currentFormIndex).getName());
			showNextIfNeeded();
		}
		
		private void showNextIfNeeded()
		{
			currentFormIndex++;
			if (formsToFill.size() > currentFormIndex)
				showEnquiryDialog(currentFormIndex, formsToFill, gotoNextUI);
			else
				gotoNextUI.run();
		}
	}
}
