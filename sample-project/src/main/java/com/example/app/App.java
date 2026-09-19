package com.example.app;

import com.example.tools.*;
import com.example.web.UserController;

public class App {
    public String run() {
        return new ToolBox().label() + new UserController().show();
    }
}
