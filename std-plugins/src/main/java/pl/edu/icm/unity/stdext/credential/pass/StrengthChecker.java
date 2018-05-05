/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.context.support.MessageSourceResourceBundle;

import com.nulabinc.zxcvbn.Feedback;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Checks strength of the password using zxcvbn derivative library.
 * 
 * @author K. Benedyczak
 */
public class StrengthChecker
{
	private static final int MAX_SCORE_FOR_GUESSING = 14; //10^14 guesses is considered super safe
	private static final int MAX_SCORE = 100;
	
	public static StrengthInfo measure(String password, Locale locale, UnityMessageSource msg)
	{
		Zxcvbn zxcvbn = new Zxcvbn();
		Strength strength = zxcvbn.measure(password);
		
		ResourceBundle msgAdapter = new MessageSourceResourceBundle(msg, locale);
		Feedback feedback = strength.getFeedback();
		Feedback localizedFeedback = feedback.withResourceBundle(msgAdapter);

		return new StrengthInfo(normalizeScore(strength),
				localizedFeedback.getWarning(locale),
				localizedFeedback.getSuggestions(locale));
	}
	
	private static int normalizeScore(Strength strength)
	{
		double guessesLog10 = strength.getGuessesLog10();
		double scoreUnscalled = guessesLog10 > MAX_SCORE_FOR_GUESSING ? 
				MAX_SCORE_FOR_GUESSING : guessesLog10;
		
		return (int) (scoreUnscalled * MAX_SCORE / MAX_SCORE_FOR_GUESSING);
	}
	
	
	public static class StrengthInfo
	{
		public final int score;
		public final String warning;
		public final List<String> suggestions;

		public StrengthInfo(int score, String warning, List<String> suggestions)
		{
			this.score = score;
			this.warning = warning;
			this.suggestions = Collections.unmodifiableList(suggestions);
		}

		@Override
		public String toString()
		{
			return "StrengthInfo [score=" + score + ", warning=" + warning
					+ ", suggestions=" + suggestions + "]";
		}
	}
}
