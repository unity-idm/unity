/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.tprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Easy to use interface to {@link TranslationProfile} storage.
 *  
 * @author K. Benedyczak
 */
@Component
public class TranslationProfileDB extends GenericObjectsDB<TranslationProfile>
{

	@Autowired
	public TranslationProfileDB(TranslationProfileHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, TranslationProfile.class, "translation profile");
	}
}
