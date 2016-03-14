/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.webadmin.reg.reqman.RequestsTable.RequestSelectionListener;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.registration.EnquiryResponseChangedEvent;
import pl.edu.icm.unity.webui.registration.RegistrationRequestChangedEvent;

import com.vaadin.ui.CustomComponent;

/**
 * Component responsible for management of the submitted registration requests.
 * 
 * Shows their table, and the selected request in the right side, where it can be edited, 
 * accepted, rejected or dropped.
 *  
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RequestsComponent extends CustomComponent
{
	private RegistrationsManagement registrationsManagement;
	private AttributeHandlerRegistry handlersRegistry;
	private UnityMessageSource msg;
	
	private RequestsTable requestsTable;
	private IdentityTypesRegistry idTypesRegistry;
	private EnquiryManagement enquiryManagement;
	private IdentitiesManagement identitiesManagement;
	
	@Autowired
	public RequestsComponent(RegistrationsManagement registrationsManagement, EnquiryManagement enquiryManagement,
			AttributeHandlerRegistry handlersRegistry,
			IdentityTypesRegistry idTypesRegistry, UnityMessageSource msg, 
			IdentitiesManagement identitiesManagement)
	{
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
		this.idTypesRegistry = idTypesRegistry;
		this.msg = msg;
		this.handlersRegistry = handlersRegistry;
		this.identitiesManagement = identitiesManagement;
		initUI();
		EventsBus eventBus = WebSession.getCurrent().getEventBus();
		eventBus.addListener(event -> requestsTable.refresh(), RegistrationRequestChangedEvent.class);
		eventBus.addListener(event -> requestsTable.refresh(), EnquiryResponseChangedEvent.class);
	}


	private void initUI()
	{
		addStyleName(Styles.visibleScroll.toString());
		requestsTable = new RequestsTable(registrationsManagement, enquiryManagement, msg);
		final RequestProcessingPanel requestPanel = new RequestProcessingPanel(msg, registrationsManagement,
				enquiryManagement, handlersRegistry, idTypesRegistry, identitiesManagement);
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
}
