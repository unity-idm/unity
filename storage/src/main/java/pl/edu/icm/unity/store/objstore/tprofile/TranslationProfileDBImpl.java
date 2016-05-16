/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.tprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.TranslationProfileDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Easy to use interface to {@link TranslationProfile} storage.
 *  
 * @author K. Benedyczak
 */
@Component
public class TranslationProfileDBImpl extends GenericObjectsDAOImpl<TranslationProfile> implements TranslationProfileDB
{

	@Autowired
	public TranslationProfileDBImpl(TranslationProfileHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, TranslationProfile.class, "translation profile");
	}
}
