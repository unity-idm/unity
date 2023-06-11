/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker.StrengthInfo;

public class StrengthCheckerTest
{
	@Test
	public void shouldReturnMaxScoreForGoodPassword() throws IOException
	{
		MessageSource msg = mock(MessageSource.class);
		StrengthInfo result = StrengthChecker.measure("horsedonteathorseradishondisk", 10, Locale.ENGLISH, msg);

		assertThat("" + result.scoreNormalized, result.scoreNormalized, is(1.0));
	}

	@Test
	public void shouldReturnLowScoreForBadPassword() throws IOException
	{
		MessageSource msg = mock(MessageSource.class);
		when(msg.getMessage(eq("feedback.repeat.warning.likeABCABCABC"), any(), any())).thenReturn("ok1");
		when(msg.getMessage(eq("feedback.extra.suggestions.addAnotherWord"), any(), any())).thenReturn("o2k");
		when(msg.getMessage(eq("feedback.repeat.suggestions.avoidRepeatedWords"), any(), any()))
				.thenReturn("ok3");
		StrengthInfo result = StrengthChecker.measure("soso", 10, Locale.ENGLISH, msg);

		assertThat("" + result.scoreNormalized, result.scoreNormalized < 0.15, is(true));

	}

	@Test
	public void shouldReturnWarningInSelectedLocale() throws IOException
	{
		MessageSource msg = mock(MessageSource.class);
		when(msg.getMessage(eq("feedback.spatial.suggestions.UseLongerKeyboardPattern"), any(),
				eq(new Locale("pl")))).thenReturn("ok1");
		when(msg.getMessage(eq("feedback.repeat.suggestions.avoidRepeatedWords"), any(), any()))
				.thenReturn("ok2");
		when(msg.getMessage(eq("feedback.extra.suggestions.addAnotherWord"), any(), any())).thenReturn("ok3");
		when(msg.getMessage(eq("feedback.spatial.warning.straightRowsOfKeys"), any(), any())).thenReturn("ok4");
		StrengthInfo result = StrengthChecker.measure("asdfghjkl;'", 10, new Locale("pl"), msg);

		assertThat(result.toString(), result.warning, is("ok4"));
	}
}
