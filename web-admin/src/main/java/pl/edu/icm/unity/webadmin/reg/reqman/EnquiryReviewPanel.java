/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.reqman;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Shows enquiry response contents and provides a possibility to edit it.
 * 
 * @author K. Benedyczak
 */
public class EnquiryReviewPanel extends RequestReviewPanelBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryReviewPanel.class);
	private Label entity;
	private IdentitiesManagement identitiesManagement;
	
	public EnquiryReviewPanel(UnityMessageSource msg, AttributeHandlerRegistry handlersRegistry,
			IdentityTypesRegistry idTypesRegistry, IdentitiesManagement identitiesManagement)
	{
		super(msg, handlersRegistry, idTypesRegistry);
		this.identitiesManagement = identitiesManagement;
		initUI();
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(true);
		entity = new Label();
		entity.setCaption(msg.getMessage("EnquiryReviewPanel.enquirySubmitter"));
		main.addComponent(entity);
		super.addStandardComponents(main);
		setCompositionRoot(main);
	}
	
	public EnquiryResponse getUpdatedRequest()
	{
		EnquiryResponse ret = new EnquiryResponse();
		super.fillRequest(ret);
		return ret;
	}
	
	public void setInput(EnquiryResponseState requestState, EnquiryForm form)
	{
		super.setInput(requestState, form);
		String label = null;
		try
		{
			label = identitiesManagement.getEntityLabel(new EntityParam(requestState.getEntityId()));
		} catch (EngineException e)
		{
			log.warn("Can not establish entity label", e);
		}
		if (label == null)
			label = "";
		label += " [" + requestState.getEntityId() + "]";
		entity.setValue(label);
	}
}
