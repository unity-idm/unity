/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import java.util.List;

/**
 * Implemented by all kinds of transactions.
 * @author K. Benedyczak
 */
public interface TransactionState
{
	/**
	 * Manually commit current transaction.
	 */
	void manualCommit();

	/**
	 * Returns list of action that should be performed after successful commit.
	 * @return
	 * 		List of actions
	 */
	List<Runnable> getPostCommitActions();

	/**
	 * Add new action that should be executed after successful commit.
	 * @param action
	 * 		Action that will be executed
	 */
	default void addPostCommitAction(Runnable action)
	{
		getPostCommitActions().add(action);
	}

	/**
	 * Executes all post commit actions that were added to this transaction.
	 */
	default void runPostCommitActions()
	{
		getPostCommitActions().forEach(Runnable::run);
	}


}
