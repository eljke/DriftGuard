package ru.eljke.driftguard.demo;

/**
 * Ссылка на внешний инструмент локального demo-стенда.
 *
 * @param id стабильный идентификатор ссылки
 * @param title название инструмента для UI
 * @param url адрес инструмента
 * @param description краткое назначение инструмента
 */
public record ToolLink(String id, String title, String url, String description) {
}
