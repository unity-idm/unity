/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;

public class FreemarkerUtilsTest
{
	@TempDir
	Path tempDir;

	@Test
	public void shouldReturnTemplateWithUnknownVariable()
	{
		Map<String, Object> vars = new HashMap<>();
		vars.put("var1", "var1Value");
		String output = FreemarkerUtils.processStringTemplate(vars, "${var1} ${var2}");
		assertThat(output).isEqualTo("var1Value ");
	}

	@Test
	public void shouldReturnCustomVariables()
	{
		Map<String, Object> vars = new HashMap<>();
		Map<String, String> custom = new HashMap<>();
		custom.put("var1", "var1Value");
		custom.put("var2", "var2Value");
		vars.put("custom", custom);

		String output = FreemarkerUtils.processStringTemplate(vars, "${custom.var1} ${custom.var2}");
		assertThat(output).isEqualTo("var1Value var2Value");
	}

	@Test
	public void shouldNotExecuteClassInstantiatedWithNewBuiltinWhenProcessing()
	{
		Path marker = tempDir.resolve("processed");

		FreemarkerUtils.processStringTemplate(Map.of(), executeTemplate(marker));

		assertThat(marker).doesNotExist();
	}

	@Test
	public void shouldNotExecuteTemplateWhenValidating()
	{
		Path marker = tempDir.resolve("validated");

		boolean valid = FreemarkerUtils.validateStringTemplate(executeTemplate(marker));

		assertThat(valid).isTrue();
		assertThat(marker).doesNotExist();
	}

	@Test
	public void shouldRejectSyntacticallyInvalidTemplate()
	{
		boolean valid = FreemarkerUtils.validateStringTemplate("Join <#if>");

		assertThat(valid).isFalse();
	}

	@Test
	public void shouldAcceptPlainTextWithVariables()
	{
		boolean valid = FreemarkerUtils.validateStringTemplate("Join ${projectName}");

		assertThat(valid).isTrue();
	}

	@Test
	public void shouldCreateConfigurationWithoutClassInstantiationAndApiAccess()
	{
		Configuration cfg = FreemarkerUtils.createConfiguration(Configuration.VERSION_2_3_34);

		assertThat(cfg.getNewBuiltinClassResolver()).isSameAs(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
		assertThat(cfg.isAPIBuiltinEnabled()).isFalse();
	}

	private static String executeTemplate(Path marker)
	{
		return "<#assign ex=\"freemarker.template.utility.Execute\"?new()>${ex(\"touch " + marker + "\")}";
	}
}
