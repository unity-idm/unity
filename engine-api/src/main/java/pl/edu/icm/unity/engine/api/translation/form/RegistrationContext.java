/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.translation.form;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.GroupSelection;

public class RegistrationContext
{
	public final List<GroupSelection> groupSelections;
	
	
	public RegistrationContext(BaseRegistrationInput response)
	{
		this.groupSelections = Collections.unmodifiableList(response.getGroupSelections());
	}


	@Override
	public int hashCode()
	{
		return Objects.hash(groupSelections);
	}


	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegistrationContext other = (RegistrationContext) obj;
		return Objects.equals(groupSelections, other.groupSelections);
	}	
}
