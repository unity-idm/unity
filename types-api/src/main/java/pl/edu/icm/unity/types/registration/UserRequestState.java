/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.NamedObject;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;


/**
 * Records an information about registration request or enquiry state. 
 * The objects of this class are fully managed by the engine, users can only retrieve it.  
 * 
 * @author K. Benedyczak
 */
public abstract class UserRequestState<T extends BaseRegistrationInput> implements NamedObject
{
	private String requestId;
	private Date timestamp;
	private T request;
	private RegistrationContext registrationContext;
	private List<AdminComment> adminComments = new ArrayList<>();
	private RegistrationRequestStatus status;

	public UserRequestState()
	{
	}

	public UserRequestState(ObjectNode root)
	{
		try
		{
			fromJson(root);
		} catch (IOException e)
		{
			throw new IllegalArgumentException("Invalid JSON", e);
		}
	}

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
	@Override
	public String getName()
	{
		return getRequestId();
	}

	@Override
	public String toString()
	{
		return "UserRequestState [requestId=" + requestId + ", timestamp=" + timestamp
				+ ", request=" + request + ", registrationContext="
				+ registrationContext + ", adminComments=" + adminComments
				+ ", status=" + status + "]";
	}
	
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		ObjectNode root = getRequest().toJson();
		root.set("AdminComments", jsonMapper.valueToTree(getAdminComments()));
		root.set("RequestId", jsonMapper.valueToTree(getRequestId()));
		root.set("Status", jsonMapper.valueToTree(getStatus()));
		root.set("Timestamp", jsonMapper.valueToTree(getTimestamp().getTime()));
		root.set("Context", getRegistrationContext().toJson());
		return root;
	}

	
	private void fromJson(ObjectNode root) throws IOException
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		JsonNode n = root.get("AdminComments");
		if (n != null)
		{
			String v = jsonMapper.writeValueAsString(n);
			List<AdminComment> r = jsonMapper.readValue(v, 
					new TypeReference<List<AdminComment>>(){});
			setAdminComments(r);
		}

		n = root.get("RequestId");
		setRequestId(n.asText());

		n = root.get("Status");
		setStatus(RegistrationRequestStatus.valueOf(n.asText()));

		n = root.get("Timestamp");
		setTimestamp(new Date(n.longValue()));

		n = root.get("Context");
		if (n != null)
			setRegistrationContext(new RegistrationContext(n));
		else
			setRegistrationContext(new RegistrationContext(true, TriggeringMode.manualAtLogin));
		
		setRequest(parseRequestFromJson(root));
	}
	
	protected abstract T parseRequestFromJson(ObjectNode root);
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((adminComments == null) ? 0 : adminComments.hashCode());
		result = prime * result + ((registrationContext == null) ? 0
				: registrationContext.hashCode());
		result = prime * result + ((request == null) ? 0 : request.hashCode());
		result = prime * result + ((requestId == null) ? 0 : requestId.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserRequestState<?> other = (UserRequestState<?>) obj;
		if (adminComments == null)
		{
			if (other.adminComments != null)
				return false;
		} else if (!adminComments.equals(other.adminComments))
			return false;
		if (registrationContext == null)
		{
			if (other.registrationContext != null)
				return false;
		} else if (!registrationContext.equals(other.registrationContext))
			return false;
		if (request == null)
		{
			if (other.request != null)
				return false;
		} else if (!request.equals(other.request))
			return false;
		if (requestId == null)
		{
			if (other.requestId != null)
				return false;
		} else if (!requestId.equals(other.requestId))
			return false;
		if (status != other.status)
			return false;
		if (timestamp == null)
		{
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		return true;
	}
}
