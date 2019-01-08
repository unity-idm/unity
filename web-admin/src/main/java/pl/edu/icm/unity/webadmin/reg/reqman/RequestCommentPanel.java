/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import java.text.SimpleDateFormat;
import java.util.List;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.UserRequestState;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestChangedEvent;

/**
 * Allows to browse and post request comments. Works with both enquiry and registration requests.
 * @author K. Benedyczak
 */
public class RequestCommentPanel extends CustomComponent
{
	private UnityMessageSource msg;
	private RegistrationsManagement regMan;
	private EnquiryManagement enquiryMan;
	private UserRequestState<?> requestState;
	
	private EventsBus bus;
	private VerticalLayout contentP; 
	private TextArea commentField;
	
	public RequestCommentPanel(UnityMessageSource msg, RegistrationsManagement regMan,
			EnquiryManagement enquiryMan)
	{
		this.msg = msg;
		this.regMan = regMan;
		this.enquiryMan = enquiryMan;
		this.bus = WebSession.getCurrent().getEventBus();
		initUI();
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		
		contentP = new VerticalLayout();
		contentP.setSpacing(true);
		contentP.setMargin(false);
		contentP.setHeight(80, Unit.PERCENTAGE);
		
		Button postPublic = new Button(msg.getMessage("RequestProcessingPanel.postPublic"));
		postPublic.setDescription(msg.getMessage("RequestProcessingPanel.postPublicTooltip"));
		postPublic.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				process(commentField.getValue(), true);
			}
		});
		Button postInternal = new Button(msg.getMessage("RequestProcessingPanel.postInternal"));
		postInternal.setDescription(msg.getMessage("RequestProcessingPanel.postInternalTooltip"));
		postInternal.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				process(commentField.getValue(), false);
			}
		});
		
		commentField = new TextArea();
		commentField.setWidth(100, Unit.PERCENTAGE);
		
		HorizontalLayout buttons = new HorizontalLayout(postPublic, postInternal);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		
		main.addComponents(contentP, commentField, buttons);
		setCompositionRoot(main);
	}
	
	private void process(String comment, boolean asPublic)
	{
		try
		{
			if (requestState instanceof RegistrationRequestState)
			{
				regMan.processRegistrationRequest(requestState.getRequestId(), 
					null, RegistrationRequestAction.update, 
					asPublic ? comment : null, asPublic ? null : comment);
				bus.fireEvent(new RegistrationRequestChangedEvent(requestState.getRequestId()));
			} else
			{
				enquiryMan.processEnquiryResponse(requestState.getRequestId(), 
						null, RegistrationRequestAction.update, 
						asPublic ? comment : null, asPublic ? null : comment);
				bus.fireEvent(new EnquiryResponseChangedEvent(requestState.getRequestId()));
			}
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("RequestProcessingPanel.errorRequestProcess"), e);
		}
	}

	public void setInput(RegistrationRequestState requestState)
	{
		setInputGeneric(requestState);
	}
	
	public void setInput(EnquiryResponseState requestState)
	{
		setInputGeneric(requestState);
	}

	private void setInputGeneric(UserRequestState<?> requestState)
	{
		commentField.setValue("");
		contentP.removeAllComponents();
		this.requestState = requestState;
		List<AdminComment> comments = requestState.getAdminComments();
		for (AdminComment comment: comments)
		{
			StringBuilder sb = new StringBuilder();
			if (comment.isPublicComment())
				sb.append(msg.getMessage("RequestCommentPanel.public"));
			else
				sb.append(msg.getMessage("RequestCommentPanel.internal"));
			sb.append(" ").append(new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT).
					format(comment.getDate()));
			sb.append("\n\n").append(comment.getContents());
			Label commentL = new Label(sb.toString(), ContentMode.PREFORMATTED);
			commentL.addStyleName(Styles.messageBox.toString());
			contentP.addComponent(commentL);
		}
	}
}
