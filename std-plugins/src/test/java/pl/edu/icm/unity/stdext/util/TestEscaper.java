/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import pl.edu.icm.unity.base.utils.Escaper;

public class TestEscaper
{
	@Test
	public void test()
	{
		String a = Escaper.encode("aa", "bb", "cc");
		String[] sp = Escaper.decode(a);
		assertThat(3).isEqualTo(sp.length);
		assertThat("aa").isEqualTo( sp[0]);
		assertThat("bb").isEqualTo(sp[1]);
		assertThat("cc").isEqualTo(sp[2]);

		a = Escaper.encode("$a$", "\\b", "cc\\$");
		sp = Escaper.decode(a);
		assertThat(3).isEqualTo(sp.length);
		assertThat("$a$").isEqualTo(sp[0]);
		assertThat("\\b").isEqualTo(sp[1]);
		assertThat("cc\\$").isEqualTo(sp[2]);
	}
}
