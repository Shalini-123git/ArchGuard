package com.example.web;

import com.example.data.UserRepository;

public class UserController {
    public String show() {
        return new UserRepository().find();
    }
}
