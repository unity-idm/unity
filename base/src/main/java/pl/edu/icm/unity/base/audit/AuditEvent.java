/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.audit;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Holds information about single event that occur in the system.
 *
 * @author R. Ledzinski
 */
public class AuditEvent
{
	public static final int MAX_NAME_LENGTH = 200;

	private String name;
	private AuditEventType type;
	private Date timestamp;
	private AuditEntity subject;
	private AuditEntity initiator;
	private AuditEventAction action;
	private JsonNode details;
	private Set<String> tags;

	private AuditEvent()
	{
		this.tags = new HashSet<>();
	}

	public String getName()
	{
		return name;
	}

	public AuditEventType getType()
	{
		return type;
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public AuditEntity getSubject()
	{
		return subject;
	}

	public AuditEntity getInitiator()
	{
		return initiator;
	}

	public AuditEventAction getAction()
	{
		return action;
	}

	public JsonNode getDetails()
	{
		return details;
	}

	public Set<String> getTags()
	{
		return tags;
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final AuditEvent that = (AuditEvent) o;
		return name.equals(that.name) &&
				type == that.type &&
				timestamp.equals(that.timestamp) &&
				Objects.equals(subject, that.subject) &&
				initiator.equals(that.initiator) &&
				action == that.action &&
				Objects.equals(details, that.details) &&
				Objects.equals(tags, that.tags);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(timestamp);
	}

	@Override
	public String toString()
	{
		return "AuditEvent{" +
				"name='" + name + '\'' +
				", type=" + type +
				", timestamp=" + timestamp.getTime() +
				", subject=" + subject +
				", initiator=" + initiator +
				", action=" + action +
				", details=" + details +
				", tags=" + tags +
				'}';
	}

	public static AuditEventBuilder builder()
	{
		return new AuditEventBuilder();
	}

	public static class AuditEventBuilder
	{
		private AuditEvent auditEvent = new AuditEvent();

		public AuditEventBuilder name(final String name)
		{
			auditEvent.name = name;
			return this;
		}

		public AuditEventBuilder type(final AuditEventType type)
		{
			auditEvent.type = type;
			return this;
		}

		public AuditEventBuilder timestamp(final Date timestamp)
		{
			auditEvent.timestamp = timestamp;
			return this;
		}

		public AuditEventBuilder subject(final AuditEntity subject)
		{
			auditEvent.subject = subject;
			return this;
		}

		public AuditEventBuilder initiator(final AuditEntity initiator)
		{
			auditEvent.initiator = initiator;
			return this;
		}

		public AuditEventBuilder action(final AuditEventAction action)
		{
			auditEvent.action = action;
			return this;
		}

		public AuditEventBuilder details(final JsonNode details)
		{
			auditEvent.details = details;
			return this;
		}

		public AuditEventBuilder tags(final Set<String> tags)
		{
			auditEvent.tags = tags;
			return this;
		}

		public AuditEventBuilder tags(String... tags)
		{
			for (String tag : tags) {
				auditEvent.tags.add(tag);
			}
			return this;
		}

		public AuditEvent build()
		{
			requireNonNull(auditEvent.name, "AuditEvent.name field is required!");
			requireNonNull(auditEvent.type, "AuditEvent.type field is required!");
			requireNonNull(auditEvent.timestamp, "AuditEvent.timestamp field is required!");
			requireNonNull(auditEvent.initiator, "AuditEvent.initiator field is required!");
			requireNonNull(auditEvent.action, "AuditEvent.action field is required!");
			requireNonNull(auditEvent.tags, "AuditEvent.tags field is required!");
			return auditEvent;
		}
	}
}
