/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap;

/**
 * Which players the world map list shows. RuneLite renders these in the config
 * dropdown via {@code Text.titleCase} ("SHOW_ALL" shows as "Show All") and
 * persists them under their enum name.
 */
public enum PlayerListFilter
{
	SHOW_ALL,
	SHOW_CLAN,
	SHOW_FRIENDS,
	SHOW_FRIENDS_CHAT
}
