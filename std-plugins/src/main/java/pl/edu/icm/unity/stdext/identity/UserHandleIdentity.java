/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.UserHandle;

/**
 * User handle generated automatically for FIDO registration and authentication processes.
 *
 * @author R. Ledzinski
 */
@Component
public class UserHandleIdentity extends AbstractStaticIdentityTypeProvider
{
    public static final String ID = "userHandle";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getDefaultDescriptionKey()
    {
        return "UserHandleIdentity.description";
    }

    @Override
    public void validate(String value)
    {
    }

    @Override
    public Identity createNewIdentity(String realm, String target, long entityId)
    {
        throw new IllegalStateException("This identity type doesn't support dynamic identity creation.");
    }

    @Override
    public IdentityParam convertFromString(String stringRepresentation, String remoteIdp,
                                           String translationProfile) throws IllegalIdentityValueException
    {
        return super.convertFromString(stringRepresentation.trim(), remoteIdp, translationProfile);
    }

    @Override
    public String getComparableValue(String from, String realm, String target)
    {
        return from;
    }

    @Override
    public String toPrettyStringNoPrefix(IdentityParam from)
    {
        return from.getValue();
    }


    @Override
    public String getHumanFriendlyDescription(MessageSource msg)
    {
        return msg.getMessage("UserHandleIdentity.description");
    }

    @Override
    public boolean isDynamic()
    {
        return false;
    }

    @Override
    public String getHumanFriendlyName(MessageSource msg)
    {
        return msg.getMessage("UserHandleIdentity.name");
    }
}
