package com.metkimod.marker;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MarkerManager {
    private static final List<Marker> markers = new CopyOnWriteArrayList<>();

        public static void addMarker(Marker marker) {
        markers.removeIf(m -> m.getOwnerId().equals(marker.getOwnerId()));
        markers.add(marker);
    }

        public static void tick() {
        markers.removeIf(Marker::isExpired);
    }

        public static List<Marker> getMarkers() {
        return Collections.unmodifiableList(markers);
    }

        public static void clear() {
        markers.clear();
    }

        public static int getCount() {
        return markers.size();
    }
}
