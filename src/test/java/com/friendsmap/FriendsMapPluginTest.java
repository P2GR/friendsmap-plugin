package com.friendsmap;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FriendsMapPluginTest
{
	// loadBuiltin takes Class<? extends Plugin>..., so the implicit array
	// created at the call site is always an unchecked generic array.
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FriendsMapPlugin.class);
		RuneLite.main(args);
	}
}
