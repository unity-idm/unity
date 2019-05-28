/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

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
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.finalization.WorkflowCompletedComponent;
import pl.edu.icm.unity.webui.forms.FormsInvitationHelper;
import pl.edu.icm.unity.webui.forms.FormsUIHelper;
import pl.edu.icm.unity.webui.forms.PrefilledSet;
import pl.edu.icm.unity.webui.forms.RegCodeException;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;
import pl.edu.icm.unity.webui.forms.StandalonePublicView;
import pl.edu.icm.unity.webui.forms.reg.GetRegistrationCodeDialog;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormDialogProvider;

/**
 * Provides public enquiry view. Used for enquiry invitation flow.
 * 
 * @author P.Piernik
 */
@PrototypeComponent
public class StandalonePublicEnquiryView extends CustomComponent implements StandalonePublicView
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StandalonePublicEnquiryView.class);
	
	private UnityMessageSource msg;
	
	private VerticalLayout main;
	private String registrationCode;
	private EnquiryResponseEditorController editorController;
	private FormsInvitationHelper invitationHelper;
	private PostFillingHandler postFillHandler;

	private EnquiryForm form;
	private EnquiryResponseEditor editor;
	
	
	@Autowired
	public StandalonePublicEnquiryView(EnquiryResponseEditorController editorController,
			@Qualifier("insecure") InvitationManagement invitationMan, UnityMessageSource msg)
	{
		this.editorController = editorController;
		this.invitationHelper = new FormsInvitationHelper(invitationMan);
		this.msg = msg;
	}

	@Override
	public String getFormName()
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
			askForCode(() -> doShowEditorOrSkipToFinalStep());
		} else
		{
			doShowEditorOrSkipToFinalStep();
		}
	}

	private void doShowEditorOrSkipToFinalStep()
	{
		EnquiryInvitationParam invitation;
		try
		{
			invitation = (EnquiryInvitationParam) getInvitationByCode(registrationCode);
		} catch (RegCodeException e)
		{
			log.error("Can not get invitation", e);
			handleError(e, e.cause);
			return;
		}
		PrefilledSet prefilled;
		try
		{
			prefilled = mergePrefilledSets(invitation, editorController.getPrefilledForSticky(form,
					new EntityParam(invitation.getEntity())), form);

			editor = editorController.getEditorInstance(form,
					RemotelyAuthenticatedContext.getLocalContext(), prefilled);

		} catch (Exception e)
		{
			log.error("Can not setup enquiry editor", e);
			handleError(e, ErrorCause.MISCONFIGURED);
			return;
		}
	
		showEditorContent(editor);
	}

	private PrefilledSet mergePrefilledSets(InvitationParam invitation, PrefilledSet fromUser, EnquiryForm form)
	{

		return new PrefilledSet(invitation.getIdentities(),
				mergePreffiledGroups(invitation.getAllowedGroups(),invitation.getGroupSelections(), fromUser.groupSelections, form),
				mergePreffiledAttributes(invitation.getAttributes(), fromUser.attributes),
				invitation.getAllowedGroups());
	}

	private Map<Integer, PrefilledEntry<Attribute>> mergePreffiledAttributes(
			Map<Integer, PrefilledEntry<Attribute>> fromInvitation,
			Map<Integer, PrefilledEntry<Attribute>> fromUser)
	{
		Map<Integer, PrefilledEntry<Attribute>> mergedAttributes = new HashMap<>();

		if (fromUser.isEmpty())
		{
			return fromInvitation;
		}

		for (Entry<Integer, PrefilledEntry<Attribute>> entryFromUser : fromUser.entrySet())
		{
			PrefilledEntry<Attribute> fromInvitationAttr = fromInvitation.get(entryFromUser.getKey());
			if (fromInvitationAttr != null)
			{
				mergedAttributes.put(entryFromUser.getKey(), fromInvitationAttr);
			} else
			{
				mergedAttributes.put(entryFromUser.getKey(), entryFromUser.getValue());
			}
		}
		return mergedAttributes;
	}

	private Map<Integer, PrefilledEntry<GroupSelection>> mergePreffiledGroups(Map<Integer, GroupSelection> allowedFromInvitiation,
			Map<Integer, PrefilledEntry<GroupSelection>> fromInvitation,
			Map<Integer, PrefilledEntry<GroupSelection>> fromUser, EnquiryForm form)
	{
		
		
		Map<Integer, PrefilledEntry<GroupSelection>> mergedGroups = new HashMap<>();
		
		if (fromUser.isEmpty())
		{
			return fromInvitation;	
		}
	
		for (Map.Entry<Integer, PrefilledEntry<GroupSelection>> entryFromUser : fromUser.entrySet())
		{
			PrefilledEntry<GroupSelection> fromInvitationG = fromInvitation.get(entryFromUser.getKey());

			if (fromInvitationG == null)
			{
				mergedGroups.put(entryFromUser.getKey(),entryFromUser.getValue());
				continue;
			}

			if (fromInvitationG.getMode().isInteractivelyEntered())
			{
				Set<String> mergedSet = new LinkedHashSet<>(
						fromInvitationG.getEntry().getSelectedGroups());
				mergedSet.addAll(entryFromUser.getValue().getEntry().getSelectedGroups());
				mergedGroups.put(entryFromUser.getKey(), new PrefilledEntry<GroupSelection>(new GroupSelection(
						mergedSet.stream().collect(Collectors.toList())),
						entryFromUser.getValue().getMode()));
			} else
			{
				mergedGroups.put(entryFromUser.getKey(), fromInvitationG);
			}

		}
		return mergedGroups;
	}
	
	private InvitationParam getInvitationByCode(String registrationCode) throws RegCodeException
	{
		if (registrationCode == null)
			throw new RegCodeException(ErrorCause.MISSING_CODE);
		
		InvitationParam invitation = invitationHelper.getInvitationByCode(registrationCode, InvitationType.ENQUIRY);
		
		if (invitation == null)
			throw new RegCodeException(ErrorCause.UNRESOLVED_INVITATION);
		if (invitation.isExpired())
			throw new RegCodeException(ErrorCause.EXPIRED_INVITATION);
		if (!invitation.getFormId().equals(form.getName()))
			throw new RegCodeException(ErrorCause.INVITATION_OF_OTHER_FORM);

		return invitation;
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

	private Component createButtonsBar()
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth(editor.formWidth(), editor.formWidthUnit());

		Button okButton = FormsUIHelper.createOKButton(
				msg.getMessage("RegistrationRequestEditorDialog.submitRequest"), event -> {
					WorkflowFinalizationConfiguration config = submit(form, editor);
					gotoFinalStep(config);
				});

		Button cancelButton = FormsUIHelper.createCancelButton(msg.getMessage("cancel"), event -> {
			WorkflowFinalizationConfiguration config = cancel();
			gotoFinalStep(config);
		});

		buttons.addComponents(cancelButton, okButton);
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
		log.debug("Enquiry is finalized, status: {}", config);
		WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, this::redirect);
		Component wrapper = finalScreen.getWrappedForFullSizeComponent();
		setCompositionRoot(wrapper);
		setSizeFull();
	}

	private void redirect(String redirectUrl)
	{
		log.debug("Enquiry is finalized, redirecting to: {}", redirectUrl);
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
