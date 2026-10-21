package com.example.domain.model

enum class QuantityMode(val displayName: String) {
    EXACT("Exact"),
    APPROXIMATE("Approximate"),
    PRESENCE_ONLY("Presence Only (Untracked)")
}

enum class ProjectStatus(val displayName: String) {
    IDEA("Idea"),
    PLANNED("Planned"),
    IN_PROGRESS("In Progress"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    ARCHIVED("Archived")
}

enum class MatchStatus(val displayName: String) {
    EXACT_MATCH("Exact Match"),
    USER_CONFIRMED("User Confirmed"),
    POSSIBLE_SUBSTITUTE("Possible Substitute")
}

enum class CoverageState(val displayName: String) {
    HAVE_ENOUGH("Have enough"),
    HAVE_SOME("Have some"),
    MISSING("Missing"),
    QUANTITY_UNKNOWN("Quantity unknown"),
    SUBSTITUTE_REVIEW_NEEDED("Possible substitute; review needed")
}

enum class StashViewMode {
    GRID,
    CARDS,
    MINIMAL
}

enum class SortOrder(val displayName: String) {
    UPDATED_DESC("Recently Updated"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    CATEGORY("Category"),
    LOCATION("Storage Location")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}
