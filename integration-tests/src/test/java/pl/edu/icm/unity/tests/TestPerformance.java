package pl.edu.icm.unity.tests;

import org.junit.Before;
import org.junit.Test;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestPerformance extends DBIntegrationTestBase
{

	private long startT;

	private void startTimer()
	{
		startT = System.currentTimeMillis();
	}

	private void stopTimer(int ops, String label)
	{
		long endT = System.currentTimeMillis();
		long periodMs = endT - startT;
		double periodS = periodMs / 1000.0;
		double opsPerS = (ops * 1000 / periodMs);
		System.out.println(label + " performed " + ops + " in " + periodS + "s, " + opsPerS
				+ " ops/s");
	}

	@Before
	public void setup() throws Exception
	{
		setupPasswordAuthn();
	}

	private void addUsers(int size) throws EngineException
	{
		for (int i = 0; i < size; i++)
		{
			Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID,
					"user" + i), "cr-pass", EntityState.valid, false);

			idsMan.setEntityCredential(new EntityParam(added1), "credential1",
					new PasswordToken("PassWord8743#%$^&*").toJson());
		}

	}

	@Test
	public void testUsers() throws EngineException
	{
		int N = 1000;
		startTimer();
		addUsers(N);
		stopTimer(N, "Add user and set passw credential");
	}

	@Test
	public void testGroups() throws EngineException
	{
		int N = 100;

		startTimer();
		for (int i = 0; i < N; i++)
		{

			Group g = new Group("G1_" + i);
			groupsMan.addGroup(g);
			for (int j = 0; j < N; j++)
			{
				Group g2 = new Group(g, "G2_" + j);
				groupsMan.addGroup(g2);
				for (int k = 0; k < N; k++)
				{
					Group g3 = new Group(g2, "G3_" + k);
					groupsMan.addGroup(g3);
				}
			}
		}
		stopTimer(N * N * N, "Group add");

		addUsers(N);

		startTimer();

		for (int i = 0; i < N; i++)
		{
			groupsMan.addMemberFromParent("G1_" + i, new EntityParam(new IdentityParam(
					UsernameIdentity.ID, "user" + i)));
		}

		for (int i = 0; i < N / 2; i++)
		{
			groupsMan.addMemberFromParent("G1_" + i + "/G2_" + i, new EntityParam(
					new IdentityParam(UsernameIdentity.ID, "user" + i)));
		}

		for (int i = 0; i < N / 4; i++)
		{
			groupsMan.addMemberFromParent("G1_" + i + "/G2_" + i + "/G3_" + i,
					new EntityParam(new IdentityParam(UsernameIdentity.ID,
							"user" + i)));
		}
		stopTimer(N + N / 2 + N / 4, "Add to group");

	}

}
