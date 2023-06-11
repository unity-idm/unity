/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.finalization.WorkflowCompletedComponent;
import pl.edu.icm.unity.webui.forms.FormsUIHelper;

/**
 * Dialog allowing to fill a registration form. It takes an editor component as argument.
 * Dialog uses 2 buttons: submit request, cancel.
 * 
 * @author K. Benedyczak
 */
public class RegistrationFormFillDialog extends AbstractDialog
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RegistrationFormFillDialog.class);
	private RegistrationRequestEditor editor;
	private ImageAccessService imageAccessService;
	private Callback callback;
	private boolean onFinalScreen;
	private IdPLoginController idpLoginController;
	private boolean withSimplifiedFinalization;
	
	public RegistrationFormFillDialog(MessageSource msg, ImageAccessService imageAccessService, String caption, 
			RegistrationRequestEditor editor, Callback callback, IdPLoginController idpLoginController,
			boolean withSimplifiedFinalization)
	{
		super(msg, caption, msg.getMessage("RegistrationRequestEditorDialog.submitRequest"), 
				msg.getMessage("cancel"));
		this.editor = editor;
		this.callback = callback;
		this.idpLoginController = idpLoginController;
		this.withSimplifiedFinalization = withSimplifiedFinalization;
		this.imageAccessService = imageAccessService;
		setSizeMode(SizeMode.LARGE);
	}

	@Override
	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setMargin(false);
		vl.setSpacing(false);
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_CENTER);
		vl.setHeight(100, Unit.PERCENTAGE);
		return vl;
	}

	@Override
	protected void onCancel()
	{
		callback.cancelled();
		super.onCancel();
	}
	
	private void gotoFinalScreen(WorkflowFinalizationConfiguration config)
	{
		log.debug("Registration is finalized, status: {}", config);
		cancel.setVisible(false);
		onFinalScreen = true;
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		wrapper.setSizeFull();
		setSizeFull();
		setContent(wrapper);

		WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, 
			(p, url) -> redirect(p, url, idpLoginController), imageAccessService);
		wrapper.addComponent(finalScreen);
		wrapper.setComponentAlignment(finalScreen, Alignment.MIDDLE_CENTER);
	}
	
	private static void redirect(Page page, String redirectUrl, IdPLoginController loginController)
	{
		loginController.breakLogin();
		page.open(redirectUrl, null);
	}
	
	@Override
	protected void onConfirm()
	{
		if (onFinalScreen)
			close();
		else
			submitRequest();
	}
	
	private void submitRequest()
	{
		RegistrationRequest request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
		if (request == null)
			return;
		try
		{
			WorkflowFinalizationConfiguration config = callback.newRequest(request);
			if (withSimplifiedFinalization)
			{
				closeDialogAndShowInfo(config.mainInformation);
				if (config.isAutoLoginAfterSignUp())
					Page.getCurrent().reload();
			} else
			{
				gotoFinalScreen(config);
			}
		} catch (Exception e) 
		{
			FormsUIHelper.handleFormSubmissionError(e, msg, editor);
		}
	}
	
	private void closeDialogAndShowInfo(String mainInformation)
	{
		close();
		NotificationPopup.showNotice(mainInformation, mainInformation);
	}

	public interface Callback
	{
		WorkflowFinalizationConfiguration newRequest(RegistrationRequest request) throws WrongArgumentException;
		void cancelled();
	}
}
