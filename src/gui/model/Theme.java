package src.gui.model;

import java.awt.Color;
import java.awt.Font;

/**
 * Central source of truth for all IDE visual styling — the Swing equivalent of a CSS file.
 * Components should reference constants here rather than hardcoding colors or fonts inline.
 *
 * FlatDarkLaf handles most general UI (buttons, tabs, scrollbars).
 * This class handles custom components that FlatLaf cannot reach:
 * the code editor, line numbers, status bar, and syntax token colors.
 */
public final class Theme {

    // Window
    /** Window opacity (0.0 = fully transparent, 1.0 = fully opaque). */
    public static final float WINDOW_OPACITY = 0.93f;

    // Editor
    public static final Color EDITOR_BG         = new Color(0x1E1E1E);
    public static final Color EDITOR_FG         = new Color(0xD4D4D4);
    public static final Color EDITOR_CARET      = new Color(0xAEAFAD);
    public static final Color EDITOR_SELECTION  = new Color(0x264F78);
    public static final Font  EDITOR_FONT       = new Font(Font.MONOSPACED, Font.PLAIN, 14);

    // Line numbers
    public static final Color LINE_NUMBER_BG    = new Color(0x1E1E1E);
    public static final Color LINE_NUMBER_FG    = new Color(0x858585);

    // Panel headers (badge strip above output panels)
    public static final Color PANEL_HEADER_BG      = new Color(0x252526);
    public static final Color PANEL_HEADER_OK      = new Color(0x4EC994);
    public static final Color PANEL_HEADER_FAIL    = new Color(0xF44747);
    public static final Color PANEL_HEADER_NEUTRAL = new Color(0x858585);

    // Terminal (Output tab)
    public static final Color TERMINAL_BG = new Color(0x0D1117);
    public static final Color TERMINAL_FG = new Color(0xABB2BF);

    // Status bar
    public static final Color STATUS_BAR_BG     = new Color(0x007ACC); // VS Code blue bar
    public static final Color STATUS_BAR_FG     = Color.WHITE;
    public static final Font  STATUS_BAR_FONT   = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    // Syntax token colors (dark theme — VSCode Dark+)
    public static final Color TOKEN_KEYWORD          = new Color(0x569CD6); // if, else, while...
    public static final Color TOKEN_ABM_KEYWORD      = new Color(0xC586C0); // spawn, move, zone...
    public static final Color TOKEN_TYPE             = new Color(0x4EC9B0); // int, float, bool...
    public static final Color TOKEN_DECLARATION      = new Color(0xDCDCAA); // agent, world, main...
    public static final Color TOKEN_IDENTIFIER       = new Color(0xD4D4D4);
    public static final Color TOKEN_LITERAL_NUMERIC  = new Color(0xB5CEA8);
    public static final Color TOKEN_LITERAL_STRING   = new Color(0xCE9178);
    public static final Color TOKEN_BOOLEAN          = new Color(0x569CD6);
    public static final Color TOKEN_NULL             = new Color(0x9CDCFE);
    public static final Color TOKEN_OPERATOR         = new Color(0xD4D4D4);
    public static final Color TOKEN_PUNCTUATION      = new Color(0x808080);
    public static final Color TOKEN_COMMENT          = new Color(0x6A9955);
    public static final Color TOKEN_ERROR            = new Color(0xF44747);

    private Theme() {}
}
