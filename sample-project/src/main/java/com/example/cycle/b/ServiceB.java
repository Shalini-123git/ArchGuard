package com.example.cycle.b;

import com.example.cycle.c.ServiceC;

public class ServiceB {
    public ServiceC next() {
        return new ServiceC();
    }
}
