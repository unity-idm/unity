/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.confirmation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.base.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.store.api.generic.ConfirmationConfigurationDB;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeDAOInternal;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;
import pl.edu.icm.unity.store.objstore.msgtemplate.MessageTemplateDBImpl;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Easy to use interface to {@link ConfirmationConfiguration} storage.
 * 
 * @author P. Piernik
 */
@Component
public class ConfirmationConfigurationDBImpl extends GenericObjectsDAOImpl<ConfirmationConfiguration>
	implements ConfirmationConfigurationDB
{
	@Autowired
	public ConfirmationConfigurationDBImpl(ConfirmationConfigurationHandler handler,
			ObjectStoreDAO dbGeneric, MessageTemplateDBImpl msgTmplDao, 
			AttributeTypeDAOInternal attributeTypeDAO)
	{
		super(handler, dbGeneric, ConfirmationConfiguration.class, "confirmation configuration");
		msgTmplDao.addRemovalHandler(this::restrictTemplateRemoval);
		msgTmplDao.addUpdateHandler(this::templateUpdatePreservesConsumer);
		attributeTypeDAO.addRemovalHandler(this::cascadeAttributeTypeRemoval);
		attributeTypeDAO.addUpdateHandler(this::propagateAttributeTypeUpdate);
	}

	private void templateUpdatePreservesConsumer(long modifiedId, String modifiedName, MessageTemplate newValue)
	{
		List<ConfirmationConfiguration> cfgs = getAll();
		for (ConfirmationConfiguration cfg : cfgs)
		{
			if (modifiedName.equals(cfg.getMsgTemplate())
					&& !newValue.getConsumer().equals(
							ConfirmationTemplateDef.NAME))
			{
				throw new IllegalArgumentException(
						"The message template is used by a "
						+ cfg.getNameToConfirm()
						+ " confirmation configuration and the template's "
						+ "type change would render the template incompatible with it");
			}
		}
	}

	private void restrictTemplateRemoval(long removedId, String removedName)
	{
		List<ConfirmationConfiguration> cfgs = getAll();
		for (ConfirmationConfiguration cfg : cfgs)
		{
			if (removedName.equals(cfg.getMsgTemplate()))
				throw new IllegalArgumentException(
						"The message template is used by a "
								+ cfg.getNameToConfirm()
								+ "  confirmation configuration ");
		}
	}

	private void cascadeAttributeTypeRemoval(long removedId, String removedName)
	{
		String configName = ATTRIBUTE_CONFIG_TYPE + removedName; 
		if (exists(configName))
			delete(configName);
	}
	
	private void propagateAttributeTypeUpdate(long modifiedId, String modifiedName, AttributeType newValue)
	{
		String oldConfigName = ATTRIBUTE_CONFIG_TYPE + modifiedName; 
		if (!exists(oldConfigName))
			return;

		if (!modifiedName.equals(newValue.getName()))
		{
			ConfirmationConfiguration toUpdate = get(oldConfigName);
			toUpdate.setNameToConfirm(newValue.getName());
			updateByName(oldConfigName, toUpdate);
		}
	}
}
