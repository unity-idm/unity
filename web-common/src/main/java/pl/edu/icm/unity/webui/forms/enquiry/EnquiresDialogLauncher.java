/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.List;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;

/**
 * Used to establish enquires to be filled and shows dialogs to fill them.
 *  
 * @author K. Benedyczak
 */
public class EnquiresDialogLauncher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiresDialogLauncher.class);
	private UnityMessageSource msg;
	private EnquiryResponseEditorController enquiryController;
	private WebAuthenticationProcessor authnProcessor;
	
	public EnquiresDialogLauncher(UnityMessageSource msg,
			EnquiryResponseEditorController enquiryController,
			WebAuthenticationProcessor authnProcessor)
	{
		this.msg = msg;
		this.enquiryController = enquiryController;
		this.authnProcessor = authnProcessor;
	}

	public void showEnquiryDialogIfNeeded()
	{
		List<EnquiryForm> formsToFill = enquiryController.getFormsToFill();
		if (!formsToFill.isEmpty())
			showEnquiryDialog(formsToFill.get(0));
	}
	
	private void showEnquiryDialog(EnquiryForm enquiry)
	{
		EnquiryResponseEditor editor;
		try
		{
			editor = enquiryController.getEditorInstance(enquiry, 
					RemotelyAuthenticatedContext.getLocalContext());
		} catch (Exception e)
		{
			log.error("Can't create an editor for enquiry form " + enquiry.getName(), e);
			return;
		}
		EnquiryFormFillDialog dialog = new EnquiryFormFillDialog(msg, 
				msg.getMessage("EnquiresDialogLauncher.caption"), editor, 
				new CallbackImpl(enquiry), enquiry.getType());
		dialog.show();
	}
	
	private class CallbackImpl implements EnquiryFormFillDialog.Callback
	{
		private EnquiryForm form;
		
		public CallbackImpl(EnquiryForm form)
		{
			this.form = form;
		}

		@Override
		public boolean newRequest(EnquiryResponse request)
		{
			boolean submitted = enquiryController.submitted(request, form, TriggeringMode.manualAtLogin);
			if (submitted)
				showEnquiryDialogIfNeeded();
			return submitted;
		}

		@Override
		public void cancelled()
		{
			enquiryController.cancelled(form, TriggeringMode.manualAtLogin);
			if (form.getType() == EnquiryType.REQUESTED_MANDATORY)
			{
				authnProcessor.logout(true);
			} else
			{
				showEnquiryDialogIfNeeded();
			}
		}

		@Override
		public void ignored()
		{
			enquiryController.markFormAsIgnored(form.getName());
			showEnquiryDialogIfNeeded();
		}
	}
}
