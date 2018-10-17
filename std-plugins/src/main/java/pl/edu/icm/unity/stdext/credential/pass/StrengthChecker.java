/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
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

import edu.vt.middleware.password.CharacterRule;
import edu.vt.middleware.password.Password;
import edu.vt.middleware.password.PasswordData;
import edu.vt.middleware.password.Rule;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Checks strength of the password using zxcvbn derivative library and provide other simple 
 * quality measurements of passwords. The returned result is normalized to 0-1 range, 
 * 1 (max score) corresponds to currently very hard not normalized value of 14 or minimum acceptable 
 * value whichever is greater.
 * 
 * @author K. Benedyczak
 */
public class StrengthChecker
{
	 //10^14 guesses is considered super safe
	private static final int DEFAULT_PERFECT_SCORE = 14;
	
	public static StrengthInfo measure(String password, int minAcceptable, UnityMessageSource msg)
	{
		return measure(password, minAcceptable, msg.getLocale(), msg);
	}
	
	static StrengthInfo measure(String password, int minAcceptable, 
			Locale locale, UnityMessageSource msg)
	{
		Zxcvbn zxcvbn = new Zxcvbn();
		Strength strength = zxcvbn.measure(password);
		
		ResourceBundle msgAdapter = new MessageSourceResourceBundle(msg, locale);
		Feedback feedback = strength.getFeedback();
		Feedback localizedFeedback = feedback.withResourceBundle(msgAdapter);

		return new StrengthInfo(normalizeScore(strength, minAcceptable),
				(int)strength.getGuessesLog10(),
				localizedFeedback.getWarning(locale),
				localizedFeedback.getSuggestions(locale));
	}
	
	public static int getCharacterClasses(String password)
	{
		List<CharacterRule> charClassesRules = PasswordVerificator.getCharacteristicsRules();
		PasswordData passwordData = new PasswordData(new Password(password));
		int ret = 0;
		for (CharacterRule rule: charClassesRules)
			if (rule.validate(passwordData).isValid())
				ret++;
		return ret;
	}

	public static boolean hasNoTrivialSequences(String password)
	{
		List<Rule> charClassesRules = PasswordVerificator.getSequencesRules();
		PasswordData passwordData = new PasswordData(new Password(password));
		for (Rule rule: charClassesRules)
			if (!rule.validate(passwordData).isValid())
				return false;
		return true;
	}
	
	private static double normalizeScore(Strength strength, int minAllowed)
	{
		int perfectScore = DEFAULT_PERFECT_SCORE > minAllowed ? 
				DEFAULT_PERFECT_SCORE : minAllowed;
		double guessesLog10 = strength.getGuessesLog10();
		double scoreBounded = guessesLog10 > perfectScore ? 
				perfectScore : guessesLog10;
		
		return scoreBounded / perfectScore;
	}
	
	
	public static class StrengthInfo
	{
		public final double scoreNormalized;
		public final int score;
		public final String warning;
		public final List<String> suggestions;

		public StrengthInfo(double scoreNormalized, int score, String warning, List<String> suggestions)
		{
			this.scoreNormalized = scoreNormalized;
			this.score = score;
			this.warning = warning;
			this.suggestions = Collections.unmodifiableList(suggestions);
		}

		@Override
		public String toString()
		{
			return "StrengthInfo [score=" + scoreNormalized + ", warning=" + warning
					+ ", suggestions=" + suggestions + "]";
		}
	}
}
