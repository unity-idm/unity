/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.pki;

import static io.imunity.console.views.ViewHeaderActionLayoutFactory.createHeaderActionLayout;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.settings.publicKeyInfrastructure", parent = "WebConsoleMenu.settings")
@Route(value = "/pki", layout = ConsoleMenu.class)
public class PKIView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final CertificatesController certController;
	private GridWithActionColumn<CertificateEntry> certGrid;

	PKIView(MessageSource msg, CertificatesController controller)
	{
		this.msg = msg;
		this.certController = controller;
		initUI();
	}

	private void initUI()
	{

		certGrid = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
		certGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		Grid.Column<CertificateEntry> nameColumn = certGrid.addComponentColumn(c -> new RouterLink(c.getName(), PKIEditView.class, c.getName()))
				.setHeader(msg.getMessage("CertificatesComponent.certificateNameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(CertificateEntry::getName));
		certGrid.setItems(certController.getCertificates());
		certGrid.sort(GridSortOrder.desc(nameColumn).build());

		H3 certCaption = new H3(msg.getMessage("CertificatesComponent.caption"));
		VerticalLayout main = new VerticalLayout(certCaption, createHeaderActionLayout(msg, PKIEditView.class), certGrid);
		main.setSpacing(false);
		getContent().add(main);
	}

	private List<SingleActionHandler<CertificateEntry>> getActionsHandlers()
	{
		SingleActionHandler<CertificateEntry> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, CertificateEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<CertificateEntry> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, CertificateEntry.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();
		return Arrays.asList(edit, remove);
	}

	private void gotoEdit(CertificateEntry cred)
	{
		UI.getCurrent()
				.navigate(PKIEditView.class, cred.getName());
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
