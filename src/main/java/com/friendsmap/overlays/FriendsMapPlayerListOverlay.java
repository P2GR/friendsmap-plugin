/*
 * Copyright (c) 2026, P2GR
 * All rights reserved.
 */
package com.friendsmap.overlays;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import com.friendsmap.FriendsMapConfig;
import com.friendsmap.FriendsMapPlugin;
import com.friendsmap.PlayerListFilter;
import com.friendsmap.model.FriendLocation;
import com.friendsmap.model.Relation;
import com.friendsmap.services.AreaLookup;
import com.friendsmap.util.FriendIconFactory;

/**
 * Scrollable list of the currently visible players, pinned to the top-left
 * corner of the world map. Each row reads {@code Playername - W366}, where the
 * world is the one the player was in at that moment (offline holds read
 * {@code offline}). Players on the local world are listed first; both groups
 * are sorted alphabetically. The list scrolls with the mouse wheel while the
 * cursor is over it. Clicking a row focuses the world map on that player.
 *
 * <p>The Player list filter config picks which relations are listed; Show All
 * groups them under Friends, Clan and Friends Chat headers and hides empty
 * sections.</p>
 *
 * <p>Renders like RuneLite's own world map overlay: {@code DYNAMIC} position on
 * the {@code MANUAL} layer, drawn after the world map interface, in absolute
 * canvas coordinates.</p>
 */
public class FriendsMapPlayerListOverlay extends Overlay implements MouseListener, MouseWheelListener
{
	private static final int MARGIN = 8;
	private static final int SCROLLBAR_WIDTH = 4;
	private static final int SCROLLBAR_INSET = 2;
	private static final int WIDTH_PADDING = 12;
	private static final int ROWS_PER_WHEEL_NOTCH = 3;

	private final Client client;
	private final FriendsMapConfig config;
	private final FriendsMapPlugin plugin;
	private final AreaLookup areaLookup;

	private final PanelComponent panelComponent = new PanelComponent();

	/** Index of the first listed player. Written by render and the wheel listener. */
	private volatile int scrollRows;
	private volatile int maxScrollRows;

	/** Absolute canvas bounds of the drawn panel; used to hit-test wheel events. */
	private volatile Rectangle listBounds = new Rectangle();

	/** Row rectangles and focus targets of the last frame, for click handling. */
	private volatile List<RowHit> rowHits = Collections.emptyList();

	@Inject
	public FriendsMapPlayerListOverlay(Client client, FriendsMapConfig config, FriendsMapPlugin plugin, AreaLookup areaLookup)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		this.areaLookup = areaLookup;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_HIGHEST);
		setLayer(OverlayLayer.MANUAL);
		drawAfterInterface(InterfaceID.WORLDMAP);
		setMovable(false);
		setSnappable(false);
		setDragTargetable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Rectangle mapBounds = worldMapBounds();
		if (!config.showPlayerList() || mapBounds == null)
		{
			resetHitTargets();
			return null;
		}

		PlayerListFilter filter = config.playerListFilter();
		List<DisplayRow> lines = buildLines(sortedPlayers(), filter);
		if (lines.isEmpty())
		{
			resetHitTargets();
			return null;
		}

		int visibleRows = Math.max(1, config.playerListRows());
		int maxScroll = Math.max(0, lines.size() - visibleRows);
		int scroll = Math.min(scrollRows, maxScroll);
		scrollRows = scroll;
		maxScrollRows = maxScroll;

		Font rowFont = FontManager.getRunescapeSmallFont().deriveFont((float) config.playerListFontSize());
		FontMetrics rowMetrics = graphics.getFontMetrics(rowFont);

		panelComponent.getChildren().clear();
		if (filter != PlayerListFilter.SHOW_ALL)
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Players (" + lines.size() + ")")
				.build());
		}

		List<LineComponent> rowComponents = new ArrayList<>();
		List<FriendLocation> rowFriends = new ArrayList<>();
		int textWidth = 0;
		int end = Math.min(lines.size(), scroll + visibleRows);
		for (int i = scroll; i < end; i++)
		{
			DisplayRow line = lines.get(i);
			if (line.header != null)
			{
				panelComponent.getChildren().add(TitleComponent.builder()
					.text(line.header)
					.build());
				continue;
			}

			FriendLocation friend = line.friend;
			String area = config.showAreaNames() ? areaLookup.nameFor(friend.getLocation()) : null;
			String label = friend.getName() + " - " + friend.getWorldLabel()
				+ (area == null ? "" : " - " + area);
			textWidth = Math.max(textWidth, rowMetrics.stringWidth(label));
			LineComponent row = LineComponent.builder()
				.left(label)
				.leftColor(FriendIconFactory.colorFor(friend.getRelation(), config))
				.leftFont(rowFont)
				.rightFont(rowFont)
				.build();
			rowComponents.add(row);
			rowFriends.add(friend);
			panelComponent.getChildren().add(row);
		}

		// Size the panel to the widest row so long names never wrap.
		panelComponent.setPreferredSize(new Dimension(textWidth + WIDTH_PADDING, 0));

		// Anchor at the top-left corner of the world map view.
		int x = mapBounds.x + MARGIN;
		int y = mapBounds.y + MARGIN;
		Rectangle origin = getBounds();
		graphics.translate(x - origin.x, y - origin.y);

		AffineTransform transform = graphics.getTransform();
		Dimension size = panelComponent.render(graphics);
		graphics.setTransform(transform);

		if (maxScroll > 0)
		{
			drawScrollbar(graphics, size, lines.size(), visibleRows);
		}

		listBounds = new Rectangle(x, y, size.width, size.height);
		rowHits = buildRowHits(rowComponents, rowFriends, x, y);

		// Returning null keeps the overlay layout machinery out of the way;
		// the panel is placed (and hit-tested) in absolute canvas coordinates.
		return null;
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
	{
		// Only scroll the list while the pointer hovers the panel; everywhere
		// else the wheel is left alone so the world map keeps zooming.
		if (!config.showPlayerList() || worldMapBounds() == null || maxScrollRows <= 0
			|| !pointerIn(listBounds))
		{
			return event;
		}

		scrollRows = Math.max(0,
			Math.min(scrollRows + event.getWheelRotation() * ROWS_PER_WHEEL_NOTCH, maxScrollRows));
		event.consume();
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (!SwingUtilities.isLeftMouseButton(event)
			|| !config.showPlayerList()
			|| worldMapBounds() == null
			|| !pointerIn(listBounds))
		{
			return event;
		}

		for (RowHit hit : rowHits)
		{
			if (hit.target != null && pointerIn(hit.bounds))
			{
				// Same effect as the world map's "Focus on" menu entry.
				client.getWorldMap().setWorldMapPositionTarget(hit.target);
				event.consume();
				break;
			}
		}
		return event;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		return event;
	}

	/**
	 * True when the pointer is inside the given rectangle. Uses the client's
	 * mouse position converted to widget space with the viewport offsets (the
	 * same conversion core's ClientUI uses), so hover tests match where the
	 * panel is actually drawn on any fixed, resizable or scaled client.
	 */
	private boolean pointerIn(Rectangle rect)
	{
		Point mouse = client.getMouseCanvasPosition();
		return mouse != null && rect.contains(
			mouse.getX() + client.getViewportXOffset(),
			mouse.getY() + client.getViewportYOffset());
	}

	/** Bounds of the world map view while the world map is open, else null. */
	private Rectangle worldMapBounds()
	{
		Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (map == null || map.isHidden())
		{
			return null;
		}
		Rectangle bounds = map.getBounds();
		return bounds == null || bounds.isEmpty() ? null : bounds;
	}

	/** Visible players: players on the local world first, each group by name. */
	private List<FriendLocation> sortedPlayers()
	{
		List<FriendLocation> players = new ArrayList<>(plugin.getCurrentFriends());
		int currentWorld = client.getWorld();
		players.sort(Comparator
			.comparing((FriendLocation friend) -> friend.getWorld() != currentWorld)
			.thenComparing(FriendLocation::getName, String.CASE_INSENSITIVE_ORDER));
		return players;
	}

	/** Display lines for the configured filter: flat player rows, or sections in Show All. */
	private static List<DisplayRow> buildLines(List<FriendLocation> players, PlayerListFilter filter)
	{
		List<DisplayRow> lines = new ArrayList<>();
		if (filter != PlayerListFilter.SHOW_ALL)
		{
			for (FriendLocation friend : players)
			{
				if (matches(friend, filter))
				{
					lines.add(DisplayRow.player(friend));
				}
			}
			return lines;
		}

		appendSection(lines, players, Relation.FRIEND, "Friends");
		appendSection(lines, players, Relation.CLAN, "Clan");
		appendSection(lines, players, Relation.FRIENDS_CHAT, "Friends Chat");
		return lines;
	}

	/** Append one section header and its players; empty sections are skipped. */
	private static void appendSection(List<DisplayRow> lines, List<FriendLocation> players,
		Relation relation, String title)
	{
		List<FriendLocation> section = new ArrayList<>();
		for (FriendLocation friend : players)
		{
			if (friend.getRelation() == relation)
			{
				section.add(friend);
			}
		}
		if (section.isEmpty())
		{
			return;
		}

		lines.add(DisplayRow.header(title + " (" + section.size() + ")"));
		for (FriendLocation friend : section)
		{
			lines.add(DisplayRow.player(friend));
		}
	}

	private static boolean matches(FriendLocation friend, PlayerListFilter filter)
	{
		switch (filter)
		{
			case SHOW_FRIENDS:
				return friend.getRelation() == Relation.FRIEND;
			case SHOW_CLAN:
				return friend.getRelation() == Relation.CLAN;
			case SHOW_FRIENDS_CHAT:
				return friend.getRelation() == Relation.FRIENDS_CHAT;
			default:
				return true;
		}
	}

	private static List<RowHit> buildRowHits(List<LineComponent> rows, List<FriendLocation> friends, int panelX, int panelY)
	{
		List<RowHit> hits = new ArrayList<>(rows.size());
		for (int i = 0; i < rows.size(); i++)
		{
			Rectangle bounds = rows.get(i).getBounds();
			hits.add(new RowHit(
				new Rectangle(panelX + bounds.x, panelY + bounds.y, bounds.width, bounds.height),
				friends.get(i).getLocation()));
		}
		return hits;
	}

	private void resetHitTargets()
	{
		listBounds = new Rectangle();
		rowHits = Collections.emptyList();
	}

	/** One display line: a section header, or a player row (exactly one is set). */
	private static final class DisplayRow
	{
		private final String header;
		private final FriendLocation friend;

		private DisplayRow(String header, FriendLocation friend)
		{
			this.header = header;
			this.friend = friend;
		}

		static DisplayRow header(String text)
		{
			return new DisplayRow(text, null);
		}

		static DisplayRow player(FriendLocation friend)
		{
			return new DisplayRow(null, friend);
		}
	}

	private static final class RowHit
	{
		private final Rectangle bounds;
		private final WorldPoint target;

		private RowHit(Rectangle bounds, WorldPoint target)
		{
			this.bounds = bounds;
			this.target = target;
		}
	}

	private void drawScrollbar(Graphics2D graphics, Dimension panelSize, int playerCount, int visibleRows)
	{
		int barX = panelSize.width - SCROLLBAR_WIDTH - SCROLLBAR_INSET;
		int trackY = SCROLLBAR_INSET;
		int trackHeight = panelSize.height - SCROLLBAR_INSET * 2;
		int thumbHeight = Math.max(SCROLLBAR_WIDTH * 2, trackHeight * visibleRows / playerCount);
		int thumbY = trackY + (trackHeight - thumbHeight) * scrollRows / Math.max(1, playerCount - visibleRows);

		graphics.setColor(new Color(255, 255, 255, 40));
		graphics.fillRoundRect(barX, trackY, SCROLLBAR_WIDTH, trackHeight, SCROLLBAR_WIDTH, SCROLLBAR_WIDTH);
		graphics.setColor(new Color(255, 255, 255, 140));
		graphics.fillRoundRect(barX, thumbY, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_WIDTH, SCROLLBAR_WIDTH);
	}
}
