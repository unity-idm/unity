package io.imunity.test.ui.accounts;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.SeleniumTestBase;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;

public abstract class UserAccountTestBase extends SeleniumTestBase
{
	@Autowired
	@Qualifier("insecure")
	protected EntityManagement entities;
	@Autowired
	@Qualifier("insecure")
	protected AttributesManagement attributes;
	@Autowired
	@Qualifier("insecure")
	private EntityCredentialManagement credentials;

	private final List<EntityParam> createdUsers = new ArrayList<>();

	protected final TestUser createUser(String description) throws EngineException
	{
		String suffix = UUID.randomUUID().toString();
		String username = "ui-user-" + suffix;
		String displayName = description + " " + suffix;
		String password = "Disposable!58-Harbor";
		long entityId = entities.addEntity(new IdentityParam("userName", username), "ui-password", EntityState.valid,
				List.of(StringAttribute.of("name", "/", displayName),
						EnumAttribute.of("sys:AuthorizationRole", "/", "Regular User"))).getEntityId();
		EntityParam entity = new EntityParam(entityId);
		trackUser(entity);
		credentials.setEntityCredential(entity, "sys:password", new PasswordToken(password).toJson());
		return new TestUser(entity, username, password, displayName);
	}

	protected final void trackUser(EntityParam entity)
	{
		createdUsers.add(entity);
	}

	@AfterEach
	void removeUsers() throws EngineException
	{
		EngineException failure = null;
		for (EntityParam entity : createdUsers.reversed())
		{
			try
			{
				entities.removeEntity(entity);
			} catch (EngineException e)
			{
				if (failure == null)
					failure = e;
				else
					failure.addSuppressed(e);
			}
		}
		if (failure != null)
			throw failure;
	}

	protected record TestUser(EntityParam entity, String username, String password, String displayName)
	{
	}
}
