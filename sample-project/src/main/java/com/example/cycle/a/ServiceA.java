package com.example.cycle.a;

import com.example.cycle.b.ServiceB;

public class ServiceA {
    public ServiceB next() {
        return new ServiceB();
    }
}
