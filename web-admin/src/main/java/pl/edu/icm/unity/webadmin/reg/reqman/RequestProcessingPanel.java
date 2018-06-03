/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import java.text.SimpleDateFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestChangedEvent;

/**
 * Responsible for displaying a submitted request ({@link RegistrationRequestState}), its editing and processing.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class RequestProcessingPanel extends CustomComponent
{
	private UnityMessageSource msg;
	private RegistrationsManagement regMan;
	
	private EventsBus bus;
	private RequestCommentPanel commentPanel;
	private GenericReviewPanel requestReviewPanel;
	private UserRequestState<?> requestState;
	private Button accept;
	private Button reject;
	private Button delete;
	private VerticalLayout main;
	private Label requestType;
	private Label requestForm;
	private Label requestId;
	private Label requestStatus;
	private Label requestDate;
	private EnquiryManagement enquiryMan;
	
	@Autowired
	public RequestProcessingPanel(UnityMessageSource msg, RegistrationsManagement regMan,
			EnquiryManagement enquiryMan, GenericReviewPanel requestReviewPanel)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.enquiryMan = enquiryMan;
		this.requestReviewPanel = requestReviewPanel;
		this.bus = WebSession.getCurrent().getEventBus();
		initUI();
	}
	
	private void initUI()
	{
		requestType = new Label();
		requestType.setCaption(msg.getMessage("RegistrationRequest.type")+":");
		requestForm = new Label();
		requestForm.setCaption(msg.getMessage("RegistrationRequest.form")+":");
		requestId = new Label();
		requestId.setCaption(msg.getMessage("RegistrationRequest.requestId")+":");
		requestStatus = new Label();
		requestStatus.setCaption(msg.getMessage("RegistrationRequest.status")+":");
		requestDate = new Label();
		requestDate.setCaption(msg.getMessage("RegistrationRequest.submitTime")+":");
		
		FormLayout topInfo = new CompactFormLayout(requestType, requestForm, requestStatus, 
				requestDate, requestId);
		
		TabSheet tabs = new TabSheet();
		tabs.addStyleName(Styles.vTabsheetMinimal.toString());
		
		commentPanel = new RequestCommentPanel(msg, regMan, enquiryMan);
		commentPanel.setCaption(msg.getMessage("RequestProcessingPanel.comments"));
		
		requestReviewPanel.setCaption(msg.getMessage("RequestProcessingPanel.requested"));
		
		tabs.addComponent(requestReviewPanel);
		tabs.addComponent(commentPanel);
		
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
		delete = new Button(msg.getMessage("RequestProcessingPanel.drop"));
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
		buttonsBar.setMargin(false);
		
		
		main = new VerticalLayout(topInfo, tabs, buttonsBar);
		main.setComponentAlignment(buttonsBar, Alignment.BOTTOM_RIGHT);
		main.setMargin(true);
		main.setSpacing(true);
		main.setVisible(false);
		setCompositionRoot(main);
	}
	
	public void setRequest(RegistrationRequestState input)
	{
		List<RegistrationForm> forms;
		try
		{
			forms = regMan.getForms();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RequestsTable.errorGetRequests"), e);
			return;
		}

		RegistrationForm form = findForm(forms, input.getRequest().getFormId(), input.getRequestId());
		setValueGeneric(input, form);
		requestType.setValue(msg.getMessage("RequestsTable.type.registration"));
		commentPanel.setInput(input);
		requestReviewPanel.setRegistration(input, form);		
	}

	public void setRequest(EnquiryResponseState input)
	{
		List<EnquiryForm> forms;
		try
		{
			forms = enquiryMan.getEnquires();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RequestsTable.errorGetRequests"), e);
			return;
		}
		EnquiryForm form = findForm(forms, input.getRequest().getFormId(), input.getRequestId());
		
		setValueGeneric(input, form);
		requestType.setValue(msg.getMessage("RequestsTable.type.enquiry"));
		commentPanel.setInput(input);
		requestReviewPanel.setEnquiry(input, form);		
	}
	
	private void setValueGeneric(UserRequestState<?> input, BaseForm form)
	{
		main.setVisible(true);
		this.requestState = input;

		BaseRegistrationInput request = input.getRequest();

		requestForm.setValue(request.getFormId());
		requestId.setValue(input.getRequestId());
		requestStatus.setValue(msg.getMessage("RegistrationRequestStatus." + input.getStatus()));
		requestDate.setValue(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).format(input.getTimestamp()));
		
		accept.setVisible(input.getStatus() == RegistrationRequestStatus.pending);
		reject.setVisible(input.getStatus() == RegistrationRequestStatus.pending);
	}
	
	private <T extends BaseForm> T findForm(List<T> forms, String id, String requestId)
	{
		for (T f: forms)
			if (f.getName().equals(id))
				return f;
		throw new IllegalStateException("Got request for the non-existing form " + 
					id + ", request id is " + requestId);
	}
	
	private void process(RegistrationRequestAction action)
	{
		try
		{
			if (requestState instanceof RegistrationRequestState)
			{
				regMan.processRegistrationRequest(requestState.getRequestId(), 
						requestReviewPanel.getUpdatedRequest(), action, null, null);
				bus.fireEvent(new RegistrationRequestChangedEvent(requestState.getRequestId()));
				if (action == RegistrationRequestAction.accept)
					bus.fireEvent(new GroupChangedEvent("/"));
			} else
			{
				enquiryMan.processEnquiryResponse(requestState.getRequestId(), 
						requestReviewPanel.getUpdatedResponse(), action, null, null);
				bus.fireEvent(new EnquiryResponseChangedEvent(requestState.getRequestId()));
			}
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RequestProcessingPanel.errorRequestProccess"), e);
		}
	}
}
