/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.util.Map;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.token.Token;

/**
 * Unlike {@link TestTxRunner}, mimics the real {@code SQLTransactionEngine} commit semantics:
 * a write performed against the wrapped {@link MockTokensMan} is only kept if the transactional
 * code returns normally. If it throws, the token store is rolled back to its pre-transaction state -
 * catching bugs where a method persists a change and then throws to signal an expected outcome
 * (which in production silently discards the persisted change, since the real transaction never
 * commits).
 */
public class RollbackOnThrowTxRunner extends TestTxRunner
{
	private final MockTokensMan tokensMan;

	public RollbackOnThrowTxRunner(MockTokensMan tokensMan)
	{
		this.tokensMan = tokensMan;
	}

	@Override
	public <T> T runInTransactionRetThrowing(TxRunnableThrowingRet<T> code) throws EngineException
	{
		Map<String, Token> snapshot = tokensMan.snapshot();
		try
		{
			return code.run();
		} catch (EngineException e)
		{
			tokensMan.restore(snapshot);
			throw e;
		}
	}
}
