/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
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
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallTable;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestChangedEvent;

import com.google.common.collect.Lists;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.Action;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Table;

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
	
	private Table requestsTable;

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
		requestsTable = new SmallTable();
		requestsTable.setNullSelectionAllowed(false);
		requestsTable.setImmediate(true);
		requestsTable.setSizeFull();
		BeanItemContainer<TableRequestBean> tableContainer = new BeanItemContainer<>(TableRequestBean.class);
		tableContainer.removeContainerProperty("element");
		requestsTable.setSelectable(true);
		requestsTable.setMultiSelect(true);
		requestsTable.setContainerDataSource(tableContainer);
		requestsTable.setVisibleColumns(new Object[] {"type", "form", "requestId", "submitTime", "status", 
				"requestedIdentity"});
		requestsTable.setColumnHeaders(new String[] {
				msg.getMessage("RegistrationRequest.type"),
				msg.getMessage("RegistrationRequest.form"),
				msg.getMessage("RegistrationRequest.requestId"),
				msg.getMessage("RegistrationRequest.submitTime"),
				msg.getMessage("RegistrationRequest.status"),
				msg.getMessage("RegistrationRequest.requestedIdentity")});
		requestsTable.setSortContainerPropertyId(requestsTable.getContainerPropertyIds().iterator().next());
		requestsTable.setSortAscending(true);

		RefreshActionHandler refreshA = new RefreshActionHandler();
		BulkProcessActionHandler acceptA = new BulkProcessActionHandler(
				RegistrationRequestAction.accept, Images.ok);
		BulkProcessActionHandler deleteA = new BulkProcessActionHandler(
				RegistrationRequestAction.drop, Images.trashBin);
		BulkProcessActionHandler rejectA = new BulkProcessActionHandler(
				RegistrationRequestAction.reject, Images.delete);
		Toolbar toolbar = new Toolbar(requestsTable, Orientation.HORIZONTAL);
		addAction(refreshA, toolbar);
		addAction(acceptA, toolbar);
		addAction(rejectA, toolbar);
		addAction(deleteA, toolbar);

		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(requestsTable, toolbar);
		tableWithToolbar.setSizeFull();
		
		setCompositionRoot(tableWithToolbar);
		refresh();
	}

	private void addAction(SingleActionHandler action, Toolbar toolbar)
	{
		requestsTable.addActionHandler(action);
		toolbar.addActionHandler(action);
	}
	
	public void addValueChangeListener(final RequestSelectionListener listener)
	{
		requestsTable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				TableRequestBean selected = getOnlyOneSelected();
				if (selected == null)
					listener.deselected();
				else if (selected.request instanceof RegistrationRequestState)
					listener.registrationChanged((RegistrationRequestState) selected.request);
				else
					listener.enquiryChanged((EnquiryResponseState) selected.request);
			}
		});
	}
	
	private TableRequestBean getOnlyOneSelected()
	{
		Collection<?> beans = (Collection<?>) requestsTable.getValue();
		return beans == null || beans.isEmpty() || beans.size() > 1 ? 
				null : ((TableRequestBean)beans.iterator().next());
	}
	
	public void refresh()
	{
		try
		{
			TableRequestBean selected = getOnlyOneSelected();
			requestsTable.removeAllItems();

			fill(selected, registrationsManagement.getRegistrationRequests());
			fill(selected, enquiryManagement.getEnquiryResponses());
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("RequestsTable.errorGetRequests"), e);
			setCompositionRoot(error);
		}
	}

	private void fill(TableRequestBean selected, List<? extends UserRequestState<?>> requests)
	{
		for (UserRequestState<?> req: requests)
		{
			TableRequestBean item = new TableRequestBean(req, msg);
			requestsTable.addItem(item);
			if (selected != null && selected.request.getRequestId().equals(req.getRequestId()))
				requestsTable.setValue(Lists.newArrayList(item));
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
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("RequestsTable.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}
	
	private class BulkProcessActionHandler extends SingleActionHandler
	{
		private final RegistrationRequestAction action;
		
		
		public BulkProcessActionHandler(RegistrationRequestAction action, Images image)
		{
			super(msg.getMessage("RequestProcessingPanel." + action.toString()), 
					image.getResource());
			this.action = action;
			setMultiTarget(true);
		}
		
		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (action != RegistrationRequestAction.drop && target instanceof Collection<?>)
			{
				Collection<?> t = (Collection<?>) target;
				boolean hasPending = t.stream().allMatch(o -> {
					TableRequestBean bean = (TableRequestBean) o;
					return bean.request.getStatus() == RegistrationRequestStatus.pending;
				});
				return hasPending ? super.getActions(target, sender) : EMPTY;
			} 
			return super.getActions(target, sender);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{	
			final Collection<?> items = (Collection<?>) requestsTable.getValue();
			new ConfirmDialog(msg, msg.getMessage(
					"RequestsTable.confirmAction."+action, items.size()),
					new ConfirmDialog.Callback()
					{
						@Override
						public void onConfirm()
						{
							process(items, action);
						}
					}).show();
		}
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
