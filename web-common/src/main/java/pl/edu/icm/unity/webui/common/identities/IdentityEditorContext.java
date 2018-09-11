/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities;

/**
 * Contains complete information necessary to build identity editor UI.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class IdentityEditorContext
{
	private final boolean required;
	private final boolean adminMode;
	private final boolean showLabelInline;

	public IdentityEditorContext(boolean required, boolean adminMode, boolean showLabelInline)
	{
		this.required = required;
		this.adminMode = adminMode;
		this.showLabelInline = showLabelInline;
	}

	public boolean isRequired()
	{
		return required;
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
		private boolean required = false;
		private boolean adminMode = false;
		private boolean showLabelInline = false;

		public Builder withRequired(boolean required)
		{
			this.required = required;
			return this;
		}

		public Builder withAdminMode(boolean adminMode)
		{
			this.adminMode = adminMode;
			return this;
		}

		public Builder withLabelInLine(boolean showLabelInline)
		{
			this.showLabelInline = showLabelInline;
			return this;
		}

		public IdentityEditorContext build()
		{
			return new IdentityEditorContext(required, adminMode, showLabelInline);
		}
	}
}
