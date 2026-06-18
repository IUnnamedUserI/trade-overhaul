package com.unnameduser.tradeoverhaul.common.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public class GuiScaleHelper {

    // Базовые значения для эталона (1920x1080, GUI scale 3)
    private static final int REFERENCE_WIDTH = 1920;
    private static final int REFERENCE_HEIGHT = 1080;
    private static final int REFERENCE_GUI_SCALE = 3;

    private final MinecraftClient client;
    private final Window window;

    public GuiScaleHelper(MinecraftClient client) {
        this.client = client;
        this.window = client.getWindow();
    }

    /**
     * Получить коэффициент масштабирования по ширине
     */
    public float getScaleX() {
        return (float) window.getScaledWidth() / REFERENCE_WIDTH * REFERENCE_GUI_SCALE;
    }

    /**
     * Получить коэффициент масштабирования по высоте
     */
    public float getScaleY() {
        return (float) window.getScaledHeight() / REFERENCE_HEIGHT * REFERENCE_GUI_SCALE;
    }

    /**
     * Масштабировать значение X
     */
    public int scaleX(int x) {
        return (int) (x * getScaleX());
    }

    /**
     * Масштабировать значение Y
     */
    public int scaleY(int y) {
        return (int) (y * getScaleY());
    }

    /**
     * Масштабировать размер (ширину/высоту)
     */
    public int scaleSize(int size) {
        return (int) (size * Math.min(getScaleX(), getScaleY()));
    }

    /**
     * Получить текущий масштаб GUI
     */
    public int getCurrentGuiScale() {
        return (int) window.getScaleFactor();
    }

    /**
     * Проверить, нужно ли применять масштабирование
     */
    public boolean needsScaling() {
        return getCurrentGuiScale() != REFERENCE_GUI_SCALE ||
                window.getScaledWidth() != REFERENCE_WIDTH / REFERENCE_GUI_SCALE;
    }
}