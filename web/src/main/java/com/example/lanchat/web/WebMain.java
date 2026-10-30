package com.example.lanchat.web;

public class WebMain {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  WiFili Messenger • Web Browser Edition Server");
        System.out.println("==================================================");
        try {
            WebServer.main(args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
