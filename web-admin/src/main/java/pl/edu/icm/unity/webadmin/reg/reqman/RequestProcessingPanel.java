/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ErrorPopup;

/**
 * Responsible for displaying a submitted request ({@link RegistrationRequestState}), its editing and processing.
 * @author K. Benedyczak
 */
public class RequestProcessingPanel extends CustomComponent
{
	private UnityMessageSource msg;
	private RegistrationsManagement regMan;
	
	private EventsBus bus;
	private RequestCommentPanel commentPanel;
	private RegistrationRequestState requestState;
	private Button accept;
	private Button reject;
	private Button delete;
	private VerticalLayout main;
	private Label requestForm;
	private Label requestId;
	private Label requestStatus;
	private Label requestDate;
	
	public RequestProcessingPanel(UnityMessageSource msg, RegistrationsManagement regMan)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.bus = WebSession.getCurrent().getEventBus();
		initUI();
	}
	
	private void initUI()
	{
		requestForm = new Label();
		requestForm.setCaption(msg.getMessage("RegistrationRequest.form")+":");
		requestId = new Label();
		requestId.setCaption(msg.getMessage("RegistrationRequest.requestId")+":");
		requestStatus = new Label();
		requestStatus.setCaption(msg.getMessage("RegistrationRequest.status")+":");
		requestDate = new Label();
		requestDate.setCaption(msg.getMessage("RegistrationRequest.submitTime")+":");
		
		FormLayout topInfo = new FormLayout(requestForm, requestStatus, requestDate, requestId);
		
		
		commentPanel = new RequestCommentPanel(msg, regMan);
		
		
		accept = new Button(msg.getMessage("RequestProcessingPanel.accept"));
		accept.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				process(RegistrationRequestAction.accept);
			}
		});
		reject = new Button(msg.getMessage("RequestProcessingPanel.reject"));
		reject.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				process(RegistrationRequestAction.reject);
			}
		});
		delete = new Button(msg.getMessage("RequestProcessingPanel.delete"));
		delete.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				process(RegistrationRequestAction.drop);
			}
		});
		HorizontalLayout buttonsBar = new HorizontalLayout(accept, reject, delete);
		buttonsBar.setSpacing(true);
		
		
		main = new VerticalLayout(topInfo, commentPanel, buttonsBar);
		main.setComponentAlignment(buttonsBar, Alignment.BOTTOM_RIGHT);
		main.setMargin(true);
		main.setSpacing(true);
		setCompositionRoot(main);
		setRequest(null);
	}
	
	public void setRequest(RegistrationRequestState input)
	{
		if (input == null)
		{
			main.setVisible(false);
			return;
		}
		main.setVisible(true);

		//TODO
		this.requestState = input;
		RegistrationRequest request = input.getRequest();
		requestForm.setValue(request.getFormId());
		requestId.setValue(input.getRequestId());
		requestStatus.setValue(msg.getMessage("RegistrationRequestStatus." + input.getStatus()));
		requestDate.setValue(Constants.SIMPLE_DATE_FORMAT.format(input.getTimestamp()));
		
		commentPanel.setInput(input);
		
		accept.setVisible(input.getStatus() == RegistrationRequestStatus.pending);
		reject.setVisible(input.getStatus() == RegistrationRequestStatus.pending);
	}
	
	private void process(RegistrationRequestAction action)
	{
		try
		{
			regMan.processReqistrationRequest(requestState.getRequestId(), 
					requestState.getRequest(), action, null, null);
			bus.fireEvent(new RegistrationRequestChangedEvent(requestState.getRequestId()));
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage("RequestProcessingPanel.errorRequestProcess"), e);
		}
	}
}
