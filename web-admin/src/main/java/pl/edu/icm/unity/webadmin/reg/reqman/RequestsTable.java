/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import java.text.SimpleDateFormat;
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

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.IdentityParam;
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
	private RegistrationsManagement registrationsManagement;
	private EnquiryManagement enquiryManagement;
	private UnityMessageSource msg;
	private EventsBus bus;
	
	private Grid<TableRequestBean> requestsTable;

	public RequestsTable(RegistrationsManagement regMan, EnquiryManagement enquiryManagement, UnityMessageSource msg)
	{
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
		
		requestsTable.addColumn(TableRequestBean::getType, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.type"))
			.setId("type");
		requestsTable.addColumn(TableRequestBean::getForm, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.form"))
			.setId("form");
		requestsTable.addColumn(TableRequestBean::getRequestId, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.requestId"))
			.setId("requestId");
		requestsTable.addColumn(TableRequestBean::getSubmitTime, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.submitTime"))
			.setId("submitTime");
		requestsTable.addColumn(TableRequestBean::getStatus, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.status"))
			.setId("status");
		requestsTable.addColumn(TableRequestBean::getRequestedIdentity, ValueProvider.identity())
			.setCaption(msg.getMessage("RegistrationRequest.requestedIdentity"))
			.setId("requestedIdentity");
		
		requestsTable.sort("type", SortDirection.ASCENDING);
		
		GridContextMenuSupport<TableRequestBean> contextMenu = new GridContextMenuSupport<>(requestsTable);
		contextMenu.addActionHandler(getRefreshAction());
		contextMenu.addActionHandler(getAcceptAction());
		contextMenu.addActionHandler(getRejectAction());
		contextMenu.addActionHandler(getDropAction());
		GridSelectionSupport.installClickListener(requestsTable);
		
		Toolbar<TableRequestBean> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
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
			TableRequestBean selected = getOnlyOneSelected();
			if (selected == null)
				listener.deselected();
			else if (selected.request instanceof RegistrationRequestState)
				listener.registrationChanged((RegistrationRequestState) selected.request);
			else
				listener.enquiryChanged((EnquiryResponseState) selected.request);
		});
	}
	
	private TableRequestBean getOnlyOneSelected()
	{
		Collection<TableRequestBean> beans = requestsTable.getSelectedItems();
		return beans == null || beans.isEmpty() || beans.size() > 1 ? 
				null : ((TableRequestBean)beans.iterator().next());
	}
	
	private SingleActionHandler<TableRequestBean> getRefreshAction()
	{
		return SingleActionHandler
			.builder4Refresh(msg, TableRequestBean.class)
			.withHandler(selection -> refresh())
			.build();
	}
	
	public void refresh()
	{
		try
		{
			Stream<TableRequestBean> regRequestsStream = registrationsManagement.getRegistrationRequests().stream()
				.map(registration -> new TableRequestBean(registration, msg));
			Stream<TableRequestBean> enqRequestsStream = enquiryManagement.getEnquiryResponses().stream()
				.map(enquiry -> new TableRequestBean(enquiry, msg));
			List<TableRequestBean> requests = Stream.concat(regRequestsStream, enqRequestsStream)
					.collect(Collectors.toList());
			requestsTable.setItems(requests);
			TableRequestBean selected = getOnlyOneSelected();
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
				processSingle((TableRequestBean) item, action);
			} catch (EngineException e)
			{
				String info = msg.getMessage("RequestsTable.processError." + action.toString(),
						((TableRequestBean)item).request.getRequestId());
				NotificationPopup.showError(msg, info, e);
				break;
			}
		}
	}
	
	private void processSingle(TableRequestBean item, RegistrationRequestAction action) throws EngineException
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
	
	private SingleActionHandler<TableRequestBean> getRejectAction()
	{
		return createAction(RegistrationRequestAction.reject, Images.reject);
	}
	
	private SingleActionHandler<TableRequestBean> getAcceptAction()
	{
		return createAction(RegistrationRequestAction.accept, Images.ok);
	}
	
	private SingleActionHandler<TableRequestBean> createAction(RegistrationRequestAction action, Images img)
	{
		return SingleActionHandler.builder(TableRequestBean.class)
				.withCaption(msg.getMessage("RequestProcessingPanel." + action.toString()))
				.withIcon(img.getResource())
				.multiTarget()
				.withHandler(items -> handleRegistrationAction(items, action))
				.withDisabledPredicate(disableWhenNotPending())
				.build();
	}
	
	private Predicate<TableRequestBean> disableWhenNotPending()
	{
		return bean -> bean.request.getStatus() != RegistrationRequestStatus.pending;
	}

	private SingleActionHandler<TableRequestBean> getDropAction()
	{
		return SingleActionHandler.builder(TableRequestBean.class)
				.withCaption(msg.getMessage("RequestProcessingPanel.drop"))
				.withIcon(Images.trashBin.getResource())
				.multiTarget()
				.withHandler(items -> handleRegistrationAction(items, RegistrationRequestAction.drop))
				.build();
	}
	
	public void handleRegistrationAction(Set<TableRequestBean> items, RegistrationRequestAction action)
	{	
		new ConfirmDialog(msg, msg.getMessage(
				"RequestsTable.confirmAction."+action, items.size()),
				() -> process(items, action)
		).show();
	}

	public static class TableRequestBean
	{
		private UserRequestState<?> request;
		private UnityMessageSource msg;
		
		public TableRequestBean(UserRequestState<?> request, UnityMessageSource msg)
		{
			this.request = request;
			this.msg = msg;
		}

		public String getType()
		{
			boolean enquiry = request instanceof EnquiryResponseState;
			return msg.getMessage("RequestsTable.type." + (enquiry ? "enquiry" : "registration"));
		}
		
		public String getForm()
		{
			return request.getRequest().getFormId();
		}
		
		public String getRequestId()
		{
			return request.getRequestId();
		}
		
		public String getSubmitTime()
		{
			return new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).format(request.getTimestamp());
		}

		public String getStatus()
		{
			return msg.getMessage("RegistrationRequestStatus." + request.getStatus());
		}
		
		public String getRequestedIdentity()
		{
			List<IdentityParam> identities = request.getRequest().getIdentities();
			if (identities.isEmpty())
				return "-";
			IdentityParam id = identities.get(0);
			return id == null ? "-" : id.toString();
		}
	}
	
	public interface RequestSelectionListener
	{
		void registrationChanged(RegistrationRequestState request);
		void enquiryChanged(EnquiryResponseState request);
		void deselected();
	}
}
