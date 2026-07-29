package src.controller;

public class BackActionException extends RuntimeException {
    public BackActionException() {
        super("\n[Back] Returning to main menu...");
    }
}