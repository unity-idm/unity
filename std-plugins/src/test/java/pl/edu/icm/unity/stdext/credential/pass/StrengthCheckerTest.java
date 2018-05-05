/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.pass.StrengthChecker.StrengthInfo;

public class StrengthCheckerTest
{
	@Test
	public void shouldReturnMaxScoreForGoodPassword() throws IOException
	{
		UnityMessageSource msg = new UnityMessageSource(mock(UnityServerConfiguration.class), true);
		StrengthInfo result = StrengthChecker.measure("horsedonteathorseradishondisk", Locale.ENGLISH, msg);
		
		assertThat(""+result.score, result.score, is(100));
	}

	@Test
	public void shouldReturnLowScoreForBadPassword() throws IOException
	{
		UnityMessageSource msg = new UnityMessageSource(mock(UnityServerConfiguration.class), true);
		StrengthInfo result = StrengthChecker.measure("soso", Locale.ENGLISH, msg);
		
		assertThat(""+result.score, result.score < 15, is(true));
		
	}
	
	@Test
	public void shouldReturnWarningInSelectedLocale() throws IOException
	{
		UnityMessageSource msg = new UnityMessageSource(mock(UnityServerConfiguration.class), true);
		StrengthInfo result = StrengthChecker.measure("asdfghjkl;'", new Locale("pl"), msg);
		
		assertThat(result.toString(), result.warning, 
				is("Ciągi znaków z klawiatury są łatwe do zgadnięcia"));
	}
}
