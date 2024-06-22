package com.mygdx.game.ui;

import java.lang.Math;

public class Joystick {
    private float initialX;
    private float initialY;
    private float radius;
    private boolean active;
    private float x;
    private float y;

    public float getInitialX() {
        return initialX;
    }

    public void setInitialX(float initialX) {
        this.initialX = initialX;
    }

    public float getInitialY() {
        return initialY;
    }

    public void setInitialY(float initialY) {
        this.initialY = initialY;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getDegree() {
        return degree;
    }

    public void setDegree(float degree) {
        this.degree = degree;
    }

    public float getStickDistPercentageX() {
        return stickDistPercentageX;
    }

    public void setStickDistPercentageX(float stickDistPercentageX) {
        this.stickDistPercentageX = stickDistPercentageX;
    }

    public float getStickDistPercentageY() {
        return stickDistPercentageY;
    }

    public void setStickDistPercentageY(float stickDistPercentageY) {
        this.stickDistPercentageY = stickDistPercentageY;
    }

    public float getDistanceX() {
        return distanceX;
    }

    public void setDistanceX(float distanceX) {
        this.distanceX = distanceX;
    }

    public float getDistanceY() {
        return distanceY;
    }

    public void setDistanceY(float distanceY) {
        this.distanceY = distanceY;
    }

    public float getRealStickDistPercentageX() {
        return realStickDistPercentageX;
    }

    public void setRealStickDistPercentageX(float realStickDistPercentageX) {
        this.realStickDistPercentageX = realStickDistPercentageX;
    }

    public float getRealStickDistPercentageY() {
        return realStickDistPercentageY;
    }

    public void setRealStickDistPercentageY(float realStickDistPercentageY) {
        this.realStickDistPercentageY = realStickDistPercentageY;
    }

    public float[] getCachedValues() {
        return cachedValues;
    }

    public void setCachedValues(float[] cachedValues) {
        this.cachedValues = cachedValues;
    }

    private float degree;
    private float stickDistPercentageX;
    private float stickDistPercentageY;
    private float distanceX;
    private float distanceY;
    private float realStickDistPercentageX;
    private float realStickDistPercentageY;
    private float[] cachedValues;

    public Joystick(float initialX, float initialY, float radius) {
        this.initialX = initialX;
        this.initialY = initialY;
        this.radius = radius;
        this.active = false;
        this.x = 0f;
        this.y = 0f;
        this.degree = 0f;
        this.stickDistPercentageX = 0f;
        this.stickDistPercentageY = 0f;
        this.distanceX = 0f;
        this.distanceY = 0f;
        this.realStickDistPercentageX = 0f;
        this.realStickDistPercentageY = 0f;
        this.cachedValues = new float[9];
        this.cachedValues[0] = this.x;
        this.cachedValues[1] = this.y;
        this.cachedValues[2] = this.degree;
        this.cachedValues[3] = this.stickDistPercentageX;
        this.cachedValues[4] = this.stickDistPercentageY;
        this.cachedValues[5] = this.distanceX;
        this.cachedValues[6] = this.distanceY;
        this.cachedValues[7] = this.realStickDistPercentageX;
        this.cachedValues[8] = this.realStickDistPercentageY;
    }

    public float[] updateValues(float mouseX, float mouseY) {
        this.distanceY = mouseY - this.initialY;
        this.distanceX = mouseX - this.initialX;
        if (this.distanceX == 0f || this.distanceY == 0f) {
            return this.cachedValues;
        }
        this.degree = (float) Math.atan2(this.distanceY, this.distanceX);

        float outerCircleHalfR = (float) Math.sqrt(this.distanceX * this.distanceX + this.distanceY * this.distanceY);

        this.realStickDistPercentageX = (float) (Math.cos(this.degree) * Math.abs(outerCircleHalfR / this.radius));
        this.realStickDistPercentageY = (float) (Math.sin(this.degree) * Math.abs(outerCircleHalfR / this.radius));
        this.stickDistPercentageX = (float) (Math.cos(this.degree) * Math.abs(outerCircleHalfR / this.radius));
        this.stickDistPercentageX = Math.max(-1f, Math.min(1f, this.stickDistPercentageX));
        this.stickDistPercentageY = (float) (Math.sin(this.degree) * Math.abs(outerCircleHalfR / this.radius));
        this.stickDistPercentageY = Math.max(-1f, Math.min(1f, this.stickDistPercentageY));

        this.x = (float) (Math.cos(this.degree) * this.radius);
        this.y = (float) (Math.sin(this.degree) * this.radius);

        this.cachedValues[0] = this.x;
        this.cachedValues[1] = this.y;
        this.cachedValues[2] = this.degree;
        this.cachedValues[3] = this.stickDistPercentageX;
        this.cachedValues[4] = this.stickDistPercentageY;
        this.cachedValues[5] = this.distanceX;
        this.cachedValues[6] = this.distanceY;
        this.cachedValues[7] = this.realStickDistPercentageX;
        this.cachedValues[8] = this.realStickDistPercentageY;

        return this.cachedValues;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void reset() {
        this.x = 0f;
        this.y = 0f;
        this.degree = 0f;
        this.stickDistPercentageX = 0f;
        this.stickDistPercentageY = 0f;
        this.distanceX = 0f;
        this.distanceY = 0f;
        this.realStickDistPercentageX = 0f;
        this.realStickDistPercentageY = 0f;
    }
}