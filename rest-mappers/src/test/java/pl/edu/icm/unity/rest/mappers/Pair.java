/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers;

public class Pair<L, R>
{
	public L left;
	public R right;

	public Pair(L left, R right)
	{
		this.left = left;
		this.right = right;
	}

	public static <L, R> Pair<L, R> of(final L left, final R right)
	{
		return new Pair<L, R>(left, right);
	}
}
