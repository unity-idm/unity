/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SearchField;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component showing a grid with the invitations.
 * 
 * @author P.Piernik
 *
 */
class InvitationsGrid extends CustomComponent
{
	private MessageSource msg;
	private InvitationsController controller;
	private GridWithActionColumn<InvitationEntry> invitationsGrid;

	InvitationsGrid(MessageSource msg, InvitationsController controller)
	{
		this.msg = msg;
		this.controller = controller;
		initUI();
	}

	private void initUI()
	{
		invitationsGrid = new GridWithActionColumn<>(msg, Collections.emptyList(), false, false);

		invitationsGrid.addSortableColumn(InvitationEntry::getType, msg.getMessage("InvitationsGrid.type"), 10);

		invitationsGrid.addSortableColumn(InvitationEntry::getForm, msg.getMessage("InvitationsGrid.form"), 10);
		invitationsGrid.addSortableColumn(InvitationEntry::getAddress,
				msg.getMessage("InvitationsGrid.contactAddress"), 10).setId("contactAddress");
		invitationsGrid.addSortableColumn(InvitationEntry::getCode, msg.getMessage("InvitationsGrid.code"), 10);
		invitationsGrid.addSortableColumn(InvitationEntry::getExpiration,
				msg.getMessage("InvitationsGrid.expiration"), 10);
		invitationsGrid.addHamburgerActions(getHamburgerActionsHandlers());
		invitationsGrid.sort("contactAddress", SortDirection.ASCENDING);

		invitationsGrid.setMultiSelect(true);
		invitationsGrid.setSizeFull();

		HamburgerMenu<InvitationEntry> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addActionHandlers(getHamburgerActionsHandlers());
		invitationsGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		SearchField search = FilterableGridHelper.generateSearchField(invitationsGrid, msg);

		Toolbar<InvitationEntry> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addHamburger(hamburgerMenu);
		toolbar.addSearch(search, Alignment.MIDDLE_RIGHT);	
		ComponentWithToolbar InvGridWithToolbar = new ComponentWithToolbar(invitationsGrid, toolbar, Alignment.BOTTOM_LEFT);
		InvGridWithToolbar.setSizeFull();
		InvGridWithToolbar.setSpacing(false);
		
		setCompositionRoot(InvGridWithToolbar);
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
			invitations.stream().filter(i -> code.equals(i.getCode())).findFirst()
					.ifPresent(i -> invitationsGrid.select(i));
		}
	}

	private Collection<InvitationEntry> getInvitations()
	{
		try
		{
			return controller.getInvitations();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private List<SingleActionHandler<InvitationEntry>> getHamburgerActionsHandlers()
	{

		SingleActionHandler<InvitationEntry> send = SingleActionHandler.builder(InvitationEntry.class)
				.withCaption(msg.getMessage("InvitationsGrid.sendCode"))
				.withDisabledPredicate(i -> i.invitation.getContactAddress() == null)
				.withIcon(Images.messageSend.getResource()).multiTarget()
				.withHandler(this::sendInvitations).build();

		SingleActionHandler<InvitationEntry> remove = SingleActionHandler
				.builder4Delete(msg, InvitationEntry.class).withHandler(this::tryRemove).build();

		return Arrays.asList(send, remove);
	}

	public void addValueChangeListener(final InvitationSelectionListener listener)
	{
		invitationsGrid.addSelectionListener(event -> {
			InvitationEntry selected = getOnlyOneSelected();
			listener.invitationChanged(selected == null ? null : selected.invitationWithCode);
		});
	}

	private InvitationEntry getOnlyOneSelected()
	{
		Collection<InvitationEntry> beans = invitationsGrid.getSelectedItems();
		return beans == null || beans.isEmpty() || beans.size() > 1 ? null
				: ((InvitationEntry) beans.iterator().next());
	}

	private void sendInvitations(Set<InvitationEntry> items)
	{
		try
		{
			controller.sendInvitations(items);
			refresh();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void remove(Set<InvitationEntry> items)
	{
		try
		{
			controller.removeInvitations(items);
			items.forEach(m -> invitationsGrid.removeElement(m));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void tryRemove(Set<InvitationEntry> items)
	{
		new ConfirmDialog(msg, msg.getMessage("InvitationsGrid.confirmDelete", items.size()),
				() -> remove(items)).show();
	}
}
