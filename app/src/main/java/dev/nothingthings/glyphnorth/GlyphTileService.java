package dev.nothingthings.glyphnorth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public final class GlyphTileService extends TileService {
    // Process-local: a killed display service must not leave a persisted 'on' tile.
    private static int current = DisplayMode.OFF;
    static int currentMode() { return current; }
    static void stateChanged(Context context, int mode) {
        current = mode;
        try { requestListeningState(context, new ComponentName(context, GlyphTileService.class)); }
        catch (RuntimeException e) {
            context.getSharedPreferences("compass", MODE_PRIVATE).edit().putString("tile_error", e.toString()).apply();
        }
    }
    static void select(Context context, int mode) {
        Intent intent = new Intent(context, CompassService.class).setAction(mode == DisplayMode.OFF ? CompassService.STOP : CompassService.START).putExtra("mode", mode);
        if (mode == DisplayMode.OFF) context.startService(intent); else context.startForegroundService(intent);
        stateChanged(context, mode);
    }
    @Override public void onStartListening() { update(); }
    @Override public void onClick() {
        try {
            getSharedPreferences("compass", MODE_PRIVATE).edit().putBoolean("display_test", false).apply();
            select(this, DisplayMode.next(current));
        } catch (RuntimeException e) {
            getSharedPreferences("compass", MODE_PRIVATE).edit().putString("hardware_status", "Tile could not switch display: " + e).apply();
        }
        update();
    }
    private void update() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel("Glyph North");
        tile.setSubtitle(DisplayMode.label(current));
        tile.setStateDescription(DisplayMode.label(current));
        tile.setContentDescription("Glyph North: " + DisplayMode.label(current) + ". Tap for " + DisplayMode.label(DisplayMode.next(current)));
        tile.setIcon(Icon.createWithResource(this, current == DisplayMode.TIME ? R.drawable.tile_clock : R.drawable.tile_compass));
        tile.setState(current == DisplayMode.OFF ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.updateTile();
    }
}
