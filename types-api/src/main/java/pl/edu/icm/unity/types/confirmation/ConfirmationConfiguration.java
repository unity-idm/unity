/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.confirmation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Confirmation subsystem configuration entry
 * 
 * @author P. Piernik
 * 
 */
public class ConfirmationConfiguration implements NamedObject
{
	public static final int DEFAULT_VALIDITY = 48*60;
	private String typeToConfirm;
	private String nameToConfirm;
	private String notificationChannel;
	private String msgTemplate;
	private int validityTime = DEFAULT_VALIDITY;
		
	private ConfirmationConfiguration()
	{
	}
	
	public ConfirmationConfiguration(String typeToConfirm, String nameToConfirm,
			String notificationChannel, String msgTemplate, int validityTime)
	{
		this.typeToConfirm = typeToConfirm;
		this.nameToConfirm = nameToConfirm;
		this.notificationChannel = notificationChannel;
		this.msgTemplate = msgTemplate;
		this.validityTime = validityTime;
	}

	@JsonCreator
	public ConfirmationConfiguration(ObjectNode root)
	{
		fromJson(root);
	}
	
	@Override
	public String getName()
	{
		return typeToConfirm+nameToConfirm;
	}

	public String getMsgTemplate()
	{
		return msgTemplate;
	}

	public void setMsgTemplate(String msgTemplate)
	{
		this.msgTemplate = msgTemplate;
	}

	public String getNotificationChannel()
	{
		return notificationChannel;
	}

	public void setNotificationChannel(String notificationChannel)
	{
		this.notificationChannel = notificationChannel;
	}

	public String getNameToConfirm()
	{
		return nameToConfirm;
	}

	public void setNameToConfirm(String nameToConfirm)
	{
		this.nameToConfirm = nameToConfirm;
	}

	public String getTypeToConfirm()
	{
		return typeToConfirm;
	}

	public void setTypeToConfirm(String typeToConfirm)
	{
		this.typeToConfirm = typeToConfirm;
	}

	public int getValidityTime()
	{
		return validityTime;
	}

	public void setValidityTime(int validityTime)
	{
		this.validityTime = validityTime;
	}

	private void fromJson(ObjectNode root)
	{
		setNameToConfirm(root.get("nameToConfirm").asText());
		setTypeToConfirm(root.get("typeToConfirm").asText());
		setMsgTemplate(root.get("msgTemplate").asText());
		setNotificationChannel(root.get("notificationChannel").asText());
		if (JsonUtil.notNull(root, "validityTime")) 
			validityTime = root.get("validityTime").asInt();	
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("nameToConfirm", getNameToConfirm());
		root.put("typeToConfirm", getTypeToConfirm());
		root.put("msgTemplate", getMsgTemplate());
		root.put("notificationChannel", getNotificationChannel());
		root.put("validityTime", getValidityTime());
		return root;
	}

	public ConfirmationConfiguration clone()
	{
		ObjectNode json = toJson();
		return new ConfirmationConfiguration(json);
	}

	@Override
	public String toString()
	{
		return "ConfirmationConfiguration [typeToConfirm=" + typeToConfirm
				+ ", nameToConfirm=" + nameToConfirm + ", notificationChannel="
				+ notificationChannel + ", msgTemplate=" + msgTemplate
				+ ", validityTime=" + validityTime + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + validityTime;
		result = prime * result + ((msgTemplate == null) ? 0 : msgTemplate.hashCode());
		result = prime * result + ((nameToConfirm == null) ? 0 : nameToConfirm.hashCode());
		result = prime * result + ((notificationChannel == null) ? 0
				: notificationChannel.hashCode());
		result = prime * result + ((typeToConfirm == null) ? 0 : typeToConfirm.hashCode());
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
		ConfirmationConfiguration other = (ConfirmationConfiguration) obj;
		if (validityTime != other.validityTime)
			return false;
		if (msgTemplate == null)
		{
			if (other.msgTemplate != null)
				return false;
		} else if (!msgTemplate.equals(other.msgTemplate))
			return false;
		if (nameToConfirm == null)
		{
			if (other.nameToConfirm != null)
				return false;
		} else if (!nameToConfirm.equals(other.nameToConfirm))
			return false;
		if (notificationChannel == null)
		{
			if (other.notificationChannel != null)
				return false;
		} else if (!notificationChannel.equals(other.notificationChannel))
			return false;
		if (typeToConfirm == null)
		{
			if (other.typeToConfirm != null)
				return false;
		} else if (!typeToConfirm.equals(other.typeToConfirm))
			return false;
		return true;
	}
	
	
	public static Builder builder()
	{
		return new Builder(new ConfirmationConfiguration());
	}

	public static class Builder
	{
		private ConfirmationConfiguration instance;

		protected Builder(ConfirmationConfiguration aInstance)
		{
			instance = aInstance;
		}

		protected ConfirmationConfiguration getInstance()
		{
			return instance;
		}

		public Builder withMsgTemplate(String aValue)
		{
			instance.setMsgTemplate(aValue);
			return this;
		}

		public Builder withNotificationChannel(String aValue)
		{
			instance.setNotificationChannel(aValue);
			return this;
		}

		public Builder withNameToConfirm(String aValue)
		{
			instance.setNameToConfirm(aValue);
			return this;
		}

		public Builder withTypeToConfirm(String aValue)
		{
			instance.setTypeToConfirm(aValue);
			return this;
		}

		public Builder withValidityTime(int validity)
		{
			instance.setValidityTime(validity);
			return this;
		}
		
		public ConfirmationConfiguration build()
		{
			return getInstance();
		}
	}
}
