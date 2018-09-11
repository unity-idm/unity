/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

/**
 * Contains complete information necessary to build credential editor UI.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class CredentialEditorContext
{
	private final String credentialConfiguration;
	private final boolean required;
	private final Long entityId;
	private final boolean adminMode;
	private final boolean showLabelInline;

	public CredentialEditorContext(String credentialConfiguration, boolean required, Long entityId, boolean adminMode,
			boolean showLabelInline)
	{
		this.credentialConfiguration = credentialConfiguration;
		this.required = required;
		this.entityId = entityId;
		this.adminMode = adminMode;
		this.showLabelInline = showLabelInline;
	}

	public String getCredentialConfiguration()
	{
		return credentialConfiguration;
	}

	public boolean isRequired()
	{
		return required;
	}

	public Long getEntityId()
	{
		return entityId;
	}

	public boolean isAdminMode()
	{
		return adminMode;
	}

	public boolean isShowLabelInline()
	{
		return showLabelInline;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		private String credentialConfiguration;
		private boolean required;
		private Long entityId;
		private boolean adminMode;
		private boolean showLabelInline;

		public Builder withConfiguration(String credentialConfiguration)
		{
			this.credentialConfiguration = credentialConfiguration;
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

		public CredentialEditorContext build()
		{
			return new CredentialEditorContext(credentialConfiguration, required, entityId, adminMode, showLabelInline);
		}
	}

}
