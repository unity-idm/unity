/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class FreemarkerUtilsTest
{
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
}
