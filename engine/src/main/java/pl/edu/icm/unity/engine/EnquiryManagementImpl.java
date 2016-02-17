/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.reg.EnquiryFormDB;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;

/**
 * Implementation of the enquiry management API.
 * 
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class EnquiryManagementImpl implements EnquiryManagement
{
	private EnquiryFormDB enquryFormDB;
	private NotificationProducer notificationProducer;
	private UnityMessageSource msg;

	
	@Override
	public void addEnquiry(EnquiryForm form) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendEnquiry(String enquiryId) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeEnquiry(String formId) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateForm(EnquiryForm updatedForm) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<EnquiryForm> getEnquires() throws EngineException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void submitEnquiryResponse(EnquiryResponse response) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}
}
