/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;

/**
 * Used by {@link BaseRequestEditor} to create UI controls. Contains
 * informations that should be set as initial state for a request.
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
		this.identities = Collections.unmodifiableMap(identities != null ? identities : new HashMap<>());
		this.groupSelections = Collections
				.unmodifiableMap(groupSelections != null ? groupSelections : new HashMap<>());
		this.attributes = Collections.unmodifiableMap(attributes != null ? attributes : new HashMap<>());
		this.allowedGroups = Collections
				.unmodifiableMap(allowedGroups != null ? allowedGroups : new HashMap<>());
	}

	public PrefilledSet()
	{
		this(null, null, null, null);
	}

	public boolean isEmpty()
	{
		return identities.isEmpty() &&  groupSelections.isEmpty() && attributes.isEmpty();
	}
	
	public boolean containsValuesOnlyWithMode(PrefilledEntryMode mode)
	{
		return (attributes.values().stream()
				.filter(v -> v != null && !v.getMode().equals(mode) && v.getEntry() != null).count() == 0)
				&& (groupSelections.values().stream()
						.filter(v -> v != null 
								&& !v.getMode().equals(mode)
								&& v.getEntry() != null && !v.getEntry().getSelectedGroups().isEmpty()
								)
						.count() == 0)
				&& (identities.values().stream().filter(
						v -> v != null && !v.getMode().equals(mode) && v.getEntry() != null)
						.count() == 0);
	}
}
