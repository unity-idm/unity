package pl.edu.icm.unity.engine.api.translation.form;

import java.util.Collections;
import java.util.List;

public class RegistrationActionValidationContext
{
	public final String allowedGroupWithChildren;
	public final List<String> allowedRootGroupAttributes;

	private RegistrationActionValidationContext(Builder builder)
	{
		this.allowedGroupWithChildren = builder.allowedGroupWithChildren;
		this.allowedRootGroupAttributes = builder.allowedRootGroupAttributes;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String allowedGroupWithChildren;
		private List<String> allowedRootGroupAttributes = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withAllowedGroupWithChildren(String allowedGroupWithChildren)
		{
			this.allowedGroupWithChildren = allowedGroupWithChildren;
			return this;
		}

		public Builder withAllowedRootGroupAttributes(List<String> allowedRootGroupAttributes)
		{
			this.allowedRootGroupAttributes = allowedRootGroupAttributes;
			return this;
		}

		public RegistrationActionValidationContext build()
		{
			return new RegistrationActionValidationContext(this);
		}
	}

}
