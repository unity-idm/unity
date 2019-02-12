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
import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
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
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;

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
		
		EnquiryFormChangeListener changeListener = new EnquiryFormChangeListener(groupDAO);
		addRemovalHandler(changeListener);
		addUpdateHandler(changeListener);
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
				
				if (needUpdate)
					update(form);
			}
		}
	}
	
	private class EnquiryFormChangeListener implements ReferenceRemovalHandler, ReferenceUpdateHandler<EnquiryForm>
	{

		private GroupDAOInternal groupDAO;

		public EnquiryFormChangeListener(GroupDAOInternal groupDAO)
		{
			this.groupDAO = groupDAO;
		}

		@Override
		public void preRemoveCheck(long removedId, String removedName)
		{
			List<Group> all = groupDAO.getAll();
			for (Group group : all)
			{
				GroupDelegationConfiguration config = group.getDelegationConfiguration();
				if (config.enabled)
				{
					assertIfGroupDelConfigContainForm(config, group.getName(), removedName);
				}else
				{
					clearGroupDelConfigEnquiryFormIfNeeded(group, config, removedName);
				}
			}
		}
		
		private void clearGroupDelConfigEnquiryFormIfNeeded(Group group, GroupDelegationConfiguration config,
				String removedName)
		{

			boolean needSignUpFormUpdate = false;
			boolean needMembershipFormUpdate = false;

			if (config.signupEnquiryForm != null && config.signupEnquiryForm.equals(removedName))
			{
				needSignUpFormUpdate = true;

			}

			if (config.membershipUpdateEnquiryForm != null
					&& config.membershipUpdateEnquiryForm.equals(removedName))
			{
				needMembershipFormUpdate = true;
			}

			if (needMembershipFormUpdate || needSignUpFormUpdate)
			{
				GroupDelegationConfiguration newConfig = new GroupDelegationConfiguration(
						config.enabled, config.logoUrl, config.registrationForm,
						needSignUpFormUpdate ? "" : config.signupEnquiryForm,
						needMembershipFormUpdate ? "" : config.membershipUpdateEnquiryForm,
						config.attributes);
				group.setDelegationConfiguration(newConfig);
				groupDAO.update(group);
			}

		}

		private void assertIfGroupDelConfigContainForm(GroupDelegationConfiguration config, String groupName, String removedName)
		{

			if (config.signupEnquiryForm != null && config.signupEnquiryForm.equals(removedName))
			{
				
				throw new IllegalArgumentException("The enquiry form is used " + "by a group "
						+ groupName + " delegation config");
				
			}

			if (config.membershipUpdateEnquiryForm != null && config.membershipUpdateEnquiryForm.equals(removedName))
			{
				throw new IllegalArgumentException("The enquiry form is used " + "by a group "
						+ groupName + " delegation config");
			}	
		}
		
		@Override
		public void preUpdateCheck(long modifiedId, String modifiedName, EnquiryForm newValue)
		{
			List<Group> all = groupDAO.getAll();
			for (Group group : all)
			{
				boolean needUpdate = false;
				GroupDelegationConfiguration config = group.getDelegationConfiguration();

				if (config.membershipUpdateEnquiryForm != null
						&& config.membershipUpdateEnquiryForm.equals(modifiedName)
						&& !newValue.getType().equals(EnquiryType.STICKY))
				{
					GroupDelegationConfiguration newConfig = new GroupDelegationConfiguration(
							config.enabled, config.logoUrl, config.registrationForm,
							config.signupEnquiryForm, "", config.attributes);
					group.setDelegationConfiguration(newConfig);
					needUpdate = true;
				}

				if (needUpdate)
					groupDAO.update(group);
			}
		}
	}
}
