/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.credentials;


import com.vaadin.flow.component.Unit;

public class CredentialEditorContext
{
	public static final CredentialEditorContext EMPTY = new CredentialEditorContext(null, null,
			false, null, null, false, false, null, null);
	private final String credentialConfiguration;
	private final String credentialName;
	private final boolean required;
	private final Long entityId;
	private final String extraInformation;
	private final boolean adminMode;
	private final boolean showLabelInline;
	private final Float customWidth;
	private final Unit customWidthUnit;

	CredentialEditorContext(String credentialConfiguration, String credentialName, boolean required,
	                        Long entityId, String extraInformation,
	                        boolean adminMode, boolean showLabelInline, Float customWidth, Unit customWidthUnit)
	{
		this.credentialConfiguration = credentialConfiguration;
		this.credentialName = credentialName;
		this.required = required;
		this.entityId = entityId;
		this.extraInformation = extraInformation;
		this.adminMode = adminMode;
		this.showLabelInline = showLabelInline;
		this.customWidth = customWidth;
		this.customWidthUnit = customWidthUnit;
	}

	public String getCredentialConfiguration()
	{
		return credentialConfiguration;
	}

	public String getCredentialName()
	{
		return credentialName;
	}

	public boolean isRequired()
	{
		return required;
	}

	public Long getEntityId()
	{
		return entityId;
	}

	public String getExtraInformation()
	{
		return extraInformation;
	}

	public boolean isAdminMode()
	{
		return adminMode;
	}

	public boolean isShowLabelInline()
	{
		return showLabelInline;
	}
	
	public boolean isCustomWidth()
	{
		return customWidth != null && customWidthUnit != null;
	}
	
	public Float getCustomWidth()
	{
		return customWidth;
	}

	public Unit getCustomWidthUnit()
	{
		return customWidthUnit;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		private String credentialConfiguration;
		private String credentialName;
		private boolean required;
		private Long entityId;
		private String extraInformation;
		private boolean adminMode;
		private boolean showLabelInline;
		private Float customWidth = null;
		private Unit customWidthUnit = null;

		public Builder withConfiguration(String credentialConfiguration)
		{
			this.credentialConfiguration = credentialConfiguration;
			return this;
		}

		public Builder withCredentialName(String credentialName)
		{
			this.credentialName = credentialName;
			return this;
		}

		public Builder withRequired(boolean required)
		{
			this.required = required;
			return this;
		}

		public Builder withEntityId(Long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public Builder withExtraInformation(String extraInformation)
		{
			this.extraInformation = extraInformation;
			return this;
		}

		public Builder withAdminMode(boolean adminMode)
		{
			this.adminMode = adminMode;
			return this;
		}

		public Builder withShowLabelInline(boolean showLabelInline)
		{
			this.showLabelInline = showLabelInline;
			return this;
		}
		
		public Builder withCustomWidth(float width)
		{
			this.customWidth = width;
			return this;
		}
		
		public Builder withCustomWidthUnit(Unit unit)
		{
			this.customWidthUnit = unit;
			return this;
		}

		public CredentialEditorContext build()
		{
			return new CredentialEditorContext(credentialConfiguration, credentialName, required, entityId, extraInformation,
					adminMode, showLabelInline, customWidth, customWidthUnit);
		}
	}

}
