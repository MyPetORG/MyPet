package de.Keyle.MyPet.util;

public final class StackTraces {

    private StackTraces() {}

    public static String currentThread() {
        StringBuilder trace = new StringBuilder();
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            trace.append("\t ").append(e).append("\n");
        }
        return trace.toString();
    }
}
