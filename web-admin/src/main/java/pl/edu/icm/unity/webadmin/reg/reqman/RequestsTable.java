/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import java.util.List;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Toolbar;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
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
	private UnityMessageSource msg;
	
	private Table requestsTable;

	public RequestsTable(RegistrationsManagement regMan, UnityMessageSource msg)
	{
		this.registrationsManagement = regMan;
		this.msg = msg;
		
		initUI();
	}

	private void initUI()
	{
		requestsTable = new Table();
		requestsTable.setNullSelectionAllowed(false);
		requestsTable.setImmediate(true);
		requestsTable.setSizeFull();
		BeanItemContainer<TableRequestBean> tableContainer = new BeanItemContainer<>(TableRequestBean.class);
		tableContainer.removeContainerProperty("element");
		requestsTable.setSelectable(true);
		requestsTable.setMultiSelect(false);
		requestsTable.setContainerDataSource(tableContainer);
		requestsTable.setVisibleColumns(new String[] {"form", "requestId", "submitTime", "status", 
				"requestedIdentity"});
		requestsTable.setColumnHeaders(new String[] {
				msg.getMessage("RegistrationRequest.form"),
				msg.getMessage("RegistrationRequest.requestId"),
				msg.getMessage("RegistrationRequest.submitTime"),
				msg.getMessage("RegistrationRequest.status"),
				msg.getMessage("RegistrationRequest.requestedIdentity")});
		requestsTable.setSortContainerPropertyId(requestsTable.getContainerPropertyIds().iterator().next());
		requestsTable.setSortAscending(true);
		RefreshActionHandler handler = new RefreshActionHandler();
		requestsTable.addActionHandler(handler);
		
		Toolbar toolbar = new Toolbar(requestsTable, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(handler);
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(requestsTable, toolbar);
		tableWithToolbar.setSizeFull();
		
		setCompositionRoot(tableWithToolbar);
		refresh();
	}

	public void addValueChangeListener(final RequestSelectionListener listener)
	{
		requestsTable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				TableRequestBean bean = (TableRequestBean) requestsTable.getValue();
				listener.requestChanged(bean == null ? null : bean.request);
			}
		});
	}
	
	public void refresh()
	{
		try
		{
			TableRequestBean selected = (TableRequestBean) requestsTable.getValue();
			List<RegistrationRequestState> requests = registrationsManagement.getRegistrationRequests();
			requestsTable.removeAllItems();
			for (RegistrationRequestState req: requests)
			{
				TableRequestBean item = new TableRequestBean(req, msg);
				requestsTable.addItem(item);
				if (selected != null && selected.request.getRequestId().equals(req.getRequestId()))
					requestsTable.setValue(item);
			}
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("RequestsTable.errorGetRequests"), e);
			setCompositionRoot(error);
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
	
	public static class TableRequestBean
	{
		private RegistrationRequestState request;
		private UnityMessageSource msg;
		
		public TableRequestBean(RegistrationRequestState request, UnityMessageSource msg)
		{
			this.request = request;
			this.msg = msg;
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
			return Constants.SIMPLE_DATE_FORMAT.format(request.getTimestamp());
		}

		public String getStatus()
		{
			return msg.getMessage("RegistrationRequestStatus." + request.getStatus());
		}
		
		public String getRequestedIdentity()
		{
			IdentityParam id = request.getRequest().getIdentities().get(0);
			return id.toString();
		}
	}
	
	public interface RequestSelectionListener
	{
		public void requestChanged(RegistrationRequestState request);
	}
}
