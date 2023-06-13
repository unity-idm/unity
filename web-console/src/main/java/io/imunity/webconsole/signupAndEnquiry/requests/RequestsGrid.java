/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.requests;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
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
 * Component showing a grid with the registration requests. It is possible to
 * register selection change listener and there is one action implemented to
 * refresh the list.
 * 
 * @author P.Piernik
 *
 */
class RequestsGrid extends CustomComponent
{
	private MessageSource msg;
	private RequestsController controller;
	private GridWithActionColumn<RequestEntry> requestsGrid;
	private EventsBus bus;

	RequestsGrid(MessageSource msg, RequestsController controller)
	{
		this.msg = msg;
		this.controller = controller;
		this.bus = WebSession.getCurrent().getEventBus();
		initUI();
	}

	private void initUI()
	{
		requestsGrid = new GridWithActionColumn<>(msg, Collections.emptyList(), false, false);

		requestsGrid.addSortableColumn(r -> r.getTypeAsString(), msg.getMessage("RegistrationRequest.type"),
				10);

		requestsGrid.addSortableColumn(r -> r.request.getRequest().getFormId(),
				msg.getMessage("RegistrationRequest.form"), 10);

		requestsGrid.addSortableColumn(r -> r.request.getStatus().toString(),
				msg.getMessage("RegistrationRequest.status"), 10);

		requestsGrid.addSortableColumn(r -> r.getSubmitTime(), msg.getMessage("RegistrationRequest.submitTime"),
				10);

		requestsGrid.addSortableColumn(r -> r.getIdentity(), msg.getMessage("RegistrationRequest.identity"),
				10);
		requestsGrid.addHamburgerActions(getHamburgerHandlers());

		requestsGrid.setMultiSelect(true);
		requestsGrid.setSizeFull();

		HamburgerMenu<RequestEntry> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addActionHandlers(getHamburgerHandlers());
		requestsGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		SearchField search = FilterableGridHelper.generateSearchField(requestsGrid, msg);
		
		Toolbar<RequestEntry> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addHamburger(hamburgerMenu);
		toolbar.addSearch(search, Alignment.MIDDLE_RIGHT);	
		ComponentWithToolbar reqGridWithToolbar = new ComponentWithToolbar(requestsGrid, toolbar, Alignment.BOTTOM_LEFT);
		reqGridWithToolbar.setSizeFull();
		reqGridWithToolbar.setSpacing(false);

		setCompositionRoot(reqGridWithToolbar);
		setSizeFull();
		refresh();
	}

	private List<SingleActionHandler<RequestEntry>> getHamburgerHandlers()
	{
		return Arrays.asList(getAcceptAction(), getRejectAction(), getDropAction());
	}

	void refresh()
	{
		Collection<RequestEntry> requests = getRequests();
		requestsGrid.setItems(requests);
		RequestEntry selected = getOnlyOneSelected();
		if (selected != null)
		{
			String requestId = selected.request.getRequestId();
			requests.stream().filter(request -> requestId.equals(request.request.getRequestId()))
					.findFirst().ifPresent(request -> requestsGrid.select(request));
		}
	}

	private Collection<RequestEntry> getRequests()
	{
		try
		{
			return controller.getRequests();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	public void addValueChangeListener(final RequestSelectionListener listener)
	{
		requestsGrid.addSelectionListener(event -> {
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
				: ((RequestEntry) beans.iterator().next());
	}

	public void process(Collection<?> items, RegistrationRequestAction action)
	{
		try
		{
			controller.process(items, action, bus);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private SingleActionHandler<RequestEntry> getRejectAction()
	{
		return createAction(RegistrationRequestAction.reject, Images.reject);
	}

	private SingleActionHandler<RequestEntry> getAcceptAction()
	{
		return createAction(RegistrationRequestAction.accept, Images.ok);
	}

	private SingleActionHandler<RequestEntry> createAction(RegistrationRequestAction action, Images img)
	{
		return SingleActionHandler.builder(RequestEntry.class)
				.withCaption(msg.getMessage("RequestProcessingPanel." + action.toString()))
				.withIcon(img.getResource()).multiTarget()
				.withHandler(items -> handleRegistrationAction(items, action))
				.withDisabledPredicate(disableWhenNotPending()).build();
	}

	private Predicate<RequestEntry> disableWhenNotPending()
	{
		return bean -> bean.request.getStatus() != RegistrationRequestStatus.pending;
	}

	private SingleActionHandler<RequestEntry> getDropAction()
	{
		return SingleActionHandler.builder(RequestEntry.class)
				.withCaption(msg.getMessage("RequestProcessingPanel.drop"))
				.withIcon(Images.trashBin.getResource()).multiTarget()
				.withHandler(items -> handleRegistrationAction(items, RegistrationRequestAction.drop))
				.build();
	}

	public void handleRegistrationAction(Set<RequestEntry> items, RegistrationRequestAction action)
	{
		new ConfirmDialog(msg, msg.getMessage("RequestsGrid.confirmAction." + action, items.size()),
				() -> process(items, action)).show();
	}
}
