/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.requests;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;

import io.imunity.console.views.directory_browser.group_browser.GroupChangedEvent;
import io.imunity.console.views.signup_and_enquiry.EnquiryResponsesChangedEvent;
import io.imunity.console.views.signup_and_enquiry.RegistrationRequestsChangedEvent;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.BaseRegistrationInput;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Responsible for displaying a submitted request
 * ({@link RegistrationRequestState}), its editing and processing.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
class RequestProcessingPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RequestProcessingPanel.class);

	private final MessageSource msg;
	private final RegistrationsManagement regMan;
	private final GenericReviewPanel requestReviewPanel;
	private final EnquiryManagement enquiryMan;
	private final NotificationPresenter notificationPresenter;

	private RequestCommentPanel commentPanel;
	private UserRequestState<?> requestState;
	private Button accept;
	private Button reject;
	private Button delete;
	private NativeLabel requestType;
	private NativeLabel requestForm;
	private NativeLabel requestId;
	private NativeLabel requestStatus;
	private NativeLabel requestDate;

	@Autowired
	RequestProcessingPanel(MessageSource msg, RegistrationsManagement regMan, EnquiryManagement enquiryMan,
			GenericReviewPanel requestReviewPanel, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.enquiryMan = enquiryMan;
		this.requestReviewPanel = requestReviewPanel;
		this.notificationPresenter = notificationPresenter;
		initUI();
	}

	private void initUI()
	{
		requestType = new NativeLabel();
		requestForm = new NativeLabel();
		requestId = new NativeLabel();
		requestStatus = new NativeLabel();
		requestDate = new NativeLabel();

		FormLayout topInfo = new FormLayout();
		topInfo.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		topInfo.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		topInfo.addFormItem(requestType, msg.getMessage("RegistrationRequest.type") + ":");
		topInfo.addFormItem(requestForm, msg.getMessage("RegistrationRequest.form") + ":");
		topInfo.addFormItem(requestId, msg.getMessage("RegistrationRequest.requestId") + ":");
		topInfo.addFormItem(requestStatus, msg.getMessage("RegistrationRequest.status") + ":");
		topInfo.addFormItem(requestDate, msg.getMessage("RegistrationRequest.submitTime") + ":");

		TabSheet tabs = new TabSheet();
		tabs.setWidthFull();
		tabs.addClassName(CssClassNames.TABSHEET_FULL.getName());
		commentPanel = new RequestCommentPanel(msg, regMan, enquiryMan, notificationPresenter);

		tabs.add(msg.getMessage("RequestProcessingPanel.requested"), requestReviewPanel)
				.addClassName(CssClassNames.TABSHEET_FULL.getName());
		tabs.add(msg.getMessage("RequestProcessingPanel.comments"), commentPanel)
				.addClassName(CssClassNames.TABSHEET_FULL.getName());

		accept = new Button(msg.getMessage("RequestProcessingPanel.accept"));
		accept.addClickListener(e -> process(RegistrationRequestAction.accept));
		reject = new Button(msg.getMessage("RequestProcessingPanel.reject"));
		reject.addClickListener(e -> process(RegistrationRequestAction.reject));
		delete = new Button(msg.getMessage("RequestProcessingPanel.drop"));
		delete.addClickListener(e -> process(RegistrationRequestAction.drop));
		
		HorizontalLayout buttonsBar = new HorizontalLayout();
		buttonsBar.setMargin(false);
		buttonsBar.setPadding(false);
		buttonsBar.setAlignItems(Alignment.END);
		buttonsBar.setJustifyContentMode(JustifyContentMode.END);
		buttonsBar.add(accept, reject, delete);
		buttonsBar.setWidthFull();

		add(topInfo);
		add(tabs);
		add(buttonsBar);

		setMargin(false);
		setSpacing(true);
		setVisible(false);
		setSizeFull();
	}

	void setRequest(RegistrationRequestState input)
	{
		List<RegistrationForm> forms;
		try
		{
			forms = regMan.getForms();
		} catch (EngineException e)
		{
			log.error("Can not get registration forms", e);
			notificationPresenter.showError("", msg.getMessage("RequestProcessingPanel.errorGetRegistrationForms"));
			return;
		}

		RegistrationForm form = findForm(forms, input.getRequest()
				.getFormId(), input.getRequestId());
		setValueGeneric(input, form);
		requestType.setText(msg.getMessage("RegistrationRequest.type.registration"));
		commentPanel.setInput(input);
		requestReviewPanel.setRegistration(input, form);
	}

	void setRequest(EnquiryResponseState input)
	{
		List<EnquiryForm> forms;
		try
		{
			forms = enquiryMan.getEnquires();
		} catch (EngineException e)
		{
			log.error("Can not get enquiry forms", e);
			notificationPresenter.showError("", msg.getMessage("RequestProcessingPanel.errorGetEnquiryForms"));
			return;
		}
		EnquiryForm form = findForm(forms, input.getRequest()
				.getFormId(), input.getRequestId());

		setValueGeneric(input, form);
		requestType.setText(msg.getMessage("RegistrationRequest.type.enquiry"));
		commentPanel.setInput(input);
		requestReviewPanel.setEnquiry(input, form);
	}

	private void setValueGeneric(UserRequestState<?> input, BaseForm form)
	{
		setVisible(true);
		this.requestState = input;

		BaseRegistrationInput request = input.getRequest();

		requestForm.setText(request.getFormId());
		requestId.setText(input.getRequestId());
		requestStatus.setText(msg.getMessage("RegistrationRequestStatus." + input.getStatus()));
		requestDate.setText(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).format(input.getTimestamp()));

		accept.setVisible(input.getStatus() == RegistrationRequestStatus.pending);
		reject.setVisible(input.getStatus() == RegistrationRequestStatus.pending);
	}

	private <T extends BaseForm> T findForm(List<T> forms, String id, String requestId)
	{
		for (T f : forms)
			if (f.getName()
					.equals(id))
				return f;
		throw new IllegalStateException("Got request for the non-existing form " + id + ", request id is " + requestId);
	}

	private void process(RegistrationRequestAction action)
	{
		try
		{
			if (requestState instanceof RegistrationRequestState)
			{
				regMan.processRegistrationRequest(requestState.getRequestId(), requestReviewPanel.getUpdatedRequest(),
						action, null, null);
				WebSession.getCurrent()
						.getEventBus()
						.fireEvent(new RegistrationRequestsChangedEvent());
				if (action == RegistrationRequestAction.accept)
					WebSession.getCurrent()
							.getEventBus()
							.fireEvent(new GroupChangedEvent(new Group("/"), false));
			} else
			{
				enquiryMan.processEnquiryResponse(requestState.getRequestId(), requestReviewPanel.getUpdatedResponse(),
						action, null, null);
				WebSession.getCurrent()
						.getEventBus()
						.fireEvent(new EnquiryResponsesChangedEvent());
			}
		} catch (EngineException e)
		{
			log.error("Request processing error", e);
			notificationPresenter.showError(msg.getMessage("RequestProcessingPanel.errorRequestProccess"), e.getMessage());

		}
	}
}
