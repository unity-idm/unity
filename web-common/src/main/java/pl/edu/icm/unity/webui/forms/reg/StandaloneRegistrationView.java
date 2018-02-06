/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.common.ConfirmationComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.PostFormFillingHandler;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreator.RequestEditorCreatedCallback;

/**
 * Used to display a standalone (not within a dialog) registration form.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class StandaloneRegistrationView extends CustomComponent implements View
{
	private RegistrationForm form;
	private RegistrationsManagement regMan;
	private UnityMessageSource msg;
	private UnityServerConfiguration cfg;
	private IdPLoginController idpLoginController;
	private VerticalLayout main;
	private RequestEditorCreator editorCreator;
	
	@Autowired
	public StandaloneRegistrationView(UnityMessageSource msg,
			@Qualifier("insecure") RegistrationsManagement regMan,
			UnityServerConfiguration cfg, 
			IdPLoginController idpLoginController,
			RequestEditorCreator editorCreator)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.cfg = cfg;
		this.idpLoginController = idpLoginController;
		this.editorCreator = editorCreator;
	}
	
	public StandaloneRegistrationView init(RegistrationForm form)
	{
		this.form = form;
		return this;
	}
	
	@Override
	public void enter(ViewChangeEvent changeEvent)
	{		
		initUIBase();
		
		editorCreator.init(form, RemotelyAuthenticatedContext.getLocalContext());
		editorCreator.invoke(new RequestEditorCreatedCallback()
		{
			@Override
			public void onCreationError(Exception e)
			{
				handleError(e);
			}
			
			@Override
			public void onCreated(RegistrationRequestEditor editor)
			{
				editorCreated(editor);
			}

			@Override
			public void onCancel()
			{
				//nop
			}
		});
	}

	private void initUIBase()
	{
		main = new VerticalLayout();
	
		addStyleName("u-standalone-public-form");
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
	}
	
	private void editorCreated(RegistrationRequestEditor editor)
	{
		LocaleChoiceComponent localeChoice = new LocaleChoiceComponent(cfg, msg);
		
		main.addComponent(localeChoice);
		main.setComponentAlignment(localeChoice, Alignment.TOP_RIGHT);

		main.addComponent(editor);
		editor.setWidth(100, Unit.PERCENTAGE);
		main.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);

		HorizontalLayout buttons = new HorizontalLayout();
		
		Button ok = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"));
		ok.addStyleName(Styles.vButtonPrimary.toString());
		ok.addClickListener(event -> {
			accept(editor);
		});
		
		Button cancel = new Button(msg.getMessage("cancel"));
		cancel.addClickListener(event -> {
			RegistrationContext context = new RegistrationContext(false, 
					idpLoginController.isLoginInProgress(), 
					TriggeringMode.manualStandalone);
			new PostFormFillingHandler(idpLoginController, form, msg, 
					regMan.getFormAutomationSupport(form))
				.cancelled(true, context);
			showConfirm(Images.error,
					msg.getMessage("StandalonePublicFormView.requestCancelled"));
		});
		buttons.addComponents(cancel, ok);
		buttons.setMargin(false);
		main.addComponent(buttons);
		main.setComponentAlignment(buttons, Alignment.MIDDLE_CENTER);		
	}
	
	private void handleError(Exception e)
	{
		if (e instanceof IllegalArgumentException)
		{
			ErrorComponent ec = new ErrorComponent();
			ec.setError(e.getMessage());
			setCompositionRoot(ec);
		} else
		{
			ErrorComponent ec = new ErrorComponent();
			ec.setError("Can not open registration editor", e);
			setCompositionRoot(ec);
		}
	}
	
	private void accept(RegistrationRequestEditor editor)
	{
		RegistrationContext context = new RegistrationContext(true, 
				idpLoginController.isLoginInProgress(), 
				TriggeringMode.manualStandalone);
		RegistrationRequest request;
		try
		{
			request = editor.getRequest();
		} catch (Exception e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
		
		
		try
		{
			String requestId = regMan.submitRegistrationRequest(request, context);
			new PostFormFillingHandler(idpLoginController, form, msg, 
					regMan.getFormAutomationSupport(form))
				.submittedRegistrationRequest(requestId, regMan, request, context);
			showConfirm(Images.ok,
					msg.getMessage("StandalonePublicFormView.requestSubmitted"));
		} catch (WrongArgumentException e)
		{
			if (e instanceof IllegalFormContentsException)
				editor.markErrorsFromException((IllegalFormContentsException) e);
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		} catch (Exception e)
		{
			new PostFormFillingHandler(idpLoginController, form, msg, 
					regMan.getFormAutomationSupport(form))
				.submissionError(e, context);
			showConfirm(Images.error,
					msg.getMessage("StandalonePublicFormView.submissionFailed"));
		}
	}

	private void showConfirm(Images icon, String message)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		ConfirmationComponent confirmation = new ConfirmationComponent(icon, message);
		wrapper.addComponent(confirmation);
		wrapper.setComponentAlignment(confirmation, Alignment.MIDDLE_CENTER);
		wrapper.setSizeFull();
		setSizeFull();
		setCompositionRoot(wrapper);
	}
}
