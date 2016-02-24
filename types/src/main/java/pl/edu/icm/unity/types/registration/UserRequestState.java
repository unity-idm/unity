/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Records an information about registration request or enquiry state. 
 * The objects of this class are fully managed by the engine, users can only retrieve it.  
 * 
 * @author K. Benedyczak
 */
public class UserRequestState<T extends BaseRegistrationInput>
{
	private String requestId;
	private Date timestamp;
	private T request;
	private RegistrationContext registrationContext;
	private List<AdminComment> adminComments = new ArrayList<>();
	private RegistrationRequestStatus status;

	public String getRequestId()
	{
		return requestId;
	}
	public void setRequestId(String requestId)
	{
		this.requestId = requestId;
	}
	public Date getTimestamp()
	{
		return timestamp;
	}
	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
	}
	public T getRequest()
	{
		return request;
	}
	public void setRequest(T request)
	{
		this.request = request;
	}
	public List<AdminComment> getAdminComments()
	{
		return adminComments;
	}
	public void setAdminComments(List<AdminComment> adminComments)
	{
		this.adminComments = adminComments;
	}
	public RegistrationRequestStatus getStatus()
	{
		return status;
	}
	public void setStatus(RegistrationRequestStatus status)
	{
		this.status = status;
	}
	public RegistrationContext getRegistrationContext()
	{
		return registrationContext;
	}
	public void setRegistrationContext(RegistrationContext registrationContext)
	{
		this.registrationContext = registrationContext;
	}
}
