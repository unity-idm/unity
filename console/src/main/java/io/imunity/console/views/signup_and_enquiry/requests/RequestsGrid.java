/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.requests;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.SortDirection;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.elements.grid.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.elements.grid.GridSearchFieldFactory;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import io.imunity.vaadin.endpoint.common.WebSession;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component showing a grid with the registration requests. It is possible to
 * register selection change listener and there is one action implemented to
 * refresh the list.
 * 
 * @author P.Piernik
 *
 */
class RequestsGrid extends VerticalLayout
{
	private final MessageSource msg;
	private final RequestsService controller;
	private final NotificationPresenter notificationPresenter;
	private GridWithActionColumn<RequestEntry> requestsGrid;

	RequestsGrid(MessageSource msg, RequestsService controller, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
		initUI();
	}

	private void initUI()
	{
		requestsGrid = new GridWithActionColumn<>(msg::getMessage, Collections.emptyList());

		requestsGrid.addColumn(r -> r.getTypeAsString())
				.setHeader(msg.getMessage("RegistrationRequest.type"))
				.setResizable(true)
				.setSortable(true);
		requestsGrid.addColumn(r -> r.request.getRequest()
				.getFormId())
				.setHeader(msg.getMessage("RegistrationRequest.form"))
				.setResizable(true)
				.setSortable(true);
		requestsGrid.addColumn(r -> r.request.getStatus()
				.toString())
				.setHeader(msg.getMessage("RegistrationRequest.status"))
				.setResizable(true)
				.setSortable(true);
		Column<RequestEntry> submitTimeColumn = requestsGrid.addColumn(r -> r.getSubmitTime())
				.setHeader(msg.getMessage("RegistrationRequest.submitTime"))
				.setResizable(true)
				.setSortable(true);
		requestsGrid.addColumn(r -> r.getIdentity())
				.setHeader(msg.getMessage("RegistrationRequest.identity"))
				.setResizable(true)
				.setSortable(true);
		requestsGrid.addHamburgerActions(getHamburgerHandlers());

		requestsGrid.setMultiSelect(true);
		requestsGrid.setSizeFull();
		requestsGrid.sort(List.of(new GridSortOrder<>(submitTimeColumn, SortDirection.DESCENDING)));
		requestsGrid.addItemClickListener(e ->
		{
			requestsGrid.deselectAll();
			requestsGrid.select(e.getItem());
		});
		ActionMenuWithHandlerSupport<RequestEntry> hamburgerMenu = new ActionMenuWithHandlerSupport<>();
		hamburgerMenu.addActionHandlers(getHamburgerHandlers());
		requestsGrid.addSelectionListener(hamburgerMenu.getSelectionListener());
		SearchField search = GridSearchFieldFactory.generateSearchField(requestsGrid, msg::getMessage);
		Toolbar<RequestEntry> toolbar = new Toolbar<>();
		toolbar.addHamburger(hamburgerMenu);
		toolbar.addSearch(search);
		ComponentWithToolbar invGridWithToolbar = new ComponentWithToolbar(requestsGrid, toolbar);
		invGridWithToolbar.setSpacing(false);
		invGridWithToolbar.setSizeFull();
		add(invGridWithToolbar);
		setSizeFull();
		setPadding(true);
		refresh();
	}

	private List<SingleActionHandler<RequestEntry>> getHamburgerHandlers()
	{
		return Arrays.asList(getAcceptAction(), getRejectAction(), getDropAction());
	}

	void refresh()
	{
		Collection<RequestEntry> requests = getRequests();
		RequestEntry selected = getOnlyOneSelected();
		requestsGrid.setItems(requests);
		if (selected != null)
		{
			String requestId = selected.request.getRequestId();
			requests.stream()
					.filter(request -> requestId.equals(request.request.getRequestId()))
					.findFirst()
					.ifPresent(request -> requestsGrid.select(request));
		}
	}

	private Collection<RequestEntry> getRequests()
	{
		try
		{
			return controller.getRequests();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
		}
		return Collections.emptyList();
	}

	public void addValueChangeListener(final RequestSelectionListener listener)
	{
		requestsGrid.addSelectionListener(event ->
		{
			RequestEntry selected = getOnlyOneSelected();
			if (selected == null)
				listener.deselected();
			else if (selected.request instanceof RegistrationRequestState)
				listener.registrationChanged((RegistrationRequestState) selected.request);
			else
				listener.enquiryChanged((EnquiryResponseState) selected.request);
		});
	}

	private RequestEntry getOnlyOneSelected()
	{
		Collection<RequestEntry> beans = requestsGrid.getSelectedItems();
		return beans == null || beans.isEmpty() || beans.size() > 1 ? null
				: ((RequestEntry) beans.iterator()
						.next());
	}

	public void process(Collection<?> items, RegistrationRequestAction action)
	{
		try
		{
			controller.process(items, action, WebSession.getCurrent()
					.getEventBus());
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
		}
	}

	private SingleActionHandler<RequestEntry> getRejectAction()
	{
		return createAction(RegistrationRequestAction.reject, VaadinIcon.BAN);
	}

	private SingleActionHandler<RequestEntry> getAcceptAction()
	{
		return createAction(RegistrationRequestAction.accept, VaadinIcon.CHECK_CIRCLE_O);
	}

	private SingleActionHandler<RequestEntry> createAction(RegistrationRequestAction action, VaadinIcon icon)
	{
		return SingleActionHandler.builder(RequestEntry.class)
				.withCaption(msg.getMessage("RequestProcessingPanel." + action.toString()))
				.withIcon(icon)
				.multiTarget()
				.withHandler(items -> handleRegistrationAction(items, action))
				.withDisabledPredicate(disableWhenNotPending())
				.build();
	}

	private Predicate<RequestEntry> disableWhenNotPending()
	{
		return bean -> bean.request.getStatus() != RegistrationRequestStatus.pending;
	}

	private SingleActionHandler<RequestEntry> getDropAction()
	{
		return SingleActionHandler.builder(RequestEntry.class)
				.withCaption(msg.getMessage("RequestProcessingPanel.drop"))
				.withIcon(VaadinIcon.TRASH)
				.multiTarget()
				.withHandler(items -> handleRegistrationAction(items, RegistrationRequestAction.drop))
				.build();
	}

	public void handleRegistrationAction(Set<RequestEntry> items, RegistrationRequestAction action)
	{
		new ConfirmDialog(msg.getMessage("RequestsGrid.confirmAction." + action, items.size()), "",
				msg.getMessage("ok"), e -> process(items, action), msg.getMessage("cancel"), e ->
				{
				}).open();
	}
}
