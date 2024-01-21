/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.forms;

import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.CommonViewParam;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.signup_and_enquiry.formfill.AdminEnquiryFormLauncher;
import io.imunity.console.views.signup_and_enquiry.formfill.AdminRegistrationFormLauncher;
import io.imunity.console.views.signup_and_enquiry.invitations.NewInvitationView;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.signupAndEnquiry.forms", parent = "WebConsoleMenu.signupAndEnquiry")
@Route(value = "/forms", layout = ConsoleMenu.class)
public class FormsView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final RegistrationFormsController registrationFormsController;
	private final EnquiryFormsController enquiryFormsController;
	private final NotificationPresenter notificationPresenter;
	private final AdminEnquiryFormLauncher adminEnquiryFormLauncher;
	private final AdminRegistrationFormLauncher adminRegistrationFormLauncher;
	private GridWithActionColumn<RegistrationForm> registrationFormsList;
	private GridWithActionColumn<EnquiryForm> enquiryFormsList;


	FormsView(MessageSource msg, RegistrationFormsController registrationFormsController,
			EnquiryFormsController enquiryFormsController, NotificationPresenter notificationPresenter, 
			AdminEnquiryFormLauncher adminEnquiryFormLauncher, AdminRegistrationFormLauncher adminRegistrationFormLauncher)
	{
		this.msg = msg;
		this.registrationFormsController = registrationFormsController;
		this.enquiryFormsController = enquiryFormsController;
		this.notificationPresenter = notificationPresenter;
		this.adminEnquiryFormLauncher = adminEnquiryFormLauncher;
		this.adminRegistrationFormLauncher = adminRegistrationFormLauncher;
		initRegistrationGridUI();
		initEnquiryGridUI();
	}

	private void initRegistrationGridUI()
	{
		VerticalLayout headerActionLayout = createHeaderActionLayout(msg, RegistrationView.class);
		registrationFormsList = new GridWithActionColumn<>(msg::getMessage, getRegistrationActionsHandlers());
		registrationFormsList.addHamburgerActions(getRegistrationHamburgerActionsHandlers());
		registrationFormsList
				.addComponentColumn(f -> new RouterLink(f.getName(), RegistrationView.class, f.getName()))
				.setHeader(msg.getMessage("RegistrationFormsComponent.nameCaption"))
				.setSortable(true)
				.setComparator(Comparator.comparing(DescribedObjectROImpl::getName))
				.setResizable(true)
				.setAutoWidth(true);
		registrationFormsList.addComponentColumn(form -> {
			if (!form.isPubliclyAvailable())
				return null;
			String linkURL = registrationFormsController.getPublicFormLink(form);
			return new Anchor(linkURL, linkURL, AnchorTarget.BLANK);
		})
				.setHeader(msg.getMessage("RegistrationFormsComponent.linkCaption"))
				.setResizable(true)
				.setAutoWidth(true);

		registrationFormsList.setItems(getRegistrationForms());

		VerticalLayout main = new VerticalLayout();
		H4 regCaption = new H4(msg.getMessage("RegistrationFormsComponent.caption"));
		main.add(regCaption);
		main.add(headerActionLayout);
		main.add(registrationFormsList);
		main.setWidthFull();
		main.setMargin(false);
		getContent().add(main);
	}

	private void initEnquiryGridUI()
	{
		VerticalLayout headerActionLayout = createHeaderActionLayout(msg, EnquiryView.class);
		enquiryFormsList = new GridWithActionColumn<>(msg::getMessage, getEnquiryActionsHandlers());
		enquiryFormsList.addHamburgerActions(getEnquiryHamburgerActionsHandlers());
		enquiryFormsList
				.addComponentColumn(f -> new RouterLink(f.getName(), EnquiryView.class, f.getName()))
				.setHeader(msg.getMessage("EnquiryFormsComponent.nameCaption"))
				.setSortable(true)
				.setComparator(Comparator.comparing(DescribedObjectROImpl::getName))
				.setResizable(true)
				.setAutoWidth(true);
		enquiryFormsList.addComponentColumn(form -> {
					String linkURL = enquiryFormsController.getPublicEnquiryLink(form);
					return new Anchor(linkURL, linkURL, AnchorTarget.BLANK);
				})
				.setHeader(msg.getMessage("EnquiryFormsComponent.linkCaption"))
				.setResizable(true)
				.setAutoWidth(true);

		enquiryFormsList.setItems(getEnquiryForms());

		VerticalLayout main = new VerticalLayout();
		H4 regCaption = new H4(msg.getMessage("EnquiryFormsComponent.caption"));
		main.add(regCaption);
		main.add(headerActionLayout);
		main.add(enquiryFormsList);
		main.setWidthFull();
		main.setMargin(false);
		getContent().add(main);
	}

	private List<SingleActionHandler<RegistrationForm>> getRegistrationActionsHandlers()
	{
		SingleActionHandler<RegistrationForm> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, RegistrationForm.class)
				.withHandler(r -> UI.getCurrent().navigate(RegistrationView.class, r.iterator().next().getName()))
				.build();
		return Collections.singletonList(edit);
	}

	private List<SingleActionHandler<EnquiryForm>> getEnquiryActionsHandlers()
	{
		SingleActionHandler<EnquiryForm> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, EnquiryForm.class)
				.withHandler(r -> UI.getCurrent().navigate(EnquiryView.class, r.iterator().next().getName()))
				.build();
		return Collections.singletonList(edit);
	}

	private List<SingleActionHandler<RegistrationForm>> getRegistrationHamburgerActionsHandlers()
	{
		SingleActionHandler<RegistrationForm> invite = SingleActionHandler.builder(RegistrationForm.class)
				.withCaption(msg.getMessage("RegistrationFormsComponent.invite"))
				.withIcon(VaadinIcon.ENVELOPE_OPEN)
				.withDisabledPredicate(form -> !(form.getRegistrationCode() == null
						&& form.isPubliclyAvailable()))
				.withHandler(items -> UI.getCurrent()
						.navigate(NewInvitationView.class,
								new QueryParameters(Map.of(CommonViewParam.name.name(), List.of(items.iterator()
										.next()
										.getName()), CommonViewParam.type.name(),
										List.of(InvitationType.REGISTRATION.toString())))))
				.build();

		SingleActionHandler<RegistrationForm> createRequest = SingleActionHandler
				.builder(RegistrationForm.class)
				.withCaption(msg.getMessage("RegistrationFormsComponent.createRequest"))
				.withIcon(VaadinIcon.FILE_ADD)
				.withHandler(items -> createRequest(items.iterator().next())).build();
		
		
		SingleActionHandler<RegistrationForm> clone = SingleActionHandler.builder(RegistrationForm.class)
				.withCaption(msg.getMessage("RegistrationFormsComponent.clone"))
				.withIcon(VaadinIcon.COPY)
				.withHandler(items -> UI.getCurrent().navigate(RegistrationView.class, QueryParameters.simple(Map.of("clone", items.iterator().next().getName()))))
				.build();

		SingleActionHandler<RegistrationForm> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, RegistrationForm.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(invite, createRequest, clone, remove);
	}
	
	private void createRequest(RegistrationForm form)
	{
		adminRegistrationFormLauncher.showRegistrationDialog(form,
				RemotelyAuthenticatedPrincipal.getLocalContext(), TriggeringMode.manualAdmin,
				this::handleError);
	}


	private List<SingleActionHandler<EnquiryForm>> getEnquiryHamburgerActionsHandlers()
	{
		SingleActionHandler<EnquiryForm> invite = SingleActionHandler.builder(EnquiryForm.class)
				.withCaption(msg.getMessage("EnquiryFormsComponent.invite"))
				.withIcon(VaadinIcon.ENVELOPE_OPEN)
				.withHandler(items -> UI.getCurrent()
						.navigate(NewInvitationView.class,
								new QueryParameters(Map.of(CommonViewParam.name.name(), List.of(items.iterator()
										.next()
										.getName()), CommonViewParam.type.name(),
										List.of(InvitationType.ENQUIRY.toString())))))
				.build();

		SingleActionHandler<EnquiryForm> createRequest = SingleActionHandler.builder(EnquiryForm.class)
				.withCaption(msg.getMessage("EnquiryFormsComponent.createResponse"))
				.withIcon(VaadinIcon.FILE_ADD)
				.withHandler(items -> createResponse(items.iterator().next())).build();
		
		SingleActionHandler<EnquiryForm> clone = SingleActionHandler.builder(EnquiryForm.class)
				.withCaption(msg.getMessage("EnquiryFormsComponent.clone"))
				.withIcon(VaadinIcon.COPY)
				.withHandler(items -> UI.getCurrent().navigate(EnquiryView.class, QueryParameters.simple(Map.of("clone", items.iterator().next().getName()))))
				.build();

		SingleActionHandler<EnquiryForm> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, EnquiryForm.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(invite, createRequest, clone, remove);
	}

	private void createResponse(EnquiryForm form)
	{
		adminEnquiryFormLauncher.showDialog(form, RemotelyAuthenticatedPrincipal.getLocalContext(),
				this::handleError);
	}

	private void handleError(Exception error)
	{
		notificationPresenter.showError(msg.getMessage("EnquiryFormsComponent.errorShowFormEdit"), error.getMessage());
	}
	
	private Collection<RegistrationForm> getRegistrationForms()
	{
		try
		{
			return registrationFormsController.getRegistrationForms();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
		}
		return Collections.emptyList();
	}

	private Collection<EnquiryForm> getEnquiryForms()
	{
		try
		{
			return enquiryFormsController.getEnquiryForms();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
		}
		return Collections.emptyList();
	}

	private void remove(RegistrationForm form, boolean dropRequests)
	{
		try
		{
			registrationFormsController.removeRegistrationForm(form, dropRequests);
			registrationFormsList.removeElement(form);
		} catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
		}
	}

	private void remove(EnquiryForm form, boolean dropRequests)
	{
		try
		{
			enquiryFormsController.removeEnquiryForm(form, dropRequests);
			enquiryFormsList.removeElement(form);
		} catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
		}
	}

	private void tryRemove(RegistrationForm form)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, Sets.newHashSet(form));
		Checkbox dropRequests = new Checkbox(msg.getMessage("RegistrationFormsComponent.dropRequests"));
		ConfirmDialog confirmDialog = new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("RegistrationFormsComponent.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> remove(form, dropRequests.getValue()),
				msg.getMessage("cancel"),
				e -> {}
		);
		confirmDialog.add(dropRequests);
		confirmDialog.open();
	}

	private void tryRemove(EnquiryForm form)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, Sets.newHashSet(form));
		Checkbox dropRequests = new Checkbox(msg.getMessage("EnquiryFormsComponent.dropRequests"));
		ConfirmDialog confirmDialog = new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("EnquiryFormsComponent.confirmDelete", confirmText),
				msg.getMessage("ok"),
				e -> remove(form, dropRequests.getValue()),
				msg.getMessage("cancel"),
				e -> {}
		);
		confirmDialog.add(dropRequests);
		confirmDialog.open();
	}
}
