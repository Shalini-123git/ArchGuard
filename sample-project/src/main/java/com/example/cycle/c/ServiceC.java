package com.example.cycle.c;

import com.example.cycle.a.ServiceA;

public class ServiceC {
    public ServiceA next() {
        return new ServiceA();
    }
}
