package com.example.tools;

import com.example.app.App;

/**
 * Only scanned when --include-tests is set. Would add tools -> app and a second cycle with app -> tools.
 */
public class ToolsTest {
    public App app() {
        return new App();
    }
}
