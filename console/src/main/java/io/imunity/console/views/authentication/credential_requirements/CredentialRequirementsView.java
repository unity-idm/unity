/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.credential_requirements;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.ActionIconBuilder;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import java.util.*;

import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.icon.VaadinIcon.TRASH;
import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.authentication.credentialRequirements", parent = "WebConsoleMenu.authentication")
@Route(value = "/credential-requirements", layout = ConsoleMenu.class)
public class CredentialRequirementsView extends ConsoleViewComponent
{
	private final CredentialRequirementsController controller;
	private final MessageSource msg;
	private final EventsBus bus;
	private final NotificationPresenter notificationPresenter;
	private Grid<CredentialRequirements> credList;

	CredentialRequirementsView(MessageSource msg, CredentialRequirementsController controller, NotificationPresenter notificationPresenter)
	{
		this.controller = controller;
		this.msg = msg;
		this.bus = WebSession.getCurrent().getEventBus();
		this.notificationPresenter = notificationPresenter;
		init();
	}

	public void init()
	{
		credList = new Grid<>();
		credList.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		Grid.Column<CredentialRequirements> nameColumn = credList.addComponentColumn(c ->
				{
					if(!c.isReadOnly())
						return new RouterLink(c.getName(), CredentialRequirementsEditView.class, c.getName());
					else
						return new Span(c.getName());
				})
				.setHeader(msg.getMessage("CredentialReqView.nameCaption"))
				.setAutoWidth(true)
				.setResizable(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(DescribedObjectROImpl::getName));
		credList.addColumn(c -> String.join(", ", c.getRequiredCredentials()))
				.setHeader(msg.getMessage("CredentialReqView.credentialsCaption"))
				.setAutoWidth(true)
				.setResizable(true)
				.setSortable(true);
		credList.addColumn(DescribedObjectROImpl::getDescription)
				.setHeader(msg.getMessage("CredentialReqView.descriptionCaption"))
				.setAutoWidth(true)
				.setResizable(true)
				.setSortable(true);
		credList.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);

		credList.sort(GridSortOrder.asc(nameColumn).build());
		credList.setItems(controller.getCredentialRequirements());

		getContent().add(new VerticalLayout(createHeaderActionLayout(msg, CredentialRequirementsEditView.class), credList));
	}

	private Component createRowActionMenu(CredentialRequirements entry)
	{
		ActionIconBuilder generalSettingsBuilder = new ActionIconBuilder().icon(EDIT);
		ActionIconBuilder removeBuilder = new ActionIconBuilder().icon(TRASH);

		if(entry.isReadOnly())
		{
			generalSettingsBuilder.disabled();
			removeBuilder.disabled();
		}
		else
		{
			generalSettingsBuilder
					.tooltipText(msg.getMessage("edit"))
					.navigation(CredentialRequirementsEditView.class, entry.getName())
					.build();

			removeBuilder
					.tooltipText(msg.getMessage("remove"))
					.clickListener(() -> tryRemove(entry))
					.build();
		}

		HorizontalLayout horizontalLayout = new HorizontalLayout(generalSettingsBuilder.build(), removeBuilder.build());
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	private void tryRemove(CredentialRequirements item)
	{
		Collection<CredentialRequirements> allCRs = controller.getCredentialRequirements();
		HashSet<String> removedCr = Sets.newHashSet(item.getName());
		List<String> crs = new ArrayList<>();
		for (CredentialRequirements cr: allCRs)
			if (!removedCr.contains(cr.getName()))
				crs.add(cr.getName());
		Collections.sort(crs);
		if (crs.isEmpty())
		{
			notificationPresenter.showError(msg.getMessage("CredentialRequirements.removalError"),
					msg.getMessage("CredentialRequirements.cantRemoveLast"));
			return;
		}

		String confirmText = MessageUtils.createConfirmFromStrings(msg, removedCr);
		ComboBox<String> replacementCR = new ComboBox<>(msg.getMessage("CredentialRequirements.replacement"), crs);
		replacementCR.setValue(crs.get(0));
		replacementCR.setWidthFull();
		VerticalLayout layout = new VerticalLayout();
		layout.add(new Span(msg.getMessage("CredentialRequirements.removalConfirm", confirmText)), replacementCR);

		ConfirmDialog confirmDialog = new ConfirmDialog();
		confirmDialog.setHeader(msg.getMessage("ConfirmDialog.confirm"));
		confirmDialog.setConfirmButton(msg.getMessage("ok"), e -> remove(item, replacementCR.getValue()));
		confirmDialog.setCancelButton(msg.getMessage("cancel"), e -> {});
		confirmDialog.add(layout);
		confirmDialog.open();
	}

	private void remove(CredentialRequirements toRemove, String replacementCR)
	{
		controller.removeCredentialRequirements(toRemove, replacementCR, bus);
		credList.setItems(controller.getCredentialRequirements());
	}

}
