/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;

/**
 * Configures which remote signUp methods should be shown in grid. 
 * 
 * @author P.Piernik
 *
 */
public class ExternalSignupGridSpec extends ExternalSignupSpec
{
	private AuthnGridSettings gridSettings;

	public ExternalSignupGridSpec(List<AuthenticationOptionKey> specs, AuthnGridSettings gridSettings)
	{
		super(specs);
		this.gridSettings = gridSettings;
	}

	ExternalSignupGridSpec()
	{
	}

	public AuthnGridSettings getGridSettings()
	{
		return gridSettings;
	}

	public void setGridSettings(AuthnGridSettings gridSettings)
	{
		this.gridSettings = gridSettings;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof ExternalSignupGridSpec))
			return false;
		if (!super.equals(other))
			return false;

		ExternalSignupGridSpec castOther = (ExternalSignupGridSpec) other;
		return Objects.equals(getGridSettings(), castOther.getGridSettings());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), getGridSettings());
	}

	public static class AuthnGridSettings
	{
		public static final int DEFAULT_HEIGHT = 8;
		
		public final boolean searchable;
		public final int height;

		public AuthnGridSettings()
		{
			height = DEFAULT_HEIGHT;
			searchable = true;
		}
		
		@JsonCreator
		public AuthnGridSettings(@JsonProperty("searchable") boolean searchable, @JsonProperty("height") int height)
		{
			this.searchable = searchable;
			this.height = height;

		}

		@Override
		public int hashCode()
		{
			return Objects.hash(searchable, height);
		}

		@Override
		public boolean equals(final Object other)
		{
			if (!(other instanceof AuthnGridSettings))
				return false;

			AuthnGridSettings castOther = (AuthnGridSettings) other;
			return Objects.equals(searchable, castOther.searchable)
					&& Objects.equals(height, castOther.height);
		}

	}

}
