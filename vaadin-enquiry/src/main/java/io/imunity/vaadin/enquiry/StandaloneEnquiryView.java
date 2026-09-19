/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.enquiry;

import static com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.CENTER;
import static pl.edu.icm.unity.engine.api.endpoint.SecuredSharedEndpointPaths.SEC_ENQUIRY_PATH;
import static pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement.ENQUIRY_PATH;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.VaadinWebLogoutHandler;
import io.imunity.vaadin.endpoint.common.forms.InvitationResolver;
import io.imunity.vaadin.endpoint.common.forms.PrefilledSet;
import io.imunity.vaadin.endpoint.common.forms.RegCodeException;
import io.imunity.vaadin.endpoint.common.forms.RegCodeException.ErrorCause;
import io.imunity.vaadin.endpoint.common.forms.ResolvedInvitationParam;
import io.imunity.vaadin.endpoint.common.forms.URLQueryPrefillCreator;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import io.imunity.vaadin.endpoint.common.forms.components.GetRegistrationCodeDialog;
import io.imunity.vaadin.endpoint.common.forms.components.WorkflowCompletedComponent;
import io.imunity.vaadin.endpoint.common.layout.WrappedLayout;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.api.utils.NameToURLEncoder;

@PermitAll
@RouteAlias(value = SEC_ENQUIRY_PATH + ":" + StandaloneEnquiryView.FORM_PARAM, layout = WrappedLayout.class)
@Route(value = ENQUIRY_PATH + ":" + StandaloneEnquiryView.FORM_PARAM, layout = WrappedLayout.class)
class StandaloneEnquiryView extends UnityViewComponent implements BeforeEnterObserver
{
	public static final String FORM_PARAM = "form";
	public static final String REG_CODE_PARAM = "regcode";
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StandaloneEnquiryView.class);

	private final EnquiryManagement enqMan;
	private final NotificationPresenter notificationPresenter;
	private final VaadinLogoImageLoader logoImageLoader;
	private final MessageSource msg;
	private final URLQueryPrefillCreator urlQueryPrefillCreator;
	private final EnquiryInvitationEntityChooser.InvitationEntityChooserComponentFactory entityChooserComponentFactory;
	private final VaadinWebLogoutHandler authnProcessor;

	private String registrationCode;
	private final EnquiryResponseEditorController editorController;
	private final InvitationResolver invitationResolver;
	private PostFillingHandler postFillHandler;
	private EnquiryForm form;
	private EnquiryResponseEditor editor;
	private ResolvedInvitationParam invitation;
	private Long selectedEntity;
	private RewriteComboToEnquiryRequest comboToEnquiryRequest;
	private Map<String, List<String>> parameters;


	@Autowired
	public StandaloneEnquiryView(EnquiryResponseEditorController editorController,
	                             InvitationResolver invitationResolver, MessageSource msg,
	                             URLQueryPrefillCreator urlQueryPrefillCreator,
	                             EnquiryInvitationEntityChooser.InvitationEntityChooserComponentFactory entityChooserComponentFactory,
	                             EnquiryManagement enqMan, VaadinLogoImageLoader logoImageLoader,
	                             VaadinWebLogoutHandler authnProcessor,
	                             NotificationPresenter notificationPresenter)
	{
		this.editorController = editorController;
		this.urlQueryPrefillCreator = urlQueryPrefillCreator;
		this.invitationResolver = invitationResolver;
		this.msg = msg;
		this.enqMan = enqMan;
		this.logoImageLoader = logoImageLoader;
		this.notificationPresenter = notificationPresenter;
		this.entityChooserComponentFactory = entityChooserComponentFactory;
		this.authnProcessor = authnProcessor;
		getContent().setClassName("u-standalone-public-form");
	}

	public StandaloneEnquiryView init(EnquiryForm form)
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
		parameters = event.getLocation().getQueryParameters().getParameters();

		if(form == null)
		{
			notificationPresenter.showError(msg.getMessage("EnquiryErrorName.title"), msg.getMessage("EnquiryErrorName.description"));
			return;
		}

		String pageTitle = form.getPageTitle().getValue(msg);
		postFillHandler = new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), false);

		registrationCode = event.getLocation().getQueryParameters()
				.getParameters()
				.getOrDefault(REG_CODE_PARAM, List.of())
				.stream().findFirst().orElse(null);
		enter();
	}

	private EnquiryForm getForm(String name)
	{
		String nameDecoded = NameToURLEncoder.decode(name);
		try
		{
			return enqMan.getEnquiry(nameDecoded);
		} catch (EngineException e)
		{
			log.error("Can't load registration forms", e);
		}
		return null;
	}

	public void enter()
	{
		if(InvocationContext.getCurrent().getLoginSession() != null)
		{
			showContentForLoggedInUser();
		}
		else if (registrationCode == null)
		{
			askForCode(this::doShowEditorOrSkipToFinalStep);
		} else
		{
			doShowEditorOrSkipToFinalStep();
		}
	}

	private void showContentForLoggedInUser()
	{
		if (!editorController.isFormApplicableForLoggedEntity(form.getName(), registrationCode == null))
		{
			log.debug("Enquiry form {} is not applicable", form.getName());
			handleError(TriggeringState.NOT_APPLICABLE_ENQUIRY);
			return;
		}

		ResolvedInvitationParam resolvedInvitationParam = null;
		PrefilledSet prefilledSet = urlQueryPrefillCreator.create(form, parameters);
		if (registrationCode != null)
		{
			try
			{
				resolvedInvitationParam = invitationResolver.getInvitationByCode(registrationCode);
				resolvedInvitationParam.assertMatchToForm(form);
				prefilledSet = getPrefilledFromInvitation(resolvedInvitationParam.getAsEnquiryInvitationParam(
						InvocationContext.getCurrent().getLoginSession().getEntityId()));

			} catch (RegCodeException e)
			{
				log.error("Can not get invitation", e);
				handleError(ErrorCause.MISSING_CODE);
			}
		}

		try
		{
			editor = editorController.getEditorInstanceForAuthenticatedUser(form, prefilledSet,
					RemotelyAuthenticatedPrincipal.getLocalContext());
			comboToEnquiryRequest = Optional.ofNullable(resolvedInvitationParam)
					.map(param -> new RewriteComboToEnquiryRequest(param.code, InvocationContext.getCurrent().getLoginSession().getEntityId(), form))
					.orElse(null);
		} catch (Exception e)
		{
			log.error("Can not setup enquiry editor", e);
			handleError(ErrorCause.MISCONFIGURED);
			return;
		}

		if (form.getType().equals(EnquiryForm.EnquiryType.STICKY))
		{
			try
			{
				if(editorController.checkIfRequestExistsForLoggedUser(form.getName()))
				{
					showRemoveLastRequestQuestionScreen();
					return;
				}
			} catch (Exception e)
			{
				log.warn("Can't check if enquiry request exists", e);
				notificationPresenter.showError(msg.getMessage("EnquiryErrorName.title"), msg.getMessage("EnquiryErrorName.description"));
				return;
			}
		}
		VerticalLayout editorContent = getEditorContent();
		editorContent.addComponentAsFirst(getLogoutButtonLayout(msg.getMessage("EnquiryWellKnownURLView.resignLogout")));
		getContent().add(editorContent);
	}

	private PrefilledSet getPrefilledFromInvitation(EnquiryInvitationParam invitation) throws RegCodeException
	{
		if (invitation != null)
		{
			FormPrefill formPrefill = invitation.getFormPrefill();
			return new PrefilledSet(formPrefill.getIdentities(), formPrefill.getGroupSelections(),
					formPrefill.getAttributes(), formPrefill.getAllowedGroups());
		}
		return new PrefilledSet();
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
			handleError(e.cause);
			return;
		}

		if (invitation.entities.size() == 0)
		{
			log.error("Enquiry invitation without any entities matching to contact address " + invitation.contactAddress);
			handleError(ErrorCause.UNRESOLVED_INVITATION);
			return;
		}

		if (invitation.entities.size() == 1)
		{
			processInvitation(invitation.entities.iterator().next().getId());
		}
		else
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
			PrefilledSet prefilled = mergeInvitationAndCurrentUserData(enqInvitation, currentUserData);
			prefilled = prefilled.mergeWith(urlQueryPrefillCreator.create(form, parameters));

			editor = editorController.getEditorInstanceForUnauthenticatedUser(form,
					enqInvitation.getFormPrefill()
							.getMessageParamsWithCustomVarObject(MessageTemplateDefinition.CUSTOM_VAR_PREFIX),
					RemotelyAuthenticatedPrincipal.getLocalContext(), prefilled,
					new EntityParam(enqInvitation.getEntity()));
			comboToEnquiryRequest = Optional.ofNullable(invitation)
					.map(param -> new RewriteComboToEnquiryRequest(invitation.code, selectedEntity, form))
					.orElse(null);

		} catch (Exception e)
		{
			log.error("Can not setup enquiry editor", e);
			handleError(ErrorCause.MISCONFIGURED);
			return;
		}

		VerticalLayout editorContent = getEditorContent();
		getContent().add(editorContent);
	}

	private PrefilledSet mergeInvitationAndCurrentUserData(EnquiryInvitationParam invitation, PrefilledSet fromUser)
	{

		FormPrefill formPrefill = invitation.getFormPrefill();
		return new PrefilledSet(formPrefill.getIdentities(),
				mergePreffiledGroups(formPrefill.getGroupSelections(), fromUser.groupSelections),
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
			Map<Integer, PrefilledEntry<GroupSelection>> fromInvitation,
			Map<Integer, PrefilledEntry<GroupSelection>> fromUser)
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
								new GroupSelection(new ArrayList<>(mergedSet)),
								entryFromUser.getValue().getMode()));
			} else
			{
				mergedGroups.put(entryFromUser.getKey(), fromInvitationG);
			}

		}
		return mergedGroups;
	}

	private VerticalLayout getEditorContent()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(false);
		main.setWidthFull();
		main.add(editor);
		editor.setWidthFull();
		main.setAlignItems(FlexComponent.Alignment.CENTER);
		Component buttonsBar = createButtonsBar();
		main.add(buttonsBar);
		return main;
	}

	private void showRemoveLastRequestQuestionScreen()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setAlignItems(FlexComponent.Alignment.CENTER);
		main.setJustifyContentMode(CENTER);

		H3 info = new H3(msg.getMessage("StandaloneStickyEnquiryView.overwriteRequestInfo"));
		info.addClassName("u-reg-title");
		main.add(info);

		HorizontalLayout buttons = new HorizontalLayout();
		main.add(buttons);

		Button removeLast = new Button(msg.getMessage("StandaloneStickyEnquiryView.removeLastRequest"));
		removeLast.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		removeLast.getStyle().set("width", "17em");
		removeLast.addClickListener(event ->
		{
			try
			{
				editorController.removePendingRequest(form.getName());
			} catch (Exception e)
			{
				// ok, we remove request before submit
				log.warn("Can not remove pending request for form " + form.getName());
			}
			getContent().removeAll();
			VerticalLayout editorContent = getEditorContent();
			getContent().add(editorContent);
		});

		Button cancelButton = getCancelButton();
		cancelButton.setSizeUndefined();
		buttons.add(cancelButton, removeLast);
		buttons.setMargin(false);
		buttons.setPadding(false);

		getContent().add(main);
		getContent().setSizeFull();
	}

	private void showEntityChooser()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		EnquiryInvitationEntityChooser invitationEntityChooserComponent = entityChooserComponentFactory.get(invitation,
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
			}
		);
		okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		okButton.setWidthFull();

		Button cancelButton = getCancelButton();

		buttons.add(cancelButton, okButton);
		buttons.setMargin(false);
		buttons.setPadding(false);
		buttons.setJustifyContentMode(CENTER);
		return buttons;
	}

	private Button getCancelButton()
	{
		Button cancelButton = new Button(msg.getMessage("cancel"), event ->
		{
			WorkflowFinalizationConfiguration config = cancel();
			gotoFinalStep(config);
		});
		cancelButton.setWidthFull();
		return cancelButton;
	}

	private void handleError(ErrorCause cause)
	{
		handleError(cause.getTriggerState());
	}
	
	private void handleError(TriggeringState cause)
	{
		WorkflowFinalizationConfiguration finalScreenConfig = postFillHandler
				.getFinalRegistrationConfigurationOnError(cause);
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
		Image logo = logoImageLoader.loadImageFromUri(config.logoURL).orElse(null);
		WorkflowCompletedComponent finalScreen = new WorkflowCompletedComponent(config, logo);
		finalScreen.setMargin(false);
		if(InvocationContext.getCurrent().getLoginSession() != null)
		{
			finalScreen.addComponentAsFirst(getLogoutButtonLayout(msg.getMessage("MainHeader.logout")));
		}
		getContent().add(finalScreen);
	}

	private VerticalLayout getLogoutButtonLayout(String buttonTxt)
	{
		VerticalLayout layout = new VerticalLayout(
				new LinkButton(
						buttonTxt,
						e -> authnProcessor.logout(true, SEC_ENQUIRY_PATH + NameToURLEncoder.encode(form.getName()))
				)
		);
		layout.setAlignItems(FlexComponent.Alignment.END);
		layout.setMargin(false);
		layout.setPadding(false);
		layout.setWidthFull();
		return layout;
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
			return editorController.submitted(request, form, TriggeringMode.manualStandalone, Optional.ofNullable(comboToEnquiryRequest));
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
			notificationPresenter.showError(msg.getMessage("Generic.formError"), e.getMessage());
		}
	}

	private WorkflowFinalizationConfiguration cancel()
	{
		return postFillHandler.getFinalRegistrationConfigurationOnError(TriggeringState.CANCELLED);
	}

	@Override
	public String getPageTitle()
	{
		return Optional.ofNullable(form)
				.map(form -> Optional.ofNullable(form.getPageTitle())
						.map(title -> title.getValue(msg))
						.orElse(form.getName())
				)
				.orElse("");
	}
}
