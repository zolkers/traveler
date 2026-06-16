package dev.traveler.core.navigation;

public enum NavigationReplanActivation {
    START_NEW_SESSION,
    PREPARE_LOOKAHEAD,
    REPLACE_ACTIVE_SESSION;

    public boolean preservesActiveSession() {
        return this != START_NEW_SESSION;
    }
}
