/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz.externalScript.input;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.util.URIUtils;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;

public record InputRequest(
		List<String> responseType,
		String clientID,
		String redirectURI,
		List<InputScope> scope,
		String responseMode,
		List<String> resources,
		String requestURI,
		List<String> prompt,
		Map<String, List<String>> customParams,
		List<String> acrValues,
		List<String> uiLocales)
{

	public static InputRequest from(AuthorizationRequest req)
	{
		Builder builder = builder().withClientID(Optional.ofNullable(req.getClientID())
				.map(c -> c.getValue())
				.orElse(null))
				.withCustomParams(req.getCustomParameters())
				.withPrompt(Optional.ofNullable(req.getPrompt())
						.map(p -> p.toStringList())
						.orElse(null))
				.withRedirectURI(Optional.ofNullable(req.getRedirectionURI())
						.map(uri -> uri.toString())
						.orElse(null))
				.withRequestURI(Optional.ofNullable(req.getRequestURI())
						.map(uri -> uri.toString())
						.orElse(null))
				.withResources(Optional.ofNullable(req.getResources())
						.map(r -> URIUtils.toStringList(r))
						.orElse(null))
				.withResponseMode(Optional.ofNullable(req.getResponseMode())
						.map(rm -> rm.toString())
						.orElse(null))
				.withResponseType(Optional.ofNullable(req.getResponseType())
						.map(rt -> rt.stream()
								.map(r -> r.getValue())
								.toList())
						.orElse(null))
				.withScope(Optional.ofNullable(req.getScope())
						.map(scope -> scope.stream()
								.map(s -> new InputScope(s.getValue(), Optional.ofNullable(s.getRequirement())
										.map(r -> r.toString())
										.orElse(null)))
								.toList())
						.orElse(null));
		if (req instanceof AuthenticationRequest areq)
		{
			builder.withAcrValues(Optional.ofNullable(areq.getACRValues())
					.map(acr -> acr.stream()
							.map(a -> a.getValue())
							.toList())
					.orElse(null))
					.withUiLocales(Optional.ofNullable(areq.getUILocales())
							.map(u -> u.stream()
									.map(l -> l.toString())
									.toList())
							.orElse(null));
		}
		return builder.build();

	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<String> responseType;
		private String clientID;
		private String redirectURI;
		private List<InputScope> scope;
		private String responseMode;
		private List<String> resources;
		private String requestURI;
		private List<String> prompt;
		private Map<String, List<String>> customParams;
		private List<String> acrValues;
		private List<String> uiLocales;

		private Builder()
		{
		}

		public Builder withResponseType(List<String> responseType)
		{
			this.responseType = responseType;
			return this;
		}

		public Builder withClientID(String clientID)
		{
			this.clientID = clientID;
			return this;
		}

		public Builder withRedirectURI(String redirectURI)
		{
			this.redirectURI = redirectURI;
			return this;
		}

		public Builder withScope(List<InputScope> scope)
		{
			this.scope = scope;
			return this;
		}

		public Builder withResponseMode(String responseMode)
		{
			this.responseMode = responseMode;
			return this;
		}

		public Builder withResources(List<String> resources)
		{
			this.resources = resources;
			return this;
		}

		public Builder withRequestURI(String requestURI)
		{
			this.requestURI = requestURI;
			return this;
		}

		public Builder withPrompt(List<String> prompt)
		{
			this.prompt = prompt;
			return this;
		}

		public Builder withCustomParams(Map<String, List<String>> customParams)
		{
			this.customParams = customParams;
			return this;
		}

		public Builder withAcrValues(List<String> acrValues)
		{
			this.acrValues = acrValues;
			return this;
		}

		public Builder withUiLocales(List<String> uiLocales)
		{
			this.uiLocales = uiLocales;
			return this;
		}

		public InputRequest build()
		{
			return new InputRequest(responseType, clientID, redirectURI, scope, responseMode, resources, requestURI,
					prompt, customParams, acrValues, uiLocales);
		}
	}

}
