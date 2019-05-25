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
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.reg.invitations.InvitationEntry;
import io.imunity.webadmin.reg.invitations.InvitationSelectionListener;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
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
	private UnityMessageSource msg;
	private InvitationsController controller;
	private GridWithActionColumn<InvitationEntry> invitationsGrid;

	InvitationsGrid(UnityMessageSource msg, InvitationsController controller)
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
		hamburgerMenu.addStyleName(SidebarStyles.sidebar.toString());
		hamburgerMenu.addActionHandlers(getHamburgerActionsHandlers());
		invitationsGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		TextField search = FilterableGridHelper.generateSearchField(invitationsGrid, msg);

		VerticalLayout gridWrapper = new VerticalLayout();
		gridWrapper.setMargin(false);
		gridWrapper.setSpacing(false);
		HorizontalLayout hamburgerAndSearchWrapper = new HorizontalLayout(hamburgerMenu, search);
		hamburgerAndSearchWrapper.setWidth(100, Unit.PERCENTAGE);
		hamburgerAndSearchWrapper.setComponentAlignment(hamburgerMenu, Alignment.BOTTOM_LEFT);
		hamburgerAndSearchWrapper.setComponentAlignment(search, Alignment.BOTTOM_RIGHT);
		hamburgerAndSearchWrapper.setMargin(false);
		hamburgerAndSearchWrapper.setSpacing(false);
		gridWrapper.addComponent(hamburgerAndSearchWrapper);
		gridWrapper.setExpandRatio(hamburgerAndSearchWrapper, 0);
		gridWrapper.addComponent(invitationsGrid);
		gridWrapper.setExpandRatio(invitationsGrid, 2);
		gridWrapper.setSizeFull();

		setCompositionRoot(gridWrapper);
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
