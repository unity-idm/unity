/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

/**
 * Shows request contents and provides a possibility to edit it.
 * 
 * @author K. Benedyczak
 */
public class RequestReviewPanel extends CustomComponent
{
	private UnityMessageSource msg;
	private RegistrationRequestState requestState;
	
	private DescriptionTextArea comment;
	
	public RequestReviewPanel(UnityMessageSource msg)
	{
		this.msg = msg;
		initUI();
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(true);
		
		comment = new DescriptionTextArea(msg.getMessage("RequestReviewPanel.comment"), true, "");
		
		main.addComponents(comment);
		setCompositionRoot(main);
	}
	
	public void setInput(RegistrationRequestState requestState)
	{
		RegistrationRequest request = requestState.getRequest();
		String comments = request.getComments();
		if (comments != null && !comments.equals(""))
		{
			comment.setVisible(true);
			comment.setValue(comments);
		} else
			comment.setVisible(false);
	}
}
