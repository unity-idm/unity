/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;

public class TestTargetedPersistentIdentity
{
	@Test
	public void allFieldsAreSet() throws IllegalTypeException, IllegalIdentityValueException
	{
		TargetedPersistentIdentity tested = new TargetedPersistentIdentity();
		
		Identity generated = tested.createNewIdentity("r1", "t1", 123l);
		
		assertThat(generated.getComparableValue()).isNotNull();
		assertThat(generated.getValue()).isNotNull();
		assertThat(generated.getRealm()).isEqualTo("r1");
		assertThat(generated.getTarget()).isEqualTo("t1");
		assertThat(generated.getEntityId()).isEqualTo(123l);
	}
}
