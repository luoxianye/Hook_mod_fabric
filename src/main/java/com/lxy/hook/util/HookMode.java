package com.lxy.hook.util;

public enum HookMode {
    NORMAL("A", "普通拉取"),
    ANCHOR("B", "固定悬挂");

    private final String id;
    private final String displayName;

    HookMode(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public HookMode next() {
        return this == NORMAL ? ANCHOR : NORMAL;
    }

    public String message() {
        return "勾爪模式：" + id + " - " + displayName;
    }
}