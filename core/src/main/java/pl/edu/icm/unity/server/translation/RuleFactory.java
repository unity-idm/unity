/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

/**
 * Implementation allows to instantiate rule.
 * @author K. Benedyczak
 * @param <T>
 */
public interface RuleFactory<T extends TranslationAction>
{
	AbstractTranslationRule<T> createRule(T action, TranslationCondition cnd);
}