/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms;

import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;

/**
 * Used by {@link BaseRequestEditor} to create UI controls. Contains
 * informations about admin preffiled invitation entries.
 * 
 * @author P.Piernik
 *
 */
public class PrefilledSet
{
	public final Map<Integer, PrefilledEntry<IdentityParam>> identities;
	public final Map<Integer, PrefilledEntry<GroupSelection>> groupSelections;
	public final Map<Integer, GroupSelection> allowedGroups;
	public final Map<Integer, PrefilledEntry<Attribute>> attributes;

	public PrefilledSet(Map<Integer, PrefilledEntry<IdentityParam>> identities,
			Map<Integer, PrefilledEntry<GroupSelection>> groupSelections,
			Map<Integer, PrefilledEntry<Attribute>> attributes, Map<Integer, GroupSelection> allowedGroups)
	{
		this.identities = new HashMap<>();

		if (identities != null)
			this.identities.putAll(identities);
		this.groupSelections = new HashMap<>();
		if (groupSelections != null)
			this.groupSelections.putAll(groupSelections);
		this.attributes = new HashMap<>();
		if (attributes != null)
			this.attributes.putAll(attributes);
		this.allowedGroups = new HashMap<>();
		if (allowedGroups != null)
			this.allowedGroups.putAll(allowedGroups);
	}
	
	public PrefilledSet()
	{
		this(null, null, null, null);
	}

}
