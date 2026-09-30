package com.kainanresto.model.util;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class Icons {
    // Icons Phosphor
    public static final String SEARCH = "M11 3a8 8 0 1 0 0 16 8 8 0 0 0 0-16z M21 21l-4.3-4.3";
    public static final String PLUS   = "M5 12h14 M12 5v14";

    // Icons Lucide
    // 24-unit grid, drawn as 2px strokes (fill transparent)
    public static final String NAV_DASHBOARD =
            "M4 3h5a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z "
                    + "M15 3h5a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z "
                    + "M15 12h5a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1z "
                    + "M4 16h5a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1z";

    public static final String NAV_INVENTORY =
            "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z "
                    + "M3.27 6.96 12 12.01l8.73-5.05 M12 22.08V12";

    public static final String NAV_MENU =
            "M3 2v7c0 1.1 .9 2 2 2h4a2 2 0 0 0 2-2V2 M7 2v20 "
                    + "M21 15V2a5 5 0 0 0-5 5v6c0 1.1 .9 2 2 2h3z M21 15v7";

    public static final String NAV_SALES_ORDERS =
            "M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1z "
                    + "M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8 M12 17.5v-11";

    public static final String NAV_ACCOUNTS =
            "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 "
                    + "M9 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z "
                    + "M22 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75";

    public static final String NAV_SETTINGS =
            "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z "
                    + "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z";

    public static final String NAV_LOGOUT =
            "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9";

    public static final String CALENDAR =
            "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z "
                    + "M16 2v4 M8 2v4 M3 10h18";
    public static final String CHEVRON_DOWN = "M6 9l6 6 6-6";

    public static final String ELLIPSIS_VERTICAL =
            "M12 11a1 1 0 1 0 0 2 1 1 0 0 0 0-2z M12 4a1 1 0 1 0 0 2 1 1 0 0 0 0-2z M12 18a1 1 0 1 0 0 2 1 1 0 0 0 0-2z";

    public static final String DOLLAR_SIGN =
            "M12 2v20 M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6";

    public static final String SHOPPING_BAG =
            "M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z M3 6h18 M16 10a4 4 0 0 1-8 0";

    public static final String ROTATE_CCW =
            "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8 M3 3v5h5";

    public static final String TRENDING_UP = "M22 7l-8.5 8.5-5-5L2 17 M16 7h6v6";

    public static final String CIRCLE_CHECK =
            "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M9 12l2 2 4-4";

    public static final String SQUARE_PEN =
            "M12 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"
                    + "M18.375 2.625a1 1 0 0 1 3 3l-9.013 9.014a2 2 0 0 1-.853.505l-2.873.84a.5.5 0 0 1-.62-.62l.84-2.873a2 2 0 0 1 .506-.852z";

    public static final String TRASH_2 =
            "M3 6h18M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2M10 11v6M14 11v6";

    public static final String HOUSE =
            "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"
                    + "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z";
    public static final String GLOBE =
            "M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0z"
                    + "M12 2a14.5 14.5 0 0 0 0 20a14.5 14.5 0 0 0 0-20M2 12h20";
    public static final String DATABASE =
            "M21 5a9 3 0 1 1-18 0a9 3 0 1 1 18 0z"
                    + "M3 5v14a9 3 0 0 0 18 0V5M3 12a9 3 0 0 0 18 0";
    public static final String SHIELD =
            "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1"
                    + "c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z";
    public static final String IMAGE =
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"
                    + "M9 8.5a1.5 1.5 0 1 1-3 0a1.5 1.5 0 1 1 3 0z"
                    + "M21 15l-3.086-3.086a2 2 0 0 0-2.828 0L6 21";
    public static final String DOWNLOAD =
            "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3";
    public static final String RECEIPT_TEXT =
            "M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1z"
                    + "M14 8H8M16 12H8M13 16H8";
    public static final String CLOCK =
            "M22 12a10 10 0 1 1-20 0a10 10 0 1 1 20 0z"
                    + "M12 6v6l4 2";
    public static final String MINUS = "M5 12h14";


    // Use this to create an icon in code
    public static SVGPath icon(String pathData, Color color, double sizePx) {
        SVGPath p = new SVGPath();
        p.setContent(pathData);
        p.setFill(color);
        double scale = sizePx / 256.0;
        p.setScaleX(scale);
        p.setScaleY(scale);
        return p;
    }
}