/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint.forms.enquiry;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.*;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.components.WorkflowCompletedComponent;
import io.imunity.vaadin23.shared.endpoint.forms.registration.GetRegistrationCodeDialog;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.forms.*;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;
import pl.edu.icm.unity.webui.forms.enquiry.RewriteComboToEnquiryRequest;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.CENTER;
import static pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement.ENQUIRY_PATH;

@Route(value = ENQUIRY_PATH + ":" + StandalonePublicEnquiryView.FORM_PARAM)
public class StandalonePublicEnquiryView extends Composite<Div> implements HasDynamicTitle, BeforeEnterObserver
{
	public static final String FORM_PARAM = "form";
	public static final String REG_CODE_PARAM = "regcode";


	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StandalonePublicEnquiryView.class);

	MessageSource msg;
	private String registrationCode;
	private EnquiryResponseEditorControllerV23 editorController;
	private InvitationResolver invitationResolver;
	private PostFillingHandler postFillHandler;

	private EnquiryForm form;
	private EnquiryResponseEditor editor;
	private ResolvedInvitationParam invitation;
	private Long selectedEntity;

	private EnquiryManagement enqMan;
	private NotificationPresenter notificationPresenter;


	private final URLQueryPrefillCreator urlQueryPrefillCreator;
	private final EnquiryInvitationEntityChooserV23.InvitationEntityChooserComponentFactoryV23 entityChooserComponentFactory;

	@Autowired
	public StandalonePublicEnquiryView(EnquiryResponseEditorControllerV23 editorController,
	                                   InvitationResolver invitationResolver, MessageSource msg,
	                                   URLQueryPrefillCreator urlQueryPrefillCreator,
	                                   EnquiryInvitationEntityChooserV23.InvitationEntityChooserComponentFactoryV23 entityChooserComponentFactory,
	                                   EnquiryManagement enqMan,
	                                   NotificationPresenter notificationPresenter)
	{
		this.editorController = editorController;
		this.urlQueryPrefillCreator = urlQueryPrefillCreator;
		this.invitationResolver = invitationResolver;
		this.msg = msg;
		this.enqMan = enqMan;
		this.notificationPresenter = notificationPresenter;
		this.entityChooserComponentFactory = entityChooserComponentFactory;
		getContent().setClassName("u-standalone-public-form");
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
	public void beforeEnter(BeforeEnterEvent event)
	{
		form = event.getRouteParameters().get(FORM_PARAM)
				.map(this::getForm)
				.orElse(null);

		String pageTitle = Optional.ofNullable(form)
				.map(BaseForm::getPageTitle)
				.map(x -> x.getValue(msg))
				.orElse(null);
		postFillHandler = new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), true);

		registrationCode = event.getLocation().getQueryParameters()
				.getParameters()
				.getOrDefault(REG_CODE_PARAM, List.of())
				.stream().findFirst().orElse(null);

		enter();
	}

	private EnquiryForm getForm(String name)
	{
		try
		{
			return enqMan.getEnquiry(name);
		} catch (EngineException e)
		{
			log.error("Can't load registration forms", e);
		}
		return null;
	}

	public void enter()
	{
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

		try
		{
			invitation = invitationResolver.getInvitationByCode(registrationCode);
			invitation.assertMatchToForm(form);
		} catch (RegCodeException e)
		{
			log.error("Can not get invitation", e);
			handleError(e, e.cause);
			return;
		}

		if (invitation.entities.size() == 0)
		{
			log.error(
					"Enquiry invitation without any entities matching to contact address " + invitation.contactAddress);
			handleError(null, ErrorCause.UNRESOLVED_INVITATION);
			return;
		}

		if (invitation.entities.size() == 1)
		{
			processInvitation(invitation.entities.iterator().next().getId());
		} else
		{
			List<Entity> entitiesWitoutAnonymous = invitation.getEntitiesWithoutAnonymous();
			if (entitiesWitoutAnonymous.size() > 1)
			{
				showEntityChooser();
			} else
			{
				log.debug("Skipping enquiry entity choose step, only anonymous entities match to contact address "
						+ invitation.contactAddress);
				processInvitation(invitation.entities.iterator().next().getId());
			}
		}
	}

	

	private void processInvitation(Long entity)
	{
		selectedEntity = entity;

		EnquiryInvitationParam enqInvitation = invitation.getAsEnquiryInvitationParam(selectedEntity);

		try
		{
			PrefilledSet currentUserData = editorController.getPrefilledSetForSticky(form,
					new EntityParam(enqInvitation.getEntity()));
			PrefilledSet prefilled = mergeInvitationAndCurrentUserData(enqInvitation, currentUserData, form);
			prefilled = prefilled.mergeWith(urlQueryPrefillCreator.create(form));

			editor = editorController.getEditorInstanceForUnauthenticatedUser(form,
					enqInvitation.getFormPrefill()
							.getMessageParamsWithCustomVarObject(MessageTemplateDefinition.CUSTOM_VAR_PREFIX),
					RemotelyAuthenticatedPrincipal.getLocalContext(), prefilled,
					new EntityParam(enqInvitation.getEntity()));

		} catch (Exception e)
		{
			log.error("Can not setup enquiry editor", e);
			handleError(e, ErrorCause.MISCONFIGURED);
			return;
		}

		showEditorContent();
	}

	private PrefilledSet mergeInvitationAndCurrentUserData(EnquiryInvitationParam invitation, PrefilledSet fromUser,
			EnquiryForm form)
	{

		FormPrefill formPrefill = invitation.getFormPrefill();
		return new PrefilledSet(formPrefill.getIdentities(),
				mergePreffiledGroups(formPrefill.getAllowedGroups(), formPrefill.getGroupSelections(),
						fromUser.groupSelections, form),
				mergePreffiledAttributes(formPrefill.getAttributes(), fromUser.attributes),
				formPrefill.getAllowedGroups());
	}

	private Map<Integer, PrefilledEntry<Attribute>> mergePreffiledAttributes(
			Map<Integer, PrefilledEntry<Attribute>> fromInvitation, Map<Integer, PrefilledEntry<Attribute>> fromUser)
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

	private Map<Integer, PrefilledEntry<GroupSelection>> mergePreffiledGroups(
			Map<Integer, GroupSelection> allowedFromInvitiation,
			Map<Integer, PrefilledEntry<GroupSelection>> fromInvitation,
			Map<Integer, PrefilledEntry<GroupSelection>> fromUser, EnquiryForm form)
	{

		Map<Integer, PrefilledEntry<GroupSelection>> mergedGroups = new HashMap<>();

		if (fromUser.isEmpty())
		{
			return fromInvitation;
		}

		for (Entry<Integer, PrefilledEntry<GroupSelection>> entryFromUser : fromUser.entrySet())
		{
			PrefilledEntry<GroupSelection> fromInvitationG = fromInvitation.get(entryFromUser.getKey());

			if (fromInvitationG == null)
			{
				mergedGroups.put(entryFromUser.getKey(), entryFromUser.getValue());
				continue;
			}

			if (fromInvitationG.getMode().isInteractivelyEntered())
			{
				Set<String> mergedSet = new LinkedHashSet<>(fromInvitationG.getEntry().getSelectedGroups());
				mergedSet.addAll(entryFromUser.getValue().getEntry().getSelectedGroups());
				mergedGroups.put(entryFromUser.getKey(),
						new PrefilledEntry<>(
								new GroupSelection(mergedSet.stream().collect(Collectors.toList())),
								entryFromUser.getValue().getMode()));
			} else
			{
				mergedGroups.put(entryFromUser.getKey(), fromInvitationG);
			}

		}
		return mergedGroups;
	}

	private void showEditorContent()
	{
		VerticalLayout main = new VerticalLayout();
		main.setWidthFull();
		main.add(editor);
		editor.setWidthFull();
		main.setAlignItems(FlexComponent.Alignment.CENTER);
		Component buttonsBar = createButtonsBar();
		main.add(buttonsBar);
		getContent().add(main);
	}

	private void showEntityChooser()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		EnquiryInvitationEntityChooserV23 invitationEntityChooserComponent = entityChooserComponentFactory.get(invitation,
				this::processInvitation, () -> gotoFinalStep(cancel()));
		main.add(invitationEntityChooserComponent);
		main.setAlignItems(FlexComponent.Alignment.CENTER);
		getContent().add(main);
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
				}, msg.getMessage("GetEnquiryCodeDialog.title"), msg.getMessage("GetEnquiryCodeDialog.information"),
				msg.getMessage("GetEnquiryCodeDialog.code"));
		askForCodeDialog.open();
	}

	private Component createButtonsBar()
	{
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidth(editor.formWidth(), editor.formWidthUnit());

		Button okButton = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"),
				event ->
				{
					WorkflowFinalizationConfiguration config = submit(form, editor);
					gotoFinalStep(config);
				});

		Button cancelButton = new Button(msg.getMessage("cancel"), event ->
		{
			WorkflowFinalizationConfiguration config = cancel();
			gotoFinalStep(config);
		});

		buttons.add(cancelButton, okButton);
		buttons.setSpacing(true);
		buttons.setMargin(true);
		buttons.setPadding(true);
		buttons.setJustifyContentMode(CENTER);
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
			redirect(UI.getCurrent().getPage(), config.redirectURL);
		else
			showFinalScreen(config);
	}

	private void showFinalScreen(WorkflowFinalizationConfiguration config)
	{
		getContent().removeAll();
		log.debug("Enquiry is finalized, status: {}", config);
		WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, new Image(config.logoURL, ""));
		getContent().add(finalScreen);
	}

	private void redirect(Page page, String redirectUrl)
	{
		log.debug("Enquiry is finalized, redirecting to: {}", redirectUrl);
		page.open(redirectUrl, null);
	}

	private WorkflowFinalizationConfiguration submit(EnquiryForm form, EnquiryResponseEditor editor)
	{
		EnquiryResponse request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
		if (request == null)
			return null;
		request.setRegistrationCode(registrationCode);
		try
		{
			return editorController.submitted(request, form, TriggeringMode.manualStandalone,
					invitation == null ? Optional.empty()
							: Optional.of(new RewriteComboToEnquiryRequest(invitation.code, selectedEntity, form)));
		} catch (WrongArgumentException e)
		{
			handleFormSubmissionError(e, msg, editor);
			return null;
		}
	}

	public void handleFormSubmissionError(Exception e, MessageSource msg, EnquiryResponseEditor editor)
	{
		if (e instanceof IllegalFormContentsException)
		{
			editor.markErrorsFromException((IllegalFormContentsException) e);
			if (e instanceof IllegalFormContentsException.OccupiedIdentityUsedInRequest)
			{
				String identity = ((IllegalFormContentsException.OccupiedIdentityUsedInRequest) e).occupiedIdentity.getValue();
				notificationPresenter.showError(msg.getMessage("FormRequest.occupiedIdentity", identity), "");
			} else
			{
				notificationPresenter.showError(msg.getMessage("Generic.formError"), e.getMessage());
			}
		} else
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
		}
	}

	private WorkflowFinalizationConfiguration cancel()
	{
		return postFillHandler.getFinalRegistrationConfigurationOnError(TriggeringState.CANCELLED);
	}

	@Override
	public String getPageTitle()
	{
		return Optional.ofNullable(form.getPageTitle())
				.map(title -> title.getValue(msg))
				.orElse("");
	}
}
