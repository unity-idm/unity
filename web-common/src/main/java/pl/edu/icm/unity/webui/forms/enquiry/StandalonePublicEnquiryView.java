/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.finalization.WorkflowCompletedComponent;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.reg.GetRegistrationCodeDialog;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormDialogProvider;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreator.ErrorCause;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreator.RegCodeException;

/**
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class StandalonePublicEnquiryView extends CustomComponent implements View
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StandalonePublicEnquiryView.class);
	
	private UnityMessageSource msg;
	
	private VerticalLayout main;
	private String registrationCode;
	private EnquiryResponseEditorController editorController;
	private InvitationManagement invitationMan;
	private PostFillingHandler postFillHandler;
	
	private EnquiryForm form;
	private EnquiryResponseEditor editor;
	
	
	@Autowired
	public StandalonePublicEnquiryView(EnquiryResponseEditorController editorController,
			@Qualifier("insecure") InvitationManagement invitationMan, UnityMessageSource msg)
	{
		this.editorController = editorController;
		this.invitationMan = invitationMan;
		this.msg = msg;
	}

	String getFormName()
	{
		if (form == null)
			return null;
		return form.getName();
	}

	public StandalonePublicEnquiryView init(EnquiryForm form)
	{
		this.form = form;
		String pageTitle = form.getPageTitle() == null ? null : form.getPageTitle().getValue(msg);
		this.postFillHandler = new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg, pageTitle,
				form.getLayoutSettings().getLogoURL(), false);
		return this;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		initUIBase();
		if (registrationCode == null)
			registrationCode = RegistrationFormDialogProvider.getCodeFromURL();

		if (registrationCode == null)
		{
			askForCode(() -> doCreateFirstStage());
		} else
		{
			doCreateFirstStage();
		}
	}

	private void doCreateFirstStage()
	{
		EnquiryInvitationParam invitation;
		try
		{
			invitation = getInvitationByCode(registrationCode);
		} catch (RegCodeException e)
		{
			handleError(e, e.cause);
			return;
		}

		try
		{
			PrefilledSet prefilled = new PrefilledSet();
			if (invitation != null)
			{
				prefilled = new PrefilledSet(invitation.getIdentities(),
						invitation.getGroupSelections(), invitation.getAttributes(),
						invitation.getAllowedGroups());
			}

			editor = editorController.getEditorInstance(form,
					RemotelyAuthenticatedContext.getLocalContext(), prefilled);
			showEditorContent(editor);
		} catch (Exception e)
		{
			log.error("Can not get enquiry editor" , e);
			handleError(e, ErrorCause.MISCONFIGURED);
		}
	}

	private EnquiryInvitationParam getInvitationByCode(String registrationCode) throws RegCodeException
	{

		if (registrationCode == null)
			throw new RegCodeException(ErrorCause.MISSING_CODE);

		InvitationWithCode inv = getInvitationInternal(registrationCode);
		EnquiryInvitationParam invitation = null;
		if (inv != null && inv.getInvitation() != null)
		{
			if (inv.getInvitation().getType().equals(InvitationType.ENQUIRY))
			{
				invitation = (EnquiryInvitationParam) inv.getInvitation();
			}
		}

		if (invitation == null)
			throw new RegCodeException(ErrorCause.UNRESOLVED_INVITATION);
		if (invitation.isExpired())
			throw new RegCodeException(ErrorCause.EXPIRED_INVITATION);
		if (!invitation.getFormId().equals(form.getName()))
			throw new RegCodeException(ErrorCause.INVITATION_OF_OTHER_FORM);

		return invitation;
	}

	private InvitationWithCode getInvitationInternal(String code)
	{
		try
		{
			return invitationMan.getInvitation(code);
		} catch (IllegalArgumentException e)
		{
			// ok
			return null;
		} catch (EngineException e)
		{
			log.warn("Error trying to check invitation with user provided code", e);
			return null;
		}
	}

	private void showEditorContent(EnquiryResponseEditor editor)
	{
		main.addComponent(editor);
		editor.setWidth(100, Unit.PERCENTAGE);
		main.setComponentAlignment(editor, Alignment.MIDDLE_CENTER);
		Component buttonsBar = createButtonsBar();
		main.addComponent(buttonsBar);
		main.setComponentAlignment(buttonsBar, Alignment.MIDDLE_CENTER);

	}

	private void askForCode(Runnable uiCreator)
	{
		GetRegistrationCodeDialog askForCodeDialog = new GetRegistrationCodeDialog(msg,
				new GetRegistrationCodeDialog.Callback()
				{
					@Override
					public void onCodeGiven(String code)
					{
						registrationCode = code;
						uiCreator.run();
					}

					@Override
					public void onCancel()
					{
						cancel();
					}
				}, msg.getMessage("GetEnquiryCodeDialog.title"),
				msg.getMessage("GetEnquiryCodeDialog.information"),
				msg.getMessage("GetEnquiryCodeDialog.code"));
		askForCodeDialog.show();
	}

	private void initUIBase()
	{
		if (form.getPageTitle() != null)
			Page.getCurrent().setTitle(form.getPageTitle().getValue(msg));
		main = new VerticalLayout();
		addStyleName("u-standalone-public-form");
		setCompositionRoot(main);
		setWidth(100, Unit.PERCENTAGE);
	}

	protected Component createButtonsBar()
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth(editor.formWidth(), editor.formWidthUnit());

		Button ok = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"));
		ok.setWidth(100, Unit.PERCENTAGE);
		ok.addStyleName(Styles.vButtonPrimary.toString());
		ok.addClickListener(event -> {
			 WorkflowFinalizationConfiguration config =
			 submit(form, editor);
			 gotoFinalStep(config);
		});

		Button cancel = new Button(msg.getMessage("cancel"));
		cancel.setWidth(100, Unit.PERCENTAGE);
		cancel.addClickListener(event -> {
			 WorkflowFinalizationConfiguration config = cancel();
			 gotoFinalStep(config);
		});
		
		buttons.addComponents(cancel, ok);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		return buttons;
	}
	
	private void handleError(Exception e, ErrorCause cause)
	{
		WorkflowFinalizationConfiguration finalScreenConfig = postFillHandler
				.getFinalRegistrationConfigurationOnError(cause.getTriggerState());
		gotoFinalStep(finalScreenConfig);
	}

	private void gotoFinalStep(WorkflowFinalizationConfiguration config)
	{
		if (config == null)
			return;
		if (config.autoRedirect)
			redirect(config.redirectURL);
		else
			showFinalScreen(config);
	}

	private void showFinalScreen(WorkflowFinalizationConfiguration config)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		wrapper.setSizeFull();
		setSizeFull();
		setCompositionRoot(wrapper);

		Component finalScreen = new WorkflowCompletedComponent(config, this::redirect);
		wrapper.addComponent(finalScreen);
		wrapper.setComponentAlignment(finalScreen, Alignment.MIDDLE_CENTER);
	}

	private void redirect(String redirectUrl)
	{
		Page.getCurrent().open(redirectUrl, null);
	}
	
	private WorkflowFinalizationConfiguration submit(EnquiryForm form, EnquiryResponseEditor editor)
	{
		EnquiryResponse request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
		if (request == null)
			return null;
		request.setRegistrationCode(registrationCode);
		try
		{
			return editorController.submitted(request, form, TriggeringMode.manualStandalone);
		} catch (WrongArgumentException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			if (e instanceof IllegalFormContentsException)
				editor.markErrorsFromException((IllegalFormContentsException) e);
			return null;
		}
	}
	
	private WorkflowFinalizationConfiguration cancel()
	{	
		return postFillHandler.getFinalRegistrationConfigurationOnError(
				TriggeringState.CANCELLED);
	}

	public void refresh(VaadinRequest request)
	{
		if (editor != null)
			editor.focusFirst();	
	}
}
