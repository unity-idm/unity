/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.store.ReferenceAwareDAO;
import pl.edu.icm.unity.store.api.GroupDAO;

/**
 * Extends {@link GroupDAO} with {@link ReferenceAwareDAO} methods, which shall be visible only internally
 * in this module.
 * @author K. Benedyczak
 */
public interface GroupDAOInternal extends GroupDAO, ReferenceAwareDAO<Group>
{
}
