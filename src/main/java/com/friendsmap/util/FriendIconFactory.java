/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.FontManager;
import com.friendsmap.FriendsMapConfig;
import com.friendsmap.model.Relation;

/**
 * Shared icon + color factory. Single source of truth for relation visuals.
 * Colors and dot size come from config, so every renderer stays consistent.
 */
public final class FriendIconFactory
{
	private static final int NAME_GAP = 3;

	private FriendIconFactory()
	{
	}

	public static Color colorFor(Relation relation, FriendsMapConfig config)
	{
		switch (relation)
		{
			case CLAN:
				return config.dotColorClan();
			case FRIENDS_CHAT:
				return config.dotColorFriendsChat();
			case FRIEND:
			default:
				return config.dotColorFriend();
		}
	}

	/**
	 * World map icon. Dot only, or dot + small name label to the right.
	 * Faded = semi-transparent, used for offline friends.
	 */
	public static BufferedImage worldMapDot(Relation relation, FriendsMapConfig config, String displayName, boolean faded)
	{
		int size = config.dotSize();
		Color color = colorFor(relation, config);
		if (faded)
		{
			color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 80);
		}

		if (displayName == null || displayName.isEmpty())
		{
			return dotImage(size, color);
		}

		Font font = FontManager.getRunescapeSmallFont();

		BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D probeGraphics = probe.createGraphics();
		probeGraphics.setFont(font);
		FontMetrics metrics = probeGraphics.getFontMetrics();
		probeGraphics.dispose();

		int textWidth = metrics.stringWidth(displayName);
		int height = Math.max(size, metrics.getHeight());
		BufferedImage image = new BufferedImage(size + NAME_GAP + textWidth, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Dot centered vertically, at the left edge.
		int dotY = (height - size) / 2;
		graphics.setColor(color);
		graphics.fillOval(0, dotY, size, size);
		graphics.setColor(Color.BLACK);
		graphics.setStroke(new BasicStroke(1f));
		graphics.drawOval(0, dotY, size - 1, size - 1);

		// Name label right of the dot, vertically centered, with shadow.
		graphics.setFont(font);
		int textX = size + NAME_GAP;
		int baseline = (height - metrics.getHeight()) / 2 + metrics.getAscent();
		graphics.setColor(Color.BLACK);
		graphics.drawString(displayName, textX + 1, baseline + 1);
		graphics.setColor(color);
		graphics.drawString(displayName, textX, baseline);
		graphics.dispose();
		return image;
	}

	private static BufferedImage dotImage(int size, Color color)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(color);
		graphics.fillOval(0, 0, size, size);
		graphics.setColor(Color.BLACK);
		graphics.setStroke(new BasicStroke(1f));
		graphics.drawOval(0, 0, size - 1, size - 1);
		graphics.dispose();
		return image;
	}
}
