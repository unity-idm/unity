/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.eform;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.reg.EnquiryFilledTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.NewEnquiryTemplateDef;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeDAOInternal;
import pl.edu.icm.unity.store.impl.groups.GroupDAOInternal;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.objstore.cred.CredentialDBImpl;
import pl.edu.icm.unity.store.objstore.msgtemplate.MessageTemplateDBImpl;
import pl.edu.icm.unity.store.objstore.reg.AttributeTypeChangeListener;
import pl.edu.icm.unity.store.objstore.reg.AttributeTypeRenameListener;
import pl.edu.icm.unity.store.objstore.reg.BaseTemplateChangeListener;
import pl.edu.icm.unity.store.objstore.reg.CredentialChangeListener;
import pl.edu.icm.unity.store.objstore.reg.CredentialRenameListener;
import pl.edu.icm.unity.store.objstore.reg.GroupChangeListener;
import pl.edu.icm.unity.store.objstore.reg.GroupRenameListener;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;

/**
 * Easy access to {@link EnquiryForm} storage.
 * @author K. Benedyczak
 */
@Component
public class EnquiryFormDBImpl extends GenericObjectsDAOImpl<EnquiryForm> implements EnquiryFormDB
{
	@Autowired
	public EnquiryFormDBImpl(EnquiryFormHandler handler, ObjectStoreDAO dbGeneric,
			CredentialDBImpl credDAO, AttributeTypeDAOInternal atDAO, GroupDAOInternal groupDAO,
			MessageTemplateDBImpl msgTemplateDB)
	{
		super(handler, dbGeneric, EnquiryForm.class, "enquiry form");
		credDAO.addRemovalHandler(new CredentialChangeListener(this));
		credDAO.addUpdateHandler(new CredentialRenameListener<>(this));
		
		atDAO.addRemovalHandler(new AttributeTypeChangeListener(this));
		atDAO.addUpdateHandler(new AttributeTypeRenameListener<>(this));
		
		groupDAO.addRemovalHandler(new GroupChangeListener(this));
		groupDAO.addUpdateHandler(new GroupRenameListener<>(this));
		
		MessageTemplateChangeListener mtListener = new MessageTemplateChangeListener();
		msgTemplateDB.addRemovalHandler(mtListener);
		msgTemplateDB.addUpdateHandler(mtListener);
	}
	
	private class MessageTemplateChangeListener extends BaseTemplateChangeListener
	{
		@Override
		public void preRemoveCheck(long removedId, String removedName)
		{
			List<EnquiryForm> forms = getAll();
			for (EnquiryForm form: forms)
			{
				EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
				preRemoveCheck(notCfg, removedName, form.getName());
				if (removedName.equals(notCfg.getEnquiryToFillTemplate()))
					throw new IllegalArgumentException("The message template is used by an "
							+ "enquiry form " + form.getName());
				if (removedName.equals(notCfg.getSubmittedTemplate()))
					throw new IllegalArgumentException("The message template is used by a "
							+ "registration form " + form.getName());
			}
		}

		@Override
		public void preUpdateCheck(long modifiedId, String modifiedName,
				MessageTemplate newValue)
		{
			List<EnquiryForm> forms = getAll();
			for (EnquiryForm form: forms)
			{
				EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
				boolean needUpdate = checkUpdated(notCfg, modifiedName, newValue, form.getName());
				if (modifiedName.equals(notCfg.getEnquiryToFillTemplate()) && 
						!newValue.getConsumer().equals(NewEnquiryTemplateDef.NAME))
				{
					throw new IllegalArgumentException("The message template is used by "
							+ "an enquiry form " + form.getName() + 
							" and the template's type change would render the "
							+ "template incompatible with it");
				}
				if (modifiedName.equals(notCfg.getSubmittedTemplate()) && 
						!newValue.getConsumer().equals(EnquiryFilledTemplateDef.NAME))
				{
					throw new IllegalArgumentException("The message template is used by "
							+ "a registration form " + form.getName() + 
							" and the template's type change would render the "
							+ "template incompatible with it");
				}
				
				if (modifiedName.equals(notCfg.getEnquiryToFillTemplate()))
				{
					notCfg.setEnquiryToFillTemplate(newValue.getName());
					needUpdate = true;
				}
				if (modifiedName.equals(notCfg.getSubmittedTemplate()))
				{
					notCfg.setSubmittedTemplate(newValue.getName());
					needUpdate = true;
				}
				
				if (modifiedName.equals(notCfg.getInvitationTemplate()))
				{
					notCfg.setInvitationTemplate(newValue.getName());
					needUpdate = true;
				}
				
				if (needUpdate)
					update(form);
			}
		}
	}
}
