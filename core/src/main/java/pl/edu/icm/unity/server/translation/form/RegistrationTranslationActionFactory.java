/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.server.translation.form;

import pl.edu.icm.unity.server.translation.TranslationActionFactory;

/**
 * Marker interface of the factories producing registration actions. We have this separated to 
 * limit dependencies coupling in the factories registry
 * @author Krzysztof Benedyczak
 */
public interface RegistrationTranslationActionFactory extends TranslationActionFactory
{

}
