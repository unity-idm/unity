/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.types;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.describedObject.NamedObject;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.i18n.I18nStringJsonUtil;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;

/**
 * Stores information about policy document
 * 
 * @author P.Piernik
 *
 */
public class StoredPolicyDocument implements NamedObject
{
	private Long id;
	private String name;
	private I18nString displayedName;
	private int revision;
	private boolean mandatory;
	private PolicyDocumentContentType contentType;
	private I18nString content;

	StoredPolicyDocument(Long id, String name, I18nString displayedName, int revision, boolean mandatory,
			I18nString content, PolicyDocumentContentType contentType)
	{
		this.id = id;
		this.name = name;
		this.displayedName = displayedName;
		this.revision = revision;
		this.mandatory = mandatory;
		this.content = content;
		this.contentType = contentType;
	}

	public StoredPolicyDocument()
	{
		contentType = PolicyDocumentContentType.EMBEDDED;
		revision = 1;
	}

	@JsonCreator
	public StoredPolicyDocument(ObjectNode src)
	{
		fromJson(src);
	}

	public StoredPolicyDocument(Long id, String name)
	{
		this.id = id;
		this.name = name;
	}

	public ObjectNode toJson()
	{
		ObjectNode main = toJsonBase();
		main.put("name", getName());
		main.put("id", getId());
		return main;
	}

	public ObjectNode toJsonBase()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.set("displayedName", I18nStringJsonUtil.toJson(getDisplayedName()));
		main.put("revision", getRevision());
		main.put("mandatory", isMandatory());
		main.put("contentType", getContentType().toString());
		main.set("content", I18nStringJsonUtil.toJson(getContent()));
		return main;
	}

	private void fromJson(ObjectNode src)
	{
		setName(src.get("name").asText());
		setId(src.get("id").asLong());
		fromJsonBase(src);
	}

	public void fromJsonBase(ObjectNode main)
	{
		I18nString displayedName = I18nStringJsonUtil.fromJson(main.get("displayedName"));
		setDisplayedName(displayedName);
		setRevision(main.get("revision").asInt());
		setMandatory(main.get("mandatory").asBoolean());
		I18nString content = I18nStringJsonUtil.fromJson(main.get("content"));
		setContentType(PolicyDocumentContentType.valueOf(main.get("contentType").asText()));
		setContent(content);
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public I18nString getDisplayedName()
	{
		return displayedName;
	}

	public void setDisplayedName(I18nString displayedName)
	{
		this.displayedName = displayedName;
	}

	public int getRevision()
	{
		return revision;
	}

	public void setRevision(int revision)
	{
		this.revision = revision;
	}

	public boolean isMandatory()
	{
		return mandatory;
	}

	public void setMandatory(boolean mandatory)
	{
		this.mandatory = mandatory;
	}

	public I18nString getContent()
	{
		return content;
	}

	public void setContent(I18nString content)
	{
		this.content = content;
	}

	public PolicyDocumentContentType getContentType()
	{
		return contentType;
	}

	public void setContentType(PolicyDocumentContentType contentType)
	{
		this.contentType = contentType;
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof StoredPolicyDocument))
			return false;
		StoredPolicyDocument castOther = (StoredPolicyDocument) other;
		return Objects.equals(id, castOther.id) && Objects.equals(name, castOther.name)
				&& Objects.equals(revision, castOther.revision)
				&& Objects.equals(displayedName, castOther.displayedName)
				&& Objects.equals(content, castOther.content)
				&& Objects.equals(contentType, castOther.contentType)
				&& Objects.equals(mandatory, castOther.mandatory);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, revision, displayedName, contentType, content, mandatory);
	}

}
