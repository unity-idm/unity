/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.identity;

import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Implementation defined identity type. 
 * 
 * Currently it is stateless, but {@link IdentityType} wraps configuration which in future can be used to 
 * convert also this interface to be stateful and so configurable. 
 * @author K. Benedyczak
 */
public interface IdentityTypeDefinition
{
	/**
	 * @return type id
	 */
	String getId();
	
	/**
	 * @return identity type default description
	 */
	String getDefaultDescriptionKey();
	
	/**
	 * @return if true then the identity type is dynamic, and can not be created manually.
	 * Dynamic identities are created automatically.
	 */
	boolean isDynamic();

	/**
	 * @return if true then the identity type can be set by user.
	 */
	boolean isUserSettable();

	/**
	 * @return false is returned only for dynamic identities, which can not be removed manually. This happens for 
	 * volatile identities, for instance session-scoped. Those identities can be only reset, i.e. all instances
	 * of its type can be removed.
	 */
	boolean isRemovable();
	
	/**
	 * @return if true then identities of this type are targeted, i.e. can have a different value 
	 * for each and every receiver (target). This implies that the authentication realm and target are mandatory
	 * parameters for the methods as e.g. the {@link #getComparableValue(String, String, String)}.
	 */
	boolean isTargeted();
	
	/**
	 * @return if true then identities of this type can be confirmed. 
	 */
	boolean isEmailVerifiable();	

	/**
	 * Checks if the identity is expired. 
	 * @param identity to be checked
	 * @return true if expired, false otherwise 
	 */
	boolean isExpired(Identity identity); 

	
	/**
	 * Checks if the value is valid
	 */
	void validate(String value) throws IllegalIdentityValueException;
	
	/**
	 * Comparable value must be guaranteed to be unique for the type, i.e. if two
	 * values are the same (case sensitive), then the identities represent the same principal.
	 * @param from mandatory raw identity value
	 * @param realm realm value, can be null
	 * @param target target for which the identity is going to be used, can be null
	 * @return comparable value of the string
	 */
	String getComparableValue(String from, String realm, String target);
	
	/**
	 * Similar to {@link #toString()}, but allows for less verbose
	 * and more user-friendly output.
	 */
	String toPrettyString(IdentityParam from);

	/**
	 * Similar to {@link #toPrettyString()}, but doesn't return id type prefix.
	 */
	String toPrettyStringNoPrefix(IdentityParam from);

	/**
	 * @return full String representation
	 */
	String toString(IdentityParam from);
	
	/**
	 * @return string representation which is most useful for end-user. Note that this representation may
	 * even hide the actual value if it is considered cryptic.
	 */
	String toHumanFriendlyString(MessageSource msg, IdentityParam from);

	/**
	 * @return Name of the type which can be presented to end user.
	 */
	String getHumanFriendlyName(MessageSource msg);

	/**
	 * @return Description of the type which can be presented to end user.
	 */
	String getHumanFriendlyDescription(MessageSource msg);
	
	/**
	 * Tries to create a new identity. Can be called only for types which report themself as dynamic.
	 * 
	 * @param realm authentication realm identifier or null if no realm is defined
	 * @param target null or the receiver of the created identity
	 * @param entityId entityId to be set in the returned identity
	 * @return newly generated identity
	 * @throws IllegalStateException if the creation failed: typically when used on a static type.
	 */
	Identity createNewIdentity(String realm, String target, long entityId);
	
	/**
	 * Creates an IdentityParam from a string representation. Typically the method is simplistic (i.e. 
	 * the {@link IdentityParam#getValue()}, remoteIdp and profile is set to the arguments 
	 * and type to {@link #getId()}), but it can also perform additional parsing to set 
	 * for instance the confirmation information. 
	 */
	IdentityParam convertFromString(String stringRepresentation, 
			String remoteIdp, String translationProfile) throws IllegalIdentityValueException;
}

