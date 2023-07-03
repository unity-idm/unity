/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.GroupSelection;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;

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
	
	/**
	 * @return a new object which has union of pre-filled settings of this and argument object. In case of conflicts 
	 * this object's values stay intact.
	 */
	public PrefilledSet mergeWith(PrefilledSet other)
	{
		return new PrefilledSet(mergeIdentities(other.identities), 
				mergeGroups(other.groupSelections), 
				mergeAttributes(other.attributes), 
				mergeAllowedGroups(other.allowedGroups));
	}
	
	private Map<Integer, GroupSelection> mergeAllowedGroups(Map<Integer, GroupSelection> otherAllowedGroups)
	{
		Map<Integer, GroupSelection> ret = new HashMap<>(otherAllowedGroups);
		ret.putAll(allowedGroups);
		return ret;
	}

	private Map<Integer, PrefilledEntry<Attribute>> mergeAttributes(
			Map<Integer, PrefilledEntry<Attribute>> otherAttributes)
	{
		Map<Integer, PrefilledEntry<Attribute>> ret = new HashMap<>(otherAttributes);
		ret.putAll(attributes);
		return ret;
	}

	private Map<Integer, PrefilledEntry<GroupSelection>> mergeGroups(
			Map<Integer, PrefilledEntry<GroupSelection>> otherGroupSelections)
	{
		Map<Integer, PrefilledEntry<GroupSelection>> ret = new HashMap<>(otherGroupSelections);
		ret.putAll(groupSelections);
		return ret;
	}

	private Map<Integer, PrefilledEntry<IdentityParam>> mergeIdentities(
			Map<Integer, PrefilledEntry<IdentityParam>> otherIdentities)
	{
		Map<Integer, PrefilledEntry<IdentityParam>> ret = new HashMap<>(otherIdentities);
		ret.putAll(identities);
		return ret;
	}
}
