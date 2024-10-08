/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.tprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.store.api.generic.InputTranslationProfileDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;

/**
 * Easy to use interface to {@link TranslationProfile} storage.
 *  
 * @author K. Benedyczak
 */
@Component
public class InputTranslationProfileDBImpl extends GenericObjectsDAOImpl<TranslationProfile> 
	implements InputTranslationProfileDB
{
	@Autowired
	public InputTranslationProfileDBImpl(InputTranslationProfileHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, TranslationProfile.class, "input translation profile");
	}
}
