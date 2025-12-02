/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz.externalScript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.Scope;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.console.AuthorizationScriptBean;
import pl.edu.icm.unity.oauth.as.webauthz.externalScript.ExternalAuthorizationScriptResponse.Status;
import pl.edu.icm.unity.oauth.as.webauthz.externalScript.input.ExternalAuthorizationScriptInput;
import pl.edu.icm.unity.oauth.as.webauthz.externalScript.input.InputAttribute;
import pl.edu.icm.unity.oauth.as.webauthz.externalScript.input.InputIdentity;
import pl.edu.icm.unity.oauth.as.webauthz.externalScript.input.InputRequest;
@Component
public class ExternalAuthorizationScriptRunner
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ExternalAuthorizationScriptRunner.class);
	private final ObjectMapper mapper = Constants.MAPPER;
	private final IdentityTypesRegistry identityTypesRegistry;

	@Autowired
	public ExternalAuthorizationScriptRunner(IdentityTypesRegistry identityTypesRegistry)
	{
		this.identityTypesRegistry = identityTypesRegistry;
	}

	public ExternalAuthorizationScriptResponse runConfiguredExternalAuthnScript(
			OAuthAuthzContext ctx,
			TranslationResult translationResult,
			OAuthASProperties config)
	{
		List<AuthorizationScriptBean> scripts = getScriptsFromConfig(config);
		if (scripts.isEmpty())
			 return ExternalAuthorizationScriptResponse.builder()
					.withStatus(Status.PROCEED)
					.build();

		Scope scope = ctx.getRequest().getScope();

		ExternalAuthorizationScriptInput input = ExternalAuthorizationScriptInput.builder()
				.withAttributes(translationResult.getAttributes().stream()
						.map(a -> InputAttribute.fromAttribute(a.getAttribute()))
						.toList())
				.withIdentities(translationResult.getIdentities().stream()
						.map(id -> InputIdentity.fromIdentity(id, identityTypesRegistry))
						.toList())
				.withRequest(InputRequest.fromAuthorizationRequest(ctx.getRequest()))
				.build();

		logPretty("External authorization script input", input);

		List<ExternalAuthorizationScriptResponse> responses = new ArrayList<>();

		for (AuthorizationScriptBean script : scripts)
		{
			if (scope != null && scriptMatches(scope.toStringList(), script))
			{
				ExternalAuthorizationScriptResponse resp = runSingleScript(input, script.getPath());

				if (resp.status() == Status.DENY)
					return resp;

				responses.add(resp);
			}
		}

		return ExternalAuthorizationScriptResponse.builder()
				.withStatus(Status.PROCEED)
				.withClaims(responses.stream()
						.flatMap(r -> r.claims().stream())
						.toList())
				.build();
	}

	private ExternalAuthorizationScriptResponse runSingleScript(ExternalAuthorizationScriptInput input, String path)
	{
		try
		{
			log.debug("Running external authorization script: {}", path);
			ProcessBuilder pb = new ProcessBuilder(path);
			pb.redirectErrorStream(false);

			Process process = pb.start();

			Thread stderrThread = new Thread(() -> readErrorStream(process, path));
			stderrThread.start();

			writeInput(process, input);
			ExternalAuthorizationScriptResponse response = readOutput(process, path);

			process.waitFor();
			stderrThread.join();

			return response;

		} catch (Exception e)
		{
			log.error("External authorization script error", e);
			return ExternalAuthorizationScriptResponse.builder()
					.withStatus(Status.DENY)
					.build();
		}
	}

	private void writeInput(Process process, ExternalAuthorizationScriptInput input)
	{
		try (OutputStream os = process.getOutputStream())
		{
			logPretty("External authorization script input", input);
			mapper.writeValue(os, input);
			os.flush();
		} catch (Exception e)
		{
			log.trace("External authorization script running without input data processing", e);
		}
	}

	private ExternalAuthorizationScriptResponse readOutput(Process process, String path) throws IOException
	{
		try (InputStream is = process.getInputStream())
		{
			ExternalAuthorizationScriptResponse resp = mapper.readValue(is, ExternalAuthorizationScriptResponse.class);
			logPretty("Received JSON response from external authorization script " + path, resp);
			return resp;
		}
	}

	private void readErrorStream(Process process, String path)
	{
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream())))
		{
			String line;
			while ((line = reader.readLine()) != null)
				log.error("[External authorization script {} STDERR] {}", path, line);
		}
		catch (IOException e)
		{
			log.error("Cannot read error stream of external authorization script {}", path, e);
		}
	}

	private List<AuthorizationScriptBean> getScriptsFromConfig(OAuthASProperties config)
	{
		return config.getStructuredListKeys(OAuthASProperties.AUTHORIZATION_SCRIPTS).stream()
				.map(key -> new AuthorizationScriptBean(
						config.getValue(key + OAuthASProperties.AUTHORIZATION_SCRIPT_TRIGGERING_SCOPE),
						config.getValue(key + OAuthASProperties.AUTHORIZATION_SCRIPT_PATH)))
				.toList();
	}

	private boolean scriptMatches(List<String> requestedScopes, AuthorizationScriptBean script)
	{
		try
		{
			for (String requested : requestedScopes)
			{
				if (Pattern.matches(script.getScope(), requested))
				{
					log.debug("Matched scope: '{}' with authorization script definition: scope='{}', path= '{}'",
							requested, script.getScope(), script.getPath());
					return true;
				}
			}
			return false;
		} catch (PatternSyntaxException e)
		{
			log.error("Invalid scope pattern: {}", script, e);
			return false;
		}
	}

	private void logPretty(String message, Object obj)
	{
		try
		{
			log.debug("{}: {}", message, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj));
		}
		catch (JsonProcessingException e)
		{
			log.error("Cannot print object: {}", message, e);
		}
	}
}
