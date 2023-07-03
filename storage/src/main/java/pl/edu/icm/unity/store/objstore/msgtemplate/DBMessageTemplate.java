/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.msgtemplate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.store.types.common.I18nStringMapper;

class DBMessageTemplate
{
	private static final String DEFAULT_TYPE = "PLAIN";

	final String name;
	final String description;
	final DBI18nMessage message;
	final String consumer;
	final String type;
	final String notificationChannel;

	private DBMessageTemplate(Builder builder)
	{
		this.name = builder.name;
		this.description = builder.description;
		this.message = builder.message;
		this.consumer = builder.consumer;
		this.type = builder.type;
		this.notificationChannel = builder.notificationChannel;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(consumer, description, message, name, notificationChannel, type);
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
		DBMessageTemplate other = (DBMessageTemplate) obj;
		return Objects.equals(consumer, other.consumer) && Objects.equals(description, other.description)
				&& Objects.equals(message, other.message) && Objects.equals(name, other.name)
				&& Objects.equals(notificationChannel, other.notificationChannel) && Objects.equals(type, other.type);
	}

	@JsonCreator
	public DBMessageTemplate(ObjectNode root)
	{
		name = root.get("name")
				.asText();
		description = root.get("description")
				.asText();
		consumer = root.get("consumer")
				.asText();

		String messageType = DEFAULT_TYPE;
		if (JsonUtil.notNull(root, "type"))
			messageType = root.get("type")
					.asText();
		type = messageType;

		if (JsonUtil.notNull(root, "notificationChannel"))
			notificationChannel = root.get("notificationChannel")
					.asText();
		else
			notificationChannel = null;

		ArrayNode messagesA = (ArrayNode) root.get("messages");
		I18nString subject = new I18nString();
		I18nString body = new I18nString();
		for (int i = 0; i < messagesA.size(); i++)
		{
			ObjectNode jsonMsg = (ObjectNode) messagesA.get(i);
			String locale = jsonMsg.get("locale")
					.asText();
			if (locale.equals(""))
			{
				JsonNode n = jsonMsg.get("subject");
				if (n != null && !n.isNull())
					subject.setDefaultValue(n.asText());
				n = jsonMsg.get("body");
				if (n != null && !n.isNull())
					body.setDefaultValue(n.asText());
			} else
			{
				JsonNode n = jsonMsg.get("subject");
				if (n != null && !n.isNull())
					subject.addValue(locale, n.asText());
				n = jsonMsg.get("body");
				if (n != null && !n.isNull())
					body.addValue(locale, n.asText());
			}
		}
		message = DBI18nMessage.builder()
				.withBody(I18nStringMapper.map(body))
				.withSubject(I18nStringMapper.map(subject))
				.build();

	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("name", name);
		root.put("description", description);
		root.put("consumer", consumer);
		if (type != null)
			root.put("type", type);
		ArrayNode jsonMessages = root.putArray("messages");
		root.put("notificationChannel", notificationChannel);

		I18nString subject = I18nStringMapper.map(message.subject);
		I18nString body = I18nStringMapper.map(message.body);
		Set<String> allUsedLocales = new HashSet<>(body.getMap()
				.keySet());
		allUsedLocales.addAll(subject.getMap()
				.keySet());
		for (String locale : allUsedLocales)
		{
			ObjectNode jsonMsg = jsonMessages.addObject();
			jsonMsg.put("locale", locale);
			jsonMsg.put("subject", subject.getValueRaw(locale));
			jsonMsg.put("body", body.getValueRaw(locale));
		}
		if (subject.getDefaultValue() != null || body.getDefaultValue() != null)
		{
			ObjectNode jsonMsg = jsonMessages.addObject();
			jsonMsg.put("locale", "");
			jsonMsg.put("subject", subject.getDefaultValue());
			jsonMsg.put("body", body.getDefaultValue());
		}
		return root;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String description;
		private DBI18nMessage message;
		private String consumer;
		private String type;
		private String notificationChannel;

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withMessage(DBI18nMessage message)
		{
			this.message = message;
			return this;
		}

		public Builder withConsumer(String consumer)
		{
			this.consumer = consumer;
			return this;
		}

		public Builder withType(String type)
		{
			this.type = type;
			return this;
		}

		public Builder withNotificationChannel(String notificationChannel)
		{
			this.notificationChannel = notificationChannel;
			return this;
		}

		public DBMessageTemplate build()
		{
			return new DBMessageTemplate(this);
		}
	}

}
