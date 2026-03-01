/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz.externalScript;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.Scope;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.utils.Log;

import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.console.AuthorizationScriptBean;
import pl.edu.icm.unity.oauth.as.webauthz.externalScript.ExternalAuthorizationScriptResponse.Status;

@Component
public class ExternalAuthorizationScriptRunner
{
	private static final long MAX_SCRIPT_OUTPUT_BYTES = 1024L * 1024L; // 1 MB

	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ExternalAuthorizationScriptRunner.class);
	private final ObjectMapper mapper = Constants.MAPPER;
	private final IdentityTypesRegistry identityTypesRegistry;
	private final URIAccessService uriAccessService;

	@Autowired
	public ExternalAuthorizationScriptRunner(IdentityTypesRegistry identityTypesRegistry, URIAccessService uriAccessService)
	{
		this.identityTypesRegistry = identityTypesRegistry;
		this.uriAccessService = uriAccessService;
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
			assertAccessToScript(path);
			
			log.debug("Running external authorization script: {}", path);
			ProcessBuilder pb = new ProcessBuilder(path);
			pb.redirectErrorStream(false);

			Process process = pb.start();

			ExecutorService executor = Executors.newFixedThreadPool(2);

			Future<?> stderrTask = executor.submit(() ->
			        readErrorStream(process, path));

			writeInput(process, input);
			ExternalAuthorizationScriptResponse response = readOutput(process, path);

			process.waitFor();
			stderrTask.get();
			executor.shutdown();
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
		BoundedInputStream bounded = BoundedInputStream.builder()
				.setInputStream(process.getInputStream())
				.setMaxCount(MAX_SCRIPT_OUTPUT_BYTES)
				.setPropagateClose(false)
				.get();

		try (bounded)
		{
			JsonNode resp = mapper.readValue(bounded, JsonNode.class);
			if (bounded.getCount() >= MAX_SCRIPT_OUTPUT_BYTES)
				throw new IOException("External authorization script stdout exceeded "
						+ MAX_SCRIPT_OUTPUT_BYTES + " bytes limit");

			logPretty("Received JSON response from external authorization script " + path, resp);

			return mapper.treeToValue(resp, ExternalAuthorizationScriptResponse.class);
		} catch (JsonProcessingException e)
		{
			if (bounded.getCount() >= MAX_SCRIPT_OUTPUT_BYTES)
				throw new IOException("External authorization script stdout exceeded "
						+ MAX_SCRIPT_OUTPUT_BYTES + " bytes limit");
			throw new IOException("Invalid JSON response from external authorization script " + path, e);
		}
	}

	private void readErrorStream(Process process, String path)
	{
		try (InputStream errorStream = process.getErrorStream())
		{
			BoundedInputStream bounded = BoundedInputStream.builder()
					.setInputStream(errorStream)
					.setMaxCount(MAX_SCRIPT_OUTPUT_BYTES)
					.setPropagateClose(false)
					.get();
			try (bounded; BufferedReader reader = new BufferedReader(new InputStreamReader(bounded)))
			{
				String error = reader.lines().collect(Collectors.joining("\n"));

				if (bounded.getCount() >= MAX_SCRIPT_OUTPUT_BYTES)
				{
					log.error("External authorization script stderr exceeded {} bytes limit",
							MAX_SCRIPT_OUTPUT_BYTES);
					return;
				}

				if (!error.isBlank())
				{
					log.error("[External authorization script {} STDERR]:\n{}", path, error);
				}
			}

		} catch (IOException e)
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
	
	private void assertAccessToScript(String path) throws IOException
	{
		Path child;
		try
		{
			child = Paths.get(new File(path).getAbsolutePath())
					.toRealPath();
		} catch (IOException e)
		{
			throw new IOException("Script " + path + " does not exists");
		}
		
		uriAccessService.assertAccessToFile(child);	
	}
}
