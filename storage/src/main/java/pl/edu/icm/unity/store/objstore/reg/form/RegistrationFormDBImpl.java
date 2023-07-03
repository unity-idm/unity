/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.msg_template.reg.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler.PlannedUpdateEvent;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeDAOInternal;
import pl.edu.icm.unity.store.impl.groups.GroupDAOInternal;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.objstore.cred.CredentialDBImpl;
import pl.edu.icm.unity.store.objstore.credreq.CredentialRequirementDBImpl;
import pl.edu.icm.unity.store.objstore.msgtemplate.MessageTemplateDBImpl;
import pl.edu.icm.unity.store.objstore.reg.AttributeTypeChangeListener;
import pl.edu.icm.unity.store.objstore.reg.AttributeTypeRenameListener;
import pl.edu.icm.unity.store.objstore.reg.BaseTemplateChangeListener;
import pl.edu.icm.unity.store.objstore.reg.CredentialChangeListener;
import pl.edu.icm.unity.store.objstore.reg.CredentialRenameListener;
import pl.edu.icm.unity.store.objstore.reg.GroupChangeListener;
import pl.edu.icm.unity.store.objstore.reg.GroupRenameListener;

/**
 * Easy access to {@link RegistrationForm} storage.
 * @author K. Benedyczak
 */
@Component
class RegistrationFormDBImpl extends GenericObjectsDAOImpl<RegistrationForm> implements RegistrationFormDB
{
	@Autowired
	public RegistrationFormDBImpl(RegistrationFormHandler handler, ObjectStoreDAO dbGeneric,
			CredentialDBImpl credDAO, CredentialRequirementDBImpl credReqDAO,
			AttributeTypeDAOInternal atDAO, GroupDAOInternal groupDAO,
			MessageTemplateDBImpl msgTemplateDB)
	{
		super(handler, dbGeneric, RegistrationForm.class, "registration form");
		credReqDAO.addRemovalHandler(this::restrictCredReqRemoval);
		credReqDAO.addUpdateHandler(this::propagateCredReqRename);
		
		credDAO.addRemovalHandler(new CredentialChangeListener(this));
		credDAO.addUpdateHandler(new CredentialRenameListener<>(this));
		
		atDAO.addRemovalHandler(new AttributeTypeChangeListener(this));
		atDAO.addUpdateHandler(new AttributeTypeRenameListener<>(this));
		
		groupDAO.addRemovalHandler(new GroupChangeListener(this));
		groupDAO.addUpdateHandler(new GroupRenameListener<>(this));
		
		MessageTemplateChangeListener mtListener = new MessageTemplateChangeListener();
		msgTemplateDB.addRemovalHandler(mtListener);
		msgTemplateDB.addUpdateHandler(mtListener);
		
		RegistrationFormChangeListener changeListener = new RegistrationFormChangeListener(groupDAO);
		addRemovalHandler(changeListener);
	}
	
	@Override
	public void deleteWithoutDependencyChecking(String name) {
		super.delete(name, true);
	}
	
	private void restrictCredReqRemoval(long removedId, String removedName)
	{
		List<RegistrationForm> forms = getAll();
		for (RegistrationForm form: forms)
		{
			if (form.getDefaultCredentialRequirement().equals(removedName))
				throw new IllegalArgumentException("The credential requirement "
						+ "is used by a registration form " + form.getName());
		}
	}
	
	private void propagateCredReqRename(PlannedUpdateEvent<CredentialRequirements> update)
	{
		if (update.modifiedName.equals(update.newValue.getName()))
			return;
		List<RegistrationForm> forms = getAll();
		for (RegistrationForm form: forms)
		{
			if (update.modifiedName.equals(form.getDefaultCredentialRequirement()))
			{
				form.setDefaultCredentialRequirement(update.newValue.getName());
				update(form);
			}
		}
	}
	
	private class MessageTemplateChangeListener extends BaseTemplateChangeListener
	{
		@Override
		public void preRemoveCheck(long removedId, String removedName)
		{
			List<RegistrationForm> forms = getAll();
			for (RegistrationForm form: forms)
			{
				RegistrationFormNotifications notCfg = form.getNotificationsConfiguration();
				preRemoveCheck(notCfg, removedName, form.getName());
				if (removedName.equals(notCfg.getInvitationTemplate()))
					throw new IllegalArgumentException("The message template is used "
							+ "by a registration form " + form.getName());
				if (removedName.equals(notCfg.getSubmittedTemplate()))
					throw new IllegalArgumentException("The message template is used "
							+ "by a registration form " + form.getName());
			}
		}

		@Override
		public void preUpdateCheck(PlannedUpdateEvent<MessageTemplate> update)
		{
			List<RegistrationForm> forms = getAll();
			for (RegistrationForm form: forms)
			{
				RegistrationFormNotifications notCfg = form.getNotificationsConfiguration();
				boolean needUpdate = checkUpdated(notCfg, update.modifiedName, update.newValue, form.getName());
				if (update.modifiedName.equals(notCfg.getSubmittedTemplate()) && 
						!update.newValue.getConsumer().equals(SubmitRegistrationTemplateDef.NAME))
				{
					throw new IllegalArgumentException("The message template is used by "
							+ "a registration form " 
							+ form.getName() + " and the template's type change "
							+ "would render the template incompatible with it");
				}
		
				if (update.modifiedName.equals(notCfg.getSubmittedTemplate()))
				{
					notCfg.setSubmittedTemplate(update.newValue.getName());
					needUpdate = true;
				}
				if (needUpdate)
					update(form);
			}
		}
	}
	
	private class RegistrationFormChangeListener implements ReferenceRemovalHandler
	{

		private GroupDAOInternal groupDAO;

		public RegistrationFormChangeListener(GroupDAOInternal groupDAO)
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

				if (config.registrationForm != null && config.registrationForm.equals(removedName))
				{
					if (config.enabled)
					{
						throw new IllegalArgumentException("The registration form is used "
								+ "by a group " + group.getName()
								+ " delegation config");

					} else
					{
						GroupDelegationConfiguration newConfig = new GroupDelegationConfiguration(
								config.enabled, config.enableSubprojects, config.logoUrl, "",
								config.signupEnquiryForm,
								config.membershipUpdateEnquiryForm, config.attributes);
						group.setDelegationConfiguration(newConfig);
						groupDAO.update(group);
					}
				}
			}
		}

	}
}
