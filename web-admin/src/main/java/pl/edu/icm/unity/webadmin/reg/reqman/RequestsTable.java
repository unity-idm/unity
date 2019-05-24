/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.data.ValueProvider;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;

import io.imunity.webadmin.reg.requests.RequestEntry;
import io.imunity.webadmin.reg.requests.RequestSelectionListener;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GridContextMenuSupport;
import pl.edu.icm.unity.webui.common.GridSelectionSupport;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallGrid;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestChangedEvent;

/**
 * Component showing a table with the registration requests. It is possible to register selection change
 * listener and there is one action implemented to refresh the list.
 * @author K. Benedyczak
 */
public class RequestsTable extends CustomComponent
{
	private EntityManagement idMan;
	private RegistrationsManagement registrationsManagement;
	private EnquiryManagement enquiryManagement;
	private UnityMessageSource msg;
	private EventsBus bus;
	
	private Grid<RequestEntry> requestsTable;

	public RequestsTable(EntityManagement idMan, RegistrationsManagement regMan, EnquiryManagement enquiryManagement, UnityMessageSource msg)
	{
		this.idMan = idMan;
		this.registrationsManagement = regMan;
		this.enquiryManagement = enquiryManagement;
		this.msg = msg;
		this.bus = WebSession.getCurrent().getEventBus();
		initUI();
	}

	private void initUI()
	{
		requestsTable = new SmallGrid<>();
		requestsTable.setSelectionMode(SelectionMode.MULTI);
		requestsTable.setSizeFull();
		
		requestsTable.addColumn(RequestEntry::getTypeAsString, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.type"))
			.setId("type");
		requestsTable.addColumn(RequestEntry::getForm, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.form"))
			.setId("form");
		requestsTable.addColumn(RequestEntry::getRequestId, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.requestId"))
			.setId("requestId");
		requestsTable.addColumn(RequestEntry::getSubmitTime, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.submitTime"))
			.setId("submitTime");
		requestsTable.addColumn(RequestEntry::getStatus, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.status"))
			.setId("status");
		requestsTable.addColumn(RequestEntry::getIdentity, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.identity"))
			.setId("requestedIdentity");
		
		requestsTable.sort("type", SortDirection.ASCENDING);
		
		GridContextMenuSupport<RequestEntry> contextMenu = new GridContextMenuSupport<>(requestsTable);
		contextMenu.addActionHandler(getRefreshAction());
		contextMenu.addActionHandler(getAcceptAction());
		contextMenu.addActionHandler(getRejectAction());
		contextMenu.addActionHandler(getDropAction());
		GridSelectionSupport.installClickListener(requestsTable);
		
		Toolbar<RequestEntry> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		requestsTable.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(contextMenu.getActionHandlers());
		
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(requestsTable, toolbar);
		tableWithToolbar.setSizeFull();
		
		setCompositionRoot(tableWithToolbar);
		refresh();
	}

	public void addValueChangeListener(final RequestSelectionListener listener)
	{
		requestsTable.addSelectionListener(event -> 
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
		Collection<RequestEntry> beans = requestsTable.getSelectedItems();
		return beans == null || beans.isEmpty() || beans.size() > 1 ? 
				null : ((RequestEntry)beans.iterator().next());
	}
	
	private SingleActionHandler<RequestEntry> getRefreshAction()
	{
		return SingleActionHandler
			.builder4Refresh(msg, RequestEntry.class)
			.withHandler(selection -> refresh())
			.build();
	}
	
	public void refresh()
	{
		try
		{
			Stream<RequestEntry> regRequestsStream = registrationsManagement.getRegistrationRequests().stream()
				.map(registration -> new RequestEntry(registration, msg, idMan));
			Stream<RequestEntry> enqRequestsStream = enquiryManagement.getEnquiryResponses().stream()
				.map(enquiry -> new RequestEntry(enquiry, msg, idMan));
			List<RequestEntry> requests = Stream.concat(regRequestsStream, enqRequestsStream)
					.collect(Collectors.toList());
			requestsTable.setItems(requests);
			RequestEntry selected = getOnlyOneSelected();
			if (selected != null)
			{
				String requestId = selected.request.getRequestId();
				requests.stream()
					.filter(request -> requestId.equals(request.request.getRequestId()))
					.findFirst()
					.ifPresent(request -> requestsTable.select(request));
			}
			
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("RequestsTable.errorGetRequests"), e);
			setCompositionRoot(error);
		}
	}
	
	public void process(Collection<?> items, RegistrationRequestAction action)
	{
		for (Object item: items)
		{
			try
			{
				processSingle((RequestEntry) item, action);
			} catch (EngineException e)
			{
				String info = msg.getMessage("RequestsTable.processError." + action.toString(),
						((RequestEntry)item).request.getRequestId());
				NotificationPopup.showError(msg, info, e);
				break;
			}
		}
	}
	
	private void processSingle(RequestEntry item, RegistrationRequestAction action) throws EngineException
	{
		UserRequestState<?> request = item.request;
		if (request instanceof RegistrationRequestState)
		{
			registrationsManagement.processRegistrationRequest(request.getRequestId(), 
				(RegistrationRequest) request.getRequest(), 
				action, 
				null, 
				null);
			bus.fireEvent(new RegistrationRequestChangedEvent(request.getRequestId()));
		} else
		{
			enquiryManagement.processEnquiryResponse(request.getRequestId(), 
					(EnquiryResponse) request.getRequest(), 
					action, 
					null, 
					null);
			bus.fireEvent(new EnquiryResponseChangedEvent(request.getRequestId()));
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
				.withIcon(img.getResource())
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
				.withIcon(Images.trashBin.getResource())
				.multiTarget()
				.withHandler(items -> handleRegistrationAction(items, RegistrationRequestAction.drop))
				.build();
	}
	
	public void handleRegistrationAction(Set<RequestEntry> items, RegistrationRequestAction action)
	{	
		new ConfirmDialog(msg, msg.getMessage(
				"RequestsTable.confirmAction."+action, items.size()),
				() -> process(items, action)
		).show();
	}
}
