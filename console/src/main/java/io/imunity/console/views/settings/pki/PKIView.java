/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.pki;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.ActionIconBuilder;
import io.imunity.vaadin.elements.Breadcrumb;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import java.util.Comparator;

import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.icon.VaadinIcon.TRASH;
import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.settings.publicKeyInfrastructure", parent = "WebConsoleMenu.settings")
@Route(value = "/pki", layout = ConsoleMenu.class)
public class PKIView extends ConsoleViewComponent
{

	private final MessageSource msg;
	private final CertificatesController certController;
	private Grid<CertificateEntry> certGrid;

	PKIView(MessageSource msg, CertificatesController controller)
	{
		this.msg = msg;
		this.certController = controller;
		initUI();
	}

	private void initUI()
	{

		certGrid = new Grid<>();
		certGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		Grid.Column<CertificateEntry> nameColumn = certGrid.addComponentColumn(c -> new RouterLink(c.getName(), PKIEditView.class, c.getName()))
				.setHeader(msg.getMessage("CertificatesComponent.certificateNameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(CertificateEntry::getName));
		certGrid.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);

		certGrid.setItems(certController.getCertificates());
		certGrid.sort(GridSortOrder.desc(nameColumn).build());

		H3 certCaption = new H3(msg.getMessage("CertificatesComponent.caption"));
		VerticalLayout main = new VerticalLayout(certCaption, createHeaderActionLayout(msg, PKIEditView.class), certGrid);
		main.setSpacing(false);
		getContent().add(main);
	}

	private Component createRowActionMenu(CertificateEntry entry)
	{
		Icon generalSettings = new ActionIconBuilder()
				.icon(EDIT)
				.tooltipText(msg.getMessage("edit"))
				.navigation(PKIEditView.class, entry.getName())
				.build();

		Icon remove = new ActionIconBuilder()
				.icon(TRASH)
				.tooltipText(msg.getMessage("remove"))
				.clickListener(() -> tryRemove(entry))
				.build();

		HorizontalLayout horizontalLayout = new HorizontalLayout(generalSettings, remove);
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	private void tryRemove(CertificateEntry cert)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(cert.getName()));
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("CertificatesComponent.confirmDeleteCertificate", confirmText),
				msg.getMessage("ok"),
				e -> remove(cert),
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}

	private void remove(CertificateEntry cert)
	{
		certController.removeCertificate(cert);
		certGrid.setItems(certController.getCertificates());
	}

}
