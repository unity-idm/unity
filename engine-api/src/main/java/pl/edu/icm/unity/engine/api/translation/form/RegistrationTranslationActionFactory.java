/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.form;

import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;

/**
 * Marker interface of the factories producing registration actions. We have this separated to 
 * limit dependencies coupling in the factories registry
 * @author Krzysztof Benedyczak
 */
public interface RegistrationTranslationActionFactory extends TranslationActionFactory<RegistrationTranslationAction>
{

}
