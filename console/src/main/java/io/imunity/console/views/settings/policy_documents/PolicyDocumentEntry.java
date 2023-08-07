/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.policy_documents;

import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;

import java.util.Locale;
import java.util.Map;

public class PolicyDocumentEntry
{
	Long id;
	String name;
	Map<Locale, String> displayedName;
	boolean mandatory;
	PolicyDocumentContentType contentType;
	Map<Locale, String> content;
	int revision;

	PolicyDocumentEntry()
	{
		displayedName = Map.of();
		content = Map.of();
		contentType = PolicyDocumentContentType.EMBEDDED;
		revision = 1;
	}
	PolicyDocumentEntry(PolicyDocumentWithRevision documentWithRevision)
	{
		this.id = documentWithRevision.id;
		this.name = documentWithRevision.name;
		this.displayedName = documentWithRevision.displayedName.getLocalizedMap();
		this.mandatory = documentWithRevision.mandatory;
		this.contentType = documentWithRevision.contentType;
		this.content = documentWithRevision.content.getLocalizedMap();
		this.revision = documentWithRevision.revision;
	}

	Long getId()
	{
		return id;
	}

	void setId(Long id)
	{
		this.id = id;
	}

	String getName()
	{
		return name;
	}

	void setName(String name)
	{
		this.name = name;
	}

	Map<Locale, String> getDisplayedName()
	{
		return displayedName;
	}

	void setDisplayedName(Map<Locale, String> displayedName)
	{
		this.displayedName = displayedName;
	}

	boolean isMandatory()
	{
		return mandatory;
	}

	void setMandatory(boolean mandatory)
	{
		this.mandatory = mandatory;
	}

	PolicyDocumentContentType getContentType()
	{
		return contentType;
	}

	void setContentType(PolicyDocumentContentType contentType)
	{
		this.contentType = contentType;
	}

	Map<Locale, String> getContent()
	{
		return content;
	}

	void setContent(Map<Locale, String> content)
	{
		this.content = content;
	}

	int getRevision()
	{
		return revision;
	}

	void setRevision(int revision)
	{
		this.revision = revision;
	}

	boolean anyFieldContains(String searched)
	{
		String textLower = searched.toLowerCase();

		if (name != null && name.toLowerCase().contains(textLower))
			return true;

		if (String.valueOf(revision).toLowerCase().contains(textLower))
			return true;

		return contentType != null && contentType.toString().toLowerCase().contains(textLower);
	}

}
