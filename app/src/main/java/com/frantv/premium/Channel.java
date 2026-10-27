package com.frantv.premium;

public class Channel {
    public final String id;
    public final String name;
    public final String country;
    public final String category;
    public final String logo;
    public final String url;

    public Channel(String id, String name, String country, String category, String logo, String url) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.category = category;
        this.logo = logo;
        this.url = url;
    }

    public String initials() {
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (parts[i].length() > 0) sb.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }
}
