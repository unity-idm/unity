/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.elements.grid.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.elements.grid.GridSearchFieldFactory;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component showing a grid with the invitations.
 * 
 * @author P.Piernik
 *
 */
class InvitationsGrid extends VerticalLayout
{
	private final MessageSource msg;
	private final InvitationsService invitationService;
	private final NotificationPresenter notificationPresenter;
	private GridWithActionColumn<InvitationEntry> invitationsGrid;

	InvitationsGrid(MessageSource msg, InvitationsService service, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.invitationService = service;
		this.notificationPresenter = notificationPresenter;
		initUI();
	}

	private void initUI()
	{
		invitationsGrid = new GridWithActionColumn<>(msg::getMessage, Collections.emptyList());
		invitationsGrid.addColumn(InvitationEntry::getType)
				.setHeader(msg.getMessage("InvitationsGrid.type")).setResizable(true);
		invitationsGrid.addColumn(InvitationEntry::getForm)
				.setHeader(msg.getMessage("InvitationsGrid.form")).setResizable(true);
		invitationsGrid.addColumn(InvitationEntry::getAddress)
				.setHeader(msg.getMessage("InvitationsGrid.contactAddress")).setResizable(true);
		invitationsGrid.addColumn(InvitationEntry::getCode)
				.setHeader(msg.getMessage("InvitationsGrid.code")).setResizable(true);
		invitationsGrid.addColumn(InvitationEntry::getExpiration)
				.setHeader(msg.getMessage("InvitationsGrid.expiration")).setResizable(true);
		invitationsGrid.addHamburgerActions(getHamburgerActionsHandlers());
		invitationsGrid.setMultiSelect(true);
		invitationsGrid.setSizeFull();
		ActionMenuWithHandlerSupport<InvitationEntry> hamburgerMenu = new ActionMenuWithHandlerSupport<>();
		hamburgerMenu.addActionHandlers(getHamburgerActionsHandlers());
		invitationsGrid.addSelectionListener(hamburgerMenu.getSelectionListener());
		SearchField search = GridSearchFieldFactory.generateSearchField(invitationsGrid, msg::getMessage);
		Toolbar<InvitationEntry> toolbar = new Toolbar<>();
		toolbar.addHamburger(hamburgerMenu);
		toolbar.addSearch(search);
		ComponentWithToolbar invGridWithToolbar = new ComponentWithToolbar(invitationsGrid, toolbar);
		invGridWithToolbar.setSpacing(false);
		invGridWithToolbar.setSizeFull();
		add(invGridWithToolbar);
		setSizeFull();
		refresh();
	}

	void refresh()
	{
		Collection<InvitationEntry> invitations = getInvitations();
		invitationsGrid.setItems(invitations);
		InvitationEntry selected = getOnlyOneSelected();
		if (selected != null)
		{
			String code = selected.getCode();
			invitations.stream()
					.filter(i -> code.equals(i.getCode()))
					.findFirst()
					.ifPresent(i -> invitationsGrid.select(i));
		}
	}

	private Collection<InvitationEntry> getInvitations()
	{
		try
		{
			return invitationService.getInvitations();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
		}
		return Collections.emptyList();
	}

	private List<SingleActionHandler<InvitationEntry>> getHamburgerActionsHandlers()
	{
		SingleActionHandler<InvitationEntry> send = SingleActionHandler.builder(InvitationEntry.class)
				.withCaption(msg.getMessage("InvitationsGrid.sendCode"))
				.withDisabledPredicate(i -> i.invitation.getContactAddress() == null)
				.withIcon(VaadinIcon.ENVELOPE_O)
				.multiTarget()
				.withHandler(this::sendInvitations)
				.build();
		SingleActionHandler<InvitationEntry> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, InvitationEntry.class)
				.withHandler(this::tryRemove)
				.build();

		return Arrays.asList(send, remove);
	}

	public void addValueChangeListener(final InvitationSelectionListener listener)
	{
		invitationsGrid.addSelectionListener(event ->
		{
			InvitationEntry selected = getOnlyOneSelected();
			listener.invitationChanged(selected == null ? null : selected.invitationWithCode);
		});
	}

	private InvitationEntry getOnlyOneSelected()
	{
		Collection<InvitationEntry> beans = invitationsGrid.getSelectedItems();
		return beans == null || beans.isEmpty() || beans.size() > 1 ? null
				: ((InvitationEntry) beans.iterator()
						.next());
	}

	private void sendInvitations(Set<InvitationEntry> items)
	{
		try
		{
			invitationService.sendInvitations(items);
			refresh();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
		}
	}

	private void remove(Set<InvitationEntry> items)
	{
		try
		{
			invitationService.removeInvitations(items);
			items.forEach(m -> invitationsGrid.removeElement(m));
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
		}
	}

	private void tryRemove(Set<InvitationEntry> items)
	{
		new ConfirmDialog(msg.getMessage("InvitationsGrid.confirmDelete", items.size()), "", msg.getMessage("ok"),
				e -> remove(items), msg.getMessage("cancel"), e ->
				{
				}).open();
	}
}
