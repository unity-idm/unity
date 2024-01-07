/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.requests;

import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import io.imunity.console.views.signup_and_enquiry.EnquiryResponsesChangedEvent;
import io.imunity.console.views.signup_and_enquiry.RegistrationRequestsChangedEvent;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.AdminComment;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;

/**
 * Allows to browse and post request comments. Works with both enquiry and
 * registration requests.
 * 
 * @author K. Benedyczak
 */
class RequestCommentPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RequestCommentPanel.class);

	private final MessageSource msg;
	private final RegistrationsManagement regMan;
	private final EnquiryManagement enquiryMan;
	private final NotificationPresenter notificationPresenter;
	private UserRequestState<?> requestState;

	private VerticalLayout contentP;
	private TextArea commentField;

	RequestCommentPanel(MessageSource msg, RegistrationsManagement regMan, EnquiryManagement enquiryMan, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.enquiryMan = enquiryMan;
		this.notificationPresenter = notificationPresenter;
		initUI();
	}

	private void initUI()
	{
		setSpacing(true);
		setPadding(false);
		setMargin(false);
		
		contentP = new VerticalLayout();
		contentP.setSpacing(false);
		contentP.setMargin(false);
		contentP.setPadding(false);
		contentP.setWidthFull();

		Button postPublic = new Button(msg.getMessage("RequestProcessingPanel.postPublic"));
		postPublic.setTooltipText(msg.getMessage("RequestProcessingPanel.postPublicTooltip"));
		postPublic.addClickListener(e ->
				process(commentField.getValue(), true)
		);
		Button postInternal = new Button(msg.getMessage("RequestProcessingPanel.postInternal"));
		postInternal.setTooltipText(msg.getMessage("RequestProcessingPanel.postInternalTooltip"));
		postInternal.addClickListener(e ->
				process(commentField.getValue(), false)
		);
		
		commentField = new TextArea();
		commentField.setWidthFull();
		
		HorizontalLayout buttons = new HorizontalLayout(postPublic, postInternal);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		buttons.setPadding(false);
		
		add(contentP, commentField, buttons);
	}

	private void process(String comment, boolean asPublic)
	{
		try
		{
			if (requestState instanceof RegistrationRequestState)
			{
				regMan.processRegistrationRequest(requestState.getRequestId(), null, RegistrationRequestAction.update,
						asPublic ? comment : null, asPublic ? null : comment);
				WebSession.getCurrent().getEventBus().fireEvent(new RegistrationRequestsChangedEvent());
			} else
			{
				enquiryMan.processEnquiryResponse(requestState.getRequestId(), null, RegistrationRequestAction.update,
						asPublic ? comment : null, asPublic ? null : comment);
				WebSession.getCurrent().getEventBus().fireEvent(new EnquiryResponsesChangedEvent());
			}
		} catch (EngineException e)
		{
			log.error("Request processing error", e);
			notificationPresenter.showError("", msg.getMessage("RequestProcessingPanel.errorRequestProcess"));
		}
	}

	void setInput(RegistrationRequestState requestState)
	{
		setInputGeneric(requestState);
	}

	void setInput(EnquiryResponseState requestState)
	{
		setInputGeneric(requestState);
	}

	private void setInputGeneric(UserRequestState<?> requestState)
	{
		commentField.setValue("");
		contentP.removeAll();
		this.requestState = requestState;
		List<AdminComment> comments = requestState.getAdminComments();
		for (AdminComment comment : comments)
		{
			StringBuilder sb = new StringBuilder();
			if (comment.isPublicComment())
				sb.append(msg.getMessage("RequestCommentPanel.public"));
			else
				sb.append(msg.getMessage("RequestCommentPanel.internal"));
			sb.append(" ")
					.append(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).format(comment.getDate()));
			sb.append("\n\n")
					.append(comment.getContents());
			NativeLabel commentL = new NativeLabel();
			commentL.setText(sb.toString());
	//		commentL.addStyleName(Styles.messageBox.toString());
			contentP.add(commentL);
		}
	}
}
