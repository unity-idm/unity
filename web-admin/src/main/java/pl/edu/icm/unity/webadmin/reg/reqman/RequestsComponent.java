/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.webadmin.reg.reqman.RequestsTable.RequestSelectionListener;
import pl.edu.icm.unity.webui.ActivationListener;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestChangedEvent;

/**
 * Component responsible for management of the submitted registration requests.
 * 
 * Shows their table, and the selected request in the right side, where it can be edited, 
 * accepted, rejected or dropped.
 *  
 * @author K. Benedyczak
 */
@PrototypeComponent
public class RequestsComponent extends CustomComponent implements ActivationListener
{
	private RegistrationsManagement registrationsManagement;
	private UnityMessageSource msg;
	private EnquiryManagement enquiryManagement;
	private RequestProcessingPanel requestPanel;
	
	private RequestsTable requestsTable;
	
	@Autowired
	public RequestsComponent(RegistrationsManagement registrationsManagement, 
			EnquiryManagement enquiryManagement,
			UnityMessageSource msg, 
			RequestProcessingPanel requestPanel)
	{
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
		this.msg = msg;
		this.requestPanel = requestPanel;
		initUI();
		EventsBus eventBus = WebSession.getCurrent().getEventBus();
		eventBus.addListener(event -> requestsTable.refresh(), RegistrationRequestChangedEvent.class);
		eventBus.addListener(event -> requestsTable.refresh(), EnquiryResponseChangedEvent.class);
	}


	private void initUI()
	{
		addStyleName(Styles.visibleScroll.toString());
		requestsTable = new RequestsTable(registrationsManagement, enquiryManagement, msg);
		requestsTable.addValueChangeListener(new RequestSelectionListener()
		{
			@Override
			public void registrationChanged(RegistrationRequestState request)
			{
				requestPanel.setVisible(true);
				requestPanel.setRequest(request);
			}

			@Override
			public void enquiryChanged(EnquiryResponseState request)
			{
				requestPanel.setVisible(true);
				requestPanel.setRequest(request);
			}

			@Override
			public void deselected()
			{
				requestPanel.setVisible(false);
			}
		});
		
		CompositeSplitPanel hl = new CompositeSplitPanel(false, true, requestsTable, requestPanel, 40);
		setCompositionRoot(hl);
		setCaption(msg.getMessage("RequestsComponent.caption"));
	}

	@Override
	public void stateChanged(boolean enabled)
	{
		if (enabled)
			requestsTable.refresh();
	}
}
