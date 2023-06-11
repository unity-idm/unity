/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.base.translation.TranslationProfile;

/**
 * Easy access to {@link TranslationProfile} storage. Used for output profiles.
 * 
 * @author K. Benedyczak
 */
public interface OutputTranslationProfileDB extends NamedCRUDDAOWithTS<TranslationProfile>
{
}
