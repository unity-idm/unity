/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.forms;

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
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.authentication.realms.RealmEditView;
import io.imunity.console.views.signup_and_enquiry.invitations.NewInvitationView;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.*;

import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.signupAndEnquiry.forms", parent = "WebConsoleMenu.signupAndEnquiry")
@Route(value = "/forms", layout = ConsoleMenu.class)
public class FormsView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final RegistrationFormsController controller;
	private final NotificationPresenter notificationPresenter;
	private GridWithActionColumn<RegistrationForm> registrationFormsList;

	public FormsView(MessageSource msg, RegistrationFormsController controller, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
		initUI();
	}

	private void initUI()
	{
		VerticalLayout headerActionLayout = createHeaderActionLayout(msg, RegistrationView.class);
		registrationFormsList = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
		registrationFormsList.addHamburgerActions(getHamburgerActionsHandlers());
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
			String linkURL = controller.getPublicFormLink(form);
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

	private List<SingleActionHandler<RegistrationForm>> getActionsHandlers()
	{
		SingleActionHandler<RegistrationForm> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, RegistrationForm.class)
				.withHandler(r -> UI.getCurrent().navigate(RegistrationView.class, r.iterator().next().getName()))
				.build();
		return Collections.singletonList(edit);

	}

	private List<SingleActionHandler<RegistrationForm>> getHamburgerActionsHandlers()
	{
		SingleActionHandler<RegistrationForm> invite = SingleActionHandler.builder(RegistrationForm.class)
				.withCaption(msg.getMessage("RegistrationFormsComponent.invite"))
				.withIcon(VaadinIcon.ENVELOPE_OPEN)
				.withDisabledPredicate(form -> !(form.getRegistrationCode() == null
						&& form.isPubliclyAvailable()))
				.withHandler(items -> UI.getCurrent().navigate(NewInvitationView.class, items.iterator().next().getName()))
				.build();

		SingleActionHandler<RegistrationForm> clone = SingleActionHandler.builder(RegistrationForm.class)
				.withCaption(msg.getMessage("RegistrationFormsComponent.clone"))
				.withIcon(VaadinIcon.COPY)
				.withHandler(items -> UI.getCurrent().navigate(RegistrationView.class, QueryParameters.simple(Map.of("clone", items.iterator().next().getName()))))
				.build();

		SingleActionHandler<RegistrationForm> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, RegistrationForm.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(invite, clone, remove);

	}

	private void handleError(Exception error)
	{
		notificationPresenter.showError(msg.getMessage("RegistrationFormsComponent.errorShowFormEdit"), error.getMessage());
	}

	private Collection<RegistrationForm> getRegistrationForms()
	{
		try
		{
			return controller.getRegistrationForms();
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
			controller.removeRegistrationForm(form, dropRequests);
			registrationFormsList.removeElement(form);
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
}
