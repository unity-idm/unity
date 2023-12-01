package pl.edu.icm.unity.engine.api.translation.form;

import java.util.Collections;
import java.util.List;

public class GroupRestrictedFormValidationContext
{
	public final String parentGroup;
	public final List<String> allowedRootGroupAttributes;

	private GroupRestrictedFormValidationContext(Builder builder)
	{
		this.parentGroup = builder.allowedGroupWithChildren;
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

		public GroupRestrictedFormValidationContext build()
		{
			return new GroupRestrictedFormValidationContext(this);
		}
	}

}
