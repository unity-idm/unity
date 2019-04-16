/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.lang.NonNull;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * Holds information about single event that occur in the system.
 * @author R. Ledzinski
 */
public class AuditEvent
{
	// TODO - overall list for Audit feature
	// - investigate issue with TAG insert on newest version of mybatis (3.5.1) working well on 3.4.6
	// - add builder
	// - add DB indexes - need more details about UI abilities
	@NonNull private String name;
	@NonNull private EventType type;
	@NonNull private Date timestamp;
	private AuditEntity subject;
	@NonNull private AuditEntity initiator;
	@NonNull private EventAction action;
	private JsonNode jsonDetails;
	@NonNull private Set<String> tags;

	public AuditEvent(@NonNull final ObjectNode json) throws JsonProcessingException {
		fromJson(json);
	}

	public AuditEvent(@NonNull final String name, @NonNull final EventType type, @NonNull final Date timestamp, @NonNull final EventAction action, final JsonNode jsonDetails,
					  final AuditEntity subject, @NonNull final AuditEntity initiator, String... tags)
	{
		this(name, type, timestamp, action, jsonDetails, subject, initiator);
		for(String tag: tags) {
			this.tags.add(tag);
		}
	}

	public AuditEvent(@NonNull final String name, @NonNull final EventType type, @NonNull final Date timestamp, @NonNull final EventAction action, final JsonNode jsonDetails,
					  final AuditEntity subject, @NonNull final AuditEntity initiator, Collection<String> tags)
	{
		this(name, type, timestamp, action, jsonDetails, subject, initiator);
		this.tags.addAll(tags);
	}

	private AuditEvent(@NonNull final String name, @NonNull final EventType type, @NonNull final Date timestamp, @NonNull final EventAction action, final JsonNode jsonDetails,
					  final AuditEntity subject, @NonNull final AuditEntity initiator)
	{
		this.name = name;
		this.type = type;
		this.timestamp = timestamp;
		this.action = action;
		this.jsonDetails = jsonDetails;
		this.subject = subject;
		this.initiator = initiator;
		this.tags=new HashSet<String>();
	}

	public AuditEvent() {
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public EventType getType() {
		return type;
	}

	public void setType(final EventType type) {
		this.type = type;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(final Date timestamp) {
		this.timestamp = timestamp;
	}

	public AuditEntity getSubject() {
		return subject;
	}

	public void setSubject(final AuditEntity subject) {
		this.subject = subject;
	}

	public AuditEntity getInitiator() {
		return initiator;
	}

	public void setInitiator(final AuditEntity initiator) {
		this.initiator = initiator;
	}

	public EventAction getAction() {
		return action;
	}

	public void setAction(final EventAction action) {
		this.action = action;
	}

	public JsonNode getJsonDetails() {
		return jsonDetails;
	}

	public void setJsonDetails(final JsonNode jsonDetails) {
		this.jsonDetails = jsonDetails;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(final Set<String> tags) {
		this.tags = tags;
	}

	public void assertValid() {
		assertNonNullValid(this);
		assertNonNullValid(initiator);
		if (subject != null) {
			assertNonNullValid(subject);
		}
	}

	private static <T> void assertNonNullValid(T obj)
	{
		// Check NonNull
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(NonNull.class)) {
				try {
					if (field.get(obj) == null) {
						throw new IllegalArgumentException(String.join("",field.getName(), " is required field of ", obj.getClass().getSimpleName()));
					}
				} catch (IllegalAccessException e) {
					throw new IllegalArgumentException(String.join("Error validating ", obj.getClass().getSimpleName(), ".", field.getName(), " field."), e);

				}
			}
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final AuditEvent that = (AuditEvent) o;
		return name.equals(that.name) &&
				type == that.type &&
				timestamp.equals(that.timestamp) &&
				Objects.equals(subject, that.subject) &&
				initiator.equals(that.initiator) &&
				action == that.action &&
				Objects.equals(jsonDetails, that.jsonDetails) &&
				Objects.equals(tags, that.tags);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp);
	}

	@Override
	public String toString() {
		return "AuditEvent{" +
				"name='" + name + '\'' +
				", type=" + type +
				", timestamp=" + timestamp +
				", subject=" + subject +
				", initiator=" + initiator +
				", action=" + action +
				", jsonDetails=" + jsonDetails +
				", tags=" + tags +
				'}';
	}

	public ObjectNode toJson() {
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("name", getName());
		root.put("type", getType().toString());
		root.put("timestamp", getTimestamp().getTime());
		root.put("action", getAction().toString());
		if (getSubject() != null) {
			root.set("subject", Constants.MAPPER.valueToTree(getSubject()));
		}
		root.set("initiator", Constants.MAPPER.valueToTree(getInitiator()));

		root.set("jsonDetails", jsonDetails);
		ArrayNode tagsNode = root.putArray("tags");
		for (String tag: tags)
			tagsNode.add(tag);
		return root;
	}

	public void fromJson(ObjectNode main) throws JsonProcessingException {
		setName(main.get("name").asText());
		setType(EventType.valueOf(main.get("type").asText()));
		setTimestamp(new Date(main.get("timestamp").asLong()));
		setAction(EventAction.valueOf(main.get("action").asText()));
		if (main.has("subject")) {
			setSubject(Constants.MAPPER.treeToValue(main.get("subject"), AuditEntity.class));
		}
		setInitiator(Constants.MAPPER.treeToValue(main.get("initiator"), AuditEntity.class));
		setJsonDetails(main.get("jsonDetails"));
		ArrayNode tagsNode = main.withArray("tags");
		this.tags = new HashSet<String>(tagsNode.size());
		Iterator<JsonNode> it = tagsNode.iterator();
		while(it.hasNext()) {
			this.tags.add(it.next().asText());
		}
	}

	public static class AuditEntity {
		@NonNull Long entityId;
		@NonNull String name;
		@NonNull String email;

		public AuditEntity(@NonNull final Long entityId, @NonNull final String name, @NonNull final String email) {
			this.entityId = entityId;
			this.name = name;
			this.email = email;
		}

		public AuditEntity(@NonNull final ObjectNode json) {
			fromJson(json);
		}

		public AuditEntity() {
		}

		public Long getEntityId() {
			return entityId;
		}

		public void setEntityId(final Long entityId) {
			this.entityId = entityId;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(final String email) {
			this.email = email;
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final AuditEntity that = (AuditEntity) o;
			return entityId.equals(that.entityId) &&
					name.equals(that.name) &&
					email.equals(that.email);
		}

		@Override
		public int hashCode() {
			return Objects.hash(entityId);
		}

		@Override
		public String toString() {
			return "AuditEntity{" +
					"entityId=" + entityId +
					", name='" + name + '\'' +
					", email='" + email + '\'' +
					'}';
		}

		public ObjectNode toJson() {
			ObjectNode main = Constants.MAPPER.createObjectNode();
			main.put("entityId", getEntityId());
			main.put("name", getName());
			main.put("email", getEmail());
			return main;
		}

		public void fromJson(ObjectNode main) {
			setEntityId(main.get("entityId").asLong());
			setName(main.get("name").asText());
			setEmail(main.get("email").asText());}
	}

	public enum EventType {
		ENTITY,
		IDENTITY,
		GROUP,
		SESSION,
		CREDENTIALS;
	}

	public enum EventAction {
		ADD,
		UPDATE,
		REMOVE;
	}
}
