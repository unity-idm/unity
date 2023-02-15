/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Used to establish enquires to be filled and shows dialogs to fill them.
 *  
 * @author K. Benedyczak
 */
@Component
public class EnquiresDialogLauncher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiresDialogLauncher.class);
	private MessageSource msg;
	private EnquiryResponseEditorControllerV8 enquiryController;
	private StandardWebLogoutHandler authnProcessor;
	
	@Autowired
	public EnquiresDialogLauncher(MessageSource msg,
			EnquiryResponseEditorControllerV8 enquiryController,
			StandardWebLogoutHandler authnProcessor)
	{
		this.msg = msg;
		this.enquiryController = enquiryController;
		this.authnProcessor = authnProcessor;
	}

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
		EnquiryFormFillDialog dialog = new EnquiryFormFillDialog(msg, 
				msg.getMessage("EnquiresDialogLauncher.caption"), editor, 
				new CallbackImpl(currentFormIndex, formsToFill, gotoNextUI), enquiry.getType());
		dialog.show();
	}
	
	private class CallbackImpl implements EnquiryFormFillDialog.Callback
	{
		private int currentFormIndex;
		private List<EnquiryForm> formsToFill;
		private Runnable gotoNextUI;
		
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
				NotificationPopup.showError(submitted.mainInformation, 
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
