/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import pl.edu.icm.unity.base.msg_template.MessageTemplate;

/**
 * Easy access to {@link MessageTemplate} storage.
 * 
 * @author K. Benedyczak
 */
public interface MessageTemplateDB extends NamedCRUDDAOWithTS<MessageTemplate>
{
}
