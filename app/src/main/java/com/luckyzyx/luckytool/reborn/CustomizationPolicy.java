/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.luckyzyx.luckytool.reborn;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Testable policy only: no Android or host-application dependencies. */
public final class CustomizationPolicy {
    private CustomizationPolicy() { }
    public static final int MAX_LINKS = 64;
    public static final int MAX_ENGINES = 24;
    public static final String BING_TEMPLATE = "https://www.bing.com/search?q={searchTerms}";
    public static final String GOOGLE_TEMPLATE = "https://www.google.com/search?q={searchTerms}";

    public static final class Link {
        public final String id, title, url;
        public Link(String id, String title, String url) {
            this.id = validId(id);
            this.title = validName(title);
            this.url = webUrl(url);
        }
    }

    public static final class Engine {
        public final String id, name, template;
        public Engine(String id, String name, String template) {
            this.id = validId(id);
            this.name = validName(name);
            this.template = searchTemplate(template);
        }
        public String key() { return "reborn_" + id; }
        public String search(String query) {
            String escaped;
            try { escaped = URLEncoder.encode(query, "UTF-8").replace("+", "%20"); }
            catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
            return template.replace("{searchTerms}", escaped);
        }
    }

    public static String validId(String raw) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Invalid identifier");
        return raw;
    }

    public static String validName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.length() > 80 || hasControl(name))
            throw new IllegalArgumentException("Name must be 1-80 printable characters");
        return name;
    }

    private static boolean hasControl(String text) {
        for (int i = 0; i < text.length(); i++) if (Character.isISOControl(text.charAt(i))) return true;
        return false;
    }

    /** No javascript:, intent:, file:, content:, user-info or protocol-relative URLs. */
    public static String webUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > 4096 || hasControl(value) || value.indexOf('\\') >= 0)
            throw new IllegalArgumentException("Invalid URL");
        URI uri;
        try { uri = URI.create(value); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Invalid URL", e); }
        String scheme = uri.getScheme();
        if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) ||
                uri.getHost() == null || uri.getHost().isEmpty() || uri.getUserInfo() != null ||
                uri.getPort() < -1 || uri.getPort() > 65535)
            throw new IllegalArgumentException("Use absolute HTTP(S) URL without credentials");
        return uri.toASCIIString();
    }

    public static String searchTemplate(String raw) {
        String value = raw == null ? "" : raw.trim();
        value = value.replace("%s", "{searchTerms}").replace("{query}", "{searchTerms}");
        if (value.contains("REBORN_QUERY_TOKEN"))
            throw new IllegalArgumentException("Reserved placeholder token");
        int first = value.indexOf("{searchTerms}");
        if (first < 0 || value.indexOf("{searchTerms}", first + 1) >= 0)
            throw new IllegalArgumentException("Need exactly one query placeholder");
        String probe = webUrl(value.replace("{searchTerms}", "REBORN_QUERY_TOKEN"));
        URI uri = URI.create(probe);
        if (uri.getRawAuthority().contains("REBORN_QUERY_TOKEN") ||
                uri.getRawFragment() != null && uri.getRawFragment().contains("REBORN_QUERY_TOKEN"))
            throw new IllegalArgumentException("Query placeholder must be in path or query");
        return probe.replace("REBORN_QUERY_TOKEN", "{searchTerms}");
    }

    /** Browser's candidate native navigation ids: Video 2; Novel 5/8. */
    public static boolean isRemovedBrowserTab(int nativeId) {
        return nativeId == 2 || nativeId == 5 || nativeId == 8;
    }

    public static <T> List<T> browserTabs(List<T> original, Function<T, Integer> nativeId) {
        ArrayList<T> kept = new ArrayList<>();
        for (T entry : original) if (!isRemovedBrowserTab(nativeId.apply(entry))) kept.add(entry);
        return kept.isEmpty() ? new ArrayList<>(original) : kept;
    }

    public static final class TabSelection<T> {
        public final List<T> tabs;
        public final int selected;
        public final boolean matched;
        private TabSelection(List<T> tabs, int selected, boolean matched) {
            this.tabs = Collections.unmodifiableList(tabs);
            this.selected = selected;
            this.matched = matched;
        }
    }

    /** Only filter '精选' if the same group contains '应用时刻'. */
    public static <T> TabSelection<T> marketTabs(List<T> original, Function<T, String> title) {
        boolean found = false;
        for (T item : original) if ("应用时刻".equals(title.apply(item))) found = true;
        if (!found) return new TabSelection<>(new ArrayList<>(original), -1, false);
        ArrayList<T> output = new ArrayList<>();
        int selection = -1;
        for (T item : original) {
            String name = title.apply(item);
            if ("精选".equals(name)) continue;
            if ("应用时刻".equals(name) && selection < 0) selection = output.size();
            output.add(item);
        }
        return new TabSelection<>(output, selection, selection >= 0);
    }

    public static <T> List<T> appendUnique(List<T> original, List<T> extra, Function<T, String> key) {
        ArrayList<T> result = new ArrayList<>(original);
        Set<String> known = new HashSet<>();
        for (T item : original) known.add(key.apply(item));
        for (T item : extra) if (known.add(key.apply(item))) result.add(item);
        return result;
    }

    public static boolean shouldLoadHome(boolean visible, boolean active, boolean homePage,
                                         long scheduledEpoch, long currentEpoch) {
        return visible && active && homePage && scheduledEpoch == currentEpoch;
    }
}
