package ru.eljke.driftguard.demo.tool;

/**
 * Link to an external tool of the local demo environment.
 *
 * @param id stable link identifier
 * @param title tool title for the UI
 * @param url tool URL
 * @param description short tool purpose
 */
public record ToolLink(String id, String title, String url, String description) {
}


