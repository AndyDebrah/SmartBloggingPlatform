package com.smartblog.ui.navigation;

public enum View {
    LOGIN("/com/smartblog/ui/view/login/LoginView.fxml"),
    MAIN("/com/smartblog/ui/view/main/MainView.fxml"),
    POSTS("/com/smartblog/ui/view/posts/PostListView.fxml"),
    POST_DIALOG("/com/smartblog/ui/view/posts/dialog/PostDialog.fxml"),
    POST_EDITOR("/com/smartblog/ui/view/posts/editor/PostEditorView.fxml"),
    COMMENTS("/com/smartblog/ui/view/comments/CommentListView.fxml"),
    TAG_MANAGER("/com/smartblog/ui/view/tags/TagManagerView.fxml"),
    USERS("/com/smartblog/ui/view/users/UserListView.fxml"),
    AUTHOR("/com/smartblog/ui/view/authors/AuthorDashboardView.fxml"),
    ADMIN("/com/smartblog/ui/view/admin/AdminDashboardView.fxml"),
    ANALYTICS("/com/smartblog/ui/view/analytics/AnalyticsView.fxml"),
    PERFORMANCE("/com/smartblog/ui/view/performance/PerformanceView.fxml");

    public final String fxml;
    View(String fxml) { this.fxml = fxml; }
}