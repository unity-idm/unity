/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.enq.form;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.types.registration.EnquiryForm;

/**
 * Easy access to {@link EnquiryForm} storage.
 * @author K. Benedyczak
 */
@Component
public class EnquiryFormDBImpl extends GenericObjectsDAOImpl<EnquiryForm> implements EnquiryFormDB
{
	@Autowired
	public EnquiryFormDBImpl(EnquiryFormHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, EnquiryForm.class, "enquiry form");
	}
}
